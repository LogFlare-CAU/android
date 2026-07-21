#Requires -Version 5.1
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Verify', 'Record')]
    [string]$Mode,

    [string]$Serial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
. (Join-Path $ScriptDir 'visual-qa-common.ps1')

$ExpectedCheckpoints = @(
    'home',
    'logs_detail',
    'projects_list',
    'project_detail',
    'project_settings',
    'my_page',
    'add_member',
    'edit_member',
    'logout_confirmation'
)

$Flows = @(
    'login-home.yaml',
    'logs-detail.yaml',
    'projects.yaml',
    'mypage-members.yaml',
    'logout.yaml'
)

$Themes = @('light', 'dark')

$QaUsername = 'qa-admin'
$QaPassword = 'qa-password'
# Not 8000: Docker Desktop squats that port on the host, and it is already taken on the device
# too, so neither the mock server nor the adb reverse listener can bind it.
$QaServerPort = 18000

# Physical devices reach the host mock server through `adb reverse`, so the app talks to its
# own loopback. `localhost` (not 127.0.0.1) is required: the debug network security config
# whitelists cleartext by that hostname, and ServerConfigRepository special-cases it for ports.
$QaBaseUrl = "http://localhost:$QaServerPort"

# Reference device profile for the recorded baselines. Baselines are only reproducible on a
# device matching all three values; a mismatch (e.g. a changed display resolution) fails loudly
# instead of silently rewriting the baseline set.
$ExpectedApi = '36'
$ExpectedSize = '1440x3120'
$ExpectedDensity = '600'
$DeviceHeightPx = [int]($ExpectedSize -split 'x')[1]

# Flows crop on `app_root`, which is edge-to-edge and so reaches under the status bar: every capture
# would otherwise carry the live clock, battery level and notification icons and never match twice.
# One UI ignores SystemUI demo mode (the usual way to freeze that chrome), so the band is dropped.
# 139 is this device's reported top inset -- `adb shell dumpsys window displays` prints it as
# `DisplayCutout{insets=Rect(0, 139 - 0, 0)}` / `overrideNonDecorInsets=[0,139]`. On this device rows
# 108..142 are empty, so the cut lands in the gap between the status bar and the app's first pixel.
$StatusBarCropPx = 139

# Long enough to outlast a full light+dark run; the original value is restored on exit.
$QaScreenOffTimeoutMs = '1800000'

$BaselinesDir = Join-Path $RepoRoot 'visual-qa\device-baselines'
$StagingDir = Join-Path $RepoRoot 'visual-qa\device-baselines-staging'
$DiffsDir = Join-Path $RepoRoot 'visual-qa\device-diffs'
$CapturesRoot = Join-Path $RepoRoot 'visual-qa\device-captures'
# Scratch area for Maestro's own artifact tree; screenshots are collected out of it by file name.
$MaestroOutputRoot = Join-Path $CapturesRoot '_maestro'
$ToolsBat = Join-Path $RepoRoot 'visual-qa\tools\build\install\tools\bin\tools.bat'
$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
$ApkPath = Join-Path $RepoRoot 'app\build\outputs\apk\debug\app-debug.apk'
$FlowsDir = Join-Path $RepoRoot '.maestro\flows'

$resolvedSerial = $null
$animationPrevious = @{}
$uiModePrevious = $null
$screenTimeoutPrevious = $null
$reverseApplied = $false
$serverProc = $null
$exitCode = 1

function Write-Info([string]$Message) {
    Write-Host "[visual-qa-maestro-mock] $Message"
}

function Stop-ProcessTree {
    param([System.Diagnostics.Process]$Process)
    if ($null -eq $Process) { return }
    try {
        if (-not $Process.HasExited) {
            & taskkill /PID $Process.Id /T /F 2>$null | Out-Null
        }
    }
    catch {
        # best-effort cleanup
    }
}

function Invoke-QaReset {
    $uri = "http://127.0.0.1:$QaServerPort/__qa/reset"
    if ($PSVersionTable.PSVersion.Major -ge 6) {
        Invoke-WebRequest -Uri $uri -Method Post -TimeoutSec 10 -UseBasicParsing | Out-Null
    }
    else {
        Invoke-WebRequest -Uri $uri -Method Post -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop | Out-Null
    }
}

function Get-CheckpointPath {
    param(
        [string]$Directory,
        [string]$Checkpoint,
        [string]$Theme
    )
    return (Join-Path $Directory ("{0}_{1}.png" -f $Checkpoint, $Theme))
}

function Assert-CaptureManifest {
    param(
        [string]$CaptureDir,
        [string]$Theme
    )
    $expected = @()
    foreach ($checkpoint in $ExpectedCheckpoints) {
        $expected += ("{0}_{1}.png" -f $checkpoint, $Theme)
    }
    $actual = @(Get-ChildItem -Path $CaptureDir -Filter '*.png' -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })
    $missing = @($expected | Where-Object { $actual -notcontains $_ })
    $unexpected = @($actual | Where-Object { $expected -notcontains $_ })
    if ($missing.Count -gt 0 -or $unexpected.Count -gt 0) {
        throw ("Capture manifest mismatch for theme '{0}'. Missing=[{1}] Unexpected=[{2}]" -f `
                $Theme, ($missing -join ', '), ($unexpected -join ', '))
    }
}

function Assert-BaselineManifest {
    param([string]$Theme)
    $missing = @()
    foreach ($checkpoint in $ExpectedCheckpoints) {
        $path = Get-CheckpointPath -Directory $BaselinesDir -Checkpoint $checkpoint -Theme $Theme
        if (-not (Test-Path -LiteralPath $path)) {
            $missing += (Split-Path -Leaf $path)
        }
    }
    $actual = @(Get-ChildItem -Path $BaselinesDir -Filter ("*_{0}.png" -f $Theme) -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })
    $expectedNames = @($ExpectedCheckpoints | ForEach-Object { "{0}_{1}.png" -f $_, $Theme })
    $unexpected = @($actual | Where-Object { $expectedNames -notcontains $_ })
    if ($missing.Count -gt 0 -or $unexpected.Count -gt 0) {
        throw ("Baseline manifest mismatch for theme '{0}'. Missing=[{1}] Unexpected=[{2}]" -f `
                $Theme, ($missing -join ', '), ($unexpected -join ', '))
    }
}

function Invoke-CompareAll {
    param(
        [string]$CaptureDir,
        [string]$Theme
    )
    New-Item -ItemType Directory -Force -Path $DiffsDir | Out-Null
    $failed = @()
    foreach ($checkpoint in $ExpectedCheckpoints) {
        $expected = Get-CheckpointPath -Directory $BaselinesDir -Checkpoint $checkpoint -Theme $Theme
        $actual = Get-CheckpointPath -Directory $CaptureDir -Checkpoint $checkpoint -Theme $Theme
        $diff = Get-CheckpointPath -Directory $DiffsDir -Checkpoint $checkpoint -Theme $Theme
        Write-Info ("compare {0}" -f (Split-Path -Leaf $actual))
        & $ToolsBat compare --expected $expected --actual $actual --diff $diff
        if ($LASTEXITCODE -ne 0) {
            $failed += (Split-Path -Leaf $actual)
        }
    }
    if ($failed.Count -gt 0) {
        throw ("Verify failed for theme '{0}'. Diffs written under {1}. Failed=[{2}]" -f `
                $Theme, $DiffsDir, ($failed -join ', '))
    }
}

function Invoke-StageRecordAll {
    param(
        [hashtable]$ThemeCaptureDirs
    )
    if (Test-Path -LiteralPath $StagingDir) {
        Remove-Item -LiteralPath $StagingDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $StagingDir | Out-Null

    foreach ($theme in $Themes) {
        $captureDir = $ThemeCaptureDirs[$theme]
        foreach ($checkpoint in $ExpectedCheckpoints) {
            $actual = Get-CheckpointPath -Directory $captureDir -Checkpoint $checkpoint -Theme $theme
            $staged = Get-CheckpointPath -Directory $StagingDir -Checkpoint $checkpoint -Theme $theme
            Write-Info ("record-device (stage) {0}" -f (Split-Path -Leaf $staged))
            & $ToolsBat record-device --actual $actual --expected $staged
            if ($LASTEXITCODE -ne 0) {
                throw ("record-device staging failed for {0}; baselines left untouched" -f (Split-Path -Leaf $staged))
            }
        }
    }
}

function Promote-BaselineSet {
    $expectedNames = @()
    foreach ($theme in $Themes) {
        foreach ($checkpoint in $ExpectedCheckpoints) {
            $expectedNames += ("{0}_{1}.png" -f $checkpoint, $theme)
        }
    }
    $csv = ($expectedNames -join ',')
    Write-Info ("promote-baselines count={0}" -f $expectedNames.Count)
    & $ToolsBat promote-baselines --staging $StagingDir --baselines $BaselinesDir --expected $csv
    if ($LASTEXITCODE -ne 0) {
        throw "promote-baselines failed; previous baselines restored if backup was available"
    }
}

function Invoke-MaestroFlow {
    param(
        [string]$DeviceSerial,
        [string]$FlowName,
        [string]$Theme,
        [string]$OutputDirAbsolute
    )
    $flowPath = Join-Path $FlowsDir $FlowName
    if (-not (Test-Path -LiteralPath $flowPath)) {
        throw "Missing Maestro flow: $flowPath"
    }
    $relativeFlow = ConvertTo-MaestroRelativePath -AbsolutePath $flowPath
    $relativeOutput = ConvertTo-MaestroRelativePath -AbsolutePath $OutputDirAbsolute

    $env:QA_BASE_URL = $QaBaseUrl
    $env:QA_USERNAME = $QaUsername
    $env:QA_PASSWORD = $QaPassword
    $env:QA_THEME = $Theme
    $env:QA_OUTPUT_DIR = $relativeOutput

    # Maestro always writes screenshots under "<test output dir>/**/takeScreenshot/<path>.png" and
    # rejects absolute paths outright, so capture into a scratch tree and collect them by file name.
    $flowOutDir = Join-Path $MaestroOutputRoot ("{0}_{1}" -f $Theme, [System.IO.Path]::GetFileNameWithoutExtension($FlowName))
    if (Test-Path -LiteralPath $flowOutDir) {
        Remove-Item -LiteralPath $flowOutDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $flowOutDir | Out-Null

    Write-Info ("maestro test {0} theme={1} output={2}" -f $FlowName, $Theme, $relativeOutput)
    Push-Location $RepoRoot
    try {
        # Maestro resolves ${...} only from --env pairs; ambient environment variables are ignored
        # and silently become the literal string "undefined" inside the flow.
        & maestro --device $DeviceSerial test --test-output-dir $flowOutDir `
            -e "QA_BASE_URL=$QaBaseUrl" `
            -e "QA_USERNAME=$QaUsername" `
            -e "QA_PASSWORD=$QaPassword" `
            -e "QA_THEME=$Theme" `
            -e "QA_OUTPUT_DIR=$relativeOutput" `
            $relativeFlow
        if ($LASTEXITCODE -ne 0) {
            throw ("Maestro flow failed: {0} (theme={1}, exit={2})" -f $FlowName, $Theme, $LASTEXITCODE)
        }
    }
    finally {
        Pop-Location
    }

    $shots = @(Get-ChildItem -LiteralPath $flowOutDir -Recurse -Filter '*.png' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]takeScreenshot[\\/]' })
    if ($shots.Count -eq 0) {
        throw ("Maestro reported success but produced no takeScreenshot output for flow {0} (theme={1}) under {2}" -f `
                $FlowName, $Theme, $flowOutDir)
    }
    foreach ($shot in $shots) {
        $collected = Join-Path $OutputDirAbsolute $shot.Name
        Move-Item -LiteralPath $shot.FullName -Destination $collected -Force
        & $ToolsBat crop-top --image $collected --pixels $StatusBarCropPx --expect-height $DeviceHeightPx
        if ($LASTEXITCODE -ne 0) {
            throw ("crop-top failed for {0} (flow={1}, theme={2})" -f $shot.Name, $FlowName, $Theme)
        }
    }
    Write-Info ("collected {0} screenshot(s) from {1}" -f $shots.Count, $FlowName)
}

try {
    if ($Mode -notin @('Verify', 'Record')) {
        throw "Invalid -Mode '$Mode'. Allowed values: Verify, Record."
    }

    Assert-Command -Name 'adb'
    Assert-Command -Name 'maestro'
    Assert-Java17
    $resolvedSerial = Resolve-EmulatorSerial -Serial $Serial
    Assert-DeviceProfile -Serial $resolvedSerial `
        -ExpectedApi $ExpectedApi -ExpectedSize $ExpectedSize -ExpectedDensity $ExpectedDensity

    Write-Info ("Mode={0} Serial={1}" -f $Mode, $resolvedSerial)
    Write-Info 'Building app debug APK and installing visual-qa tools distribution'
    Push-Location $RepoRoot
    try {
        & $Gradlew ':app:assembleDebug' ':visual-qa:tools:installDist' --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle assembleDebug/installDist failed with exit $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path -LiteralPath $ApkPath)) {
        throw "Debug APK missing after assembleDebug: $ApkPath"
    }
    if (-not (Test-Path -LiteralPath $ToolsBat)) {
        throw "tools.bat missing after installDist: $ToolsBat"
    }

    Write-Info "Installing APK on $resolvedSerial"
    & adb -s $resolvedSerial install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed with exit $LASTEXITCODE"
    }

    Write-Info ("Reversing device localhost:{0} to host mock server" -f $QaServerPort)
    # A binding left over from a crashed run would make the bind below fail; clearing it is a no-op otherwise.
    & adb -s $resolvedSerial reverse --remove ("tcp:{0}" -f $QaServerPort) 2>$null | Out-Null
    & adb -s $resolvedSerial reverse ("tcp:{0}" -f $QaServerPort) ("tcp:{0}" -f $QaServerPort) | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw ("adb reverse tcp:{0} failed with exit {1}. If this is 'Address already in use', something on the device already listens on {0}; pick a different `$QaServerPort." -f $QaServerPort, $LASTEXITCODE)
    }
    $reverseApplied = $true

    $uiModePrevious = (& adb -s $resolvedSerial shell cmd uimode night | Out-String).Trim()
    $screenTimeoutPrevious = Get-ScreenOffTimeout -Serial $resolvedSerial
    Set-ScreenOffTimeout -Serial $resolvedSerial -Milliseconds $QaScreenOffTimeoutMs
    Capture-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious
    Disable-AnimationScales -Serial $resolvedSerial

    Write-Info ("Starting mock server on 127.0.0.1:{0}" -f $QaServerPort)
    $serverProc = Start-Process -FilePath $ToolsBat -ArgumentList @('server', '--host', '127.0.0.1', '--port', "$QaServerPort") `
        -WorkingDirectory (Split-Path -Parent $ToolsBat) `
        -PassThru -WindowStyle Hidden
    Wait-HttpHealth -Url "http://127.0.0.1:$QaServerPort/__qa/health" -TimeoutSeconds 60

    $themeCaptureDirs = @{}
    foreach ($theme in $Themes) {
        $captureDir = Join-Path $CapturesRoot $theme
        if (Test-Path -LiteralPath $captureDir) {
            Remove-Item -LiteralPath $captureDir -Recurse -Force
        }
        New-Item -ItemType Directory -Force -Path $captureDir | Out-Null
        $themeCaptureDirs[$theme] = $captureDir

        foreach ($flow in $Flows) {
            Write-Info ("Preparing fixture/device for flow={0} theme={1}" -f $flow, $theme)
            Invoke-QaReset
            & adb -s $resolvedSerial shell pm clear com.logflare.android | Out-Null
            Deny-PostNotificationsIfPresent -DeviceSerial $resolvedSerial
            Set-UiMode -Serial $resolvedSerial -Theme $theme
            Disable-AnimationScales -Serial $resolvedSerial
            Invoke-MaestroFlow -DeviceSerial $resolvedSerial -FlowName $flow -Theme $theme -OutputDirAbsolute $captureDir
        }

        Assert-CaptureManifest -CaptureDir $captureDir -Theme $theme
    }

    if ($Mode -eq 'Verify') {
        foreach ($theme in $Themes) {
            Assert-BaselineManifest -Theme $theme
            Invoke-CompareAll -CaptureDir $themeCaptureDirs[$theme] -Theme $theme
        }
        Write-Info 'Verify completed: all checkpoints matched.'
    }
    else {
        Invoke-StageRecordAll -ThemeCaptureDirs $themeCaptureDirs
        Promote-BaselineSet
        Write-Info 'Record completed: staged then promoted complete baseline set.'
    }

    $exitCode = 0
}
catch {
    Write-Error $_
    $exitCode = 1
}
finally {
    Stop-ProcessTree -Process $serverProc

    if (-not [string]::IsNullOrWhiteSpace($resolvedSerial)) {
        try {
            if ($animationPrevious.Count -gt 0) {
                Restore-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious
            }
        }
        catch {
            Write-Warning ("Failed to restore animation scales: {0}" -f $_.Exception.Message)
        }

        if (-not [string]::IsNullOrWhiteSpace($uiModePrevious)) {
            try {
                $prev = Resolve-UiModeNightState -Raw $uiModePrevious
                if ($prev -eq 'yes') {
                    & adb -s $resolvedSerial shell cmd uimode night yes | Out-Null
                }
                elseif ($prev -eq 'no') {
                    & adb -s $resolvedSerial shell cmd uimode night no | Out-Null
                }
            }
            catch {
                Write-Warning ("Failed to restore UI mode: {0}" -f $_.Exception.Message)
            }
        }

        if (-not [string]::IsNullOrWhiteSpace($screenTimeoutPrevious)) {
            try {
                Set-ScreenOffTimeout -Serial $resolvedSerial -Milliseconds $screenTimeoutPrevious
            }
            catch {
                Write-Warning ("Failed to restore screen off timeout: {0}" -f $_.Exception.Message)
            }
        }

        if ($reverseApplied) {
            try {
                & adb -s $resolvedSerial reverse --remove ("tcp:{0}" -f $QaServerPort) | Out-Null
            }
            catch {
                Write-Warning ("Failed to remove adb reverse: {0}" -f $_.Exception.Message)
            }
        }
    }

    Remove-Item Env:QA_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:QA_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:QA_BASE_URL -ErrorAction SilentlyContinue
    Remove-Item Env:QA_THEME -ErrorAction SilentlyContinue
    Remove-Item Env:QA_OUTPUT_DIR -ErrorAction SilentlyContinue
}

exit $exitCode
