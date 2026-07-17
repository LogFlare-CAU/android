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
$QaBaseUrl = 'http://10.0.2.2:8000'

$BaselinesDir = Join-Path $RepoRoot 'visual-qa\device-baselines'
$DiffsDir = Join-Path $RepoRoot 'visual-qa\device-diffs'
$CapturesRoot = Join-Path $RepoRoot 'visual-qa\device-captures'
$ToolsBat = Join-Path $RepoRoot 'visual-qa\tools\build\install\tools\bin\tools.bat'
$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
$ApkPath = Join-Path $RepoRoot 'app\build\outputs\apk\debug\app-debug.apk'
$FlowsDir = Join-Path $RepoRoot '.maestro\flows'

$resolvedSerial = $null
$animationPrevious = @{}
$uiModePrevious = $null
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
    $uri = 'http://127.0.0.1:8000/__qa/reset'
    if ($PSVersionTable.PSVersion.Major -ge 6) {
        Invoke-WebRequest -Uri $uri -Method Post -TimeoutSec 10 -UseBasicParsing | Out-Null
    }
    else {
        Invoke-WebRequest -Uri $uri -Method Post -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop | Out-Null
    }
}

function Deny-PostNotificationsIfPresent {
    param([string]$DeviceSerial)
    $packages = & adb -s $DeviceSerial shell pm list permissions -g 2>$null | Out-String
    if ($packages -match 'POST_NOTIFICATIONS' -or $true) {
        & adb -s $DeviceSerial shell pm deny com.logflare.android android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
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

function Invoke-RecordAll {
    param(
        [string]$CaptureDir,
        [string]$Theme
    )
    New-Item -ItemType Directory -Force -Path $BaselinesDir | Out-Null
    foreach ($checkpoint in $ExpectedCheckpoints) {
        $expected = Get-CheckpointPath -Directory $BaselinesDir -Checkpoint $checkpoint -Theme $Theme
        $actual = Get-CheckpointPath -Directory $CaptureDir -Checkpoint $checkpoint -Theme $Theme
        Write-Info ("record-device {0}" -f (Split-Path -Leaf $expected))
        & $ToolsBat record-device --actual $actual --expected $expected
        if ($LASTEXITCODE -ne 0) {
            throw ("record-device failed for {0}" -f (Split-Path -Leaf $expected))
        }
    }
}

function Invoke-MaestroFlow {
    param(
        [string]$DeviceSerial,
        [string]$FlowName,
        [string]$Theme,
        [string]$OutputDir
    )
    $flowPath = Join-Path $FlowsDir $FlowName
    if (-not (Test-Path -LiteralPath $flowPath)) {
        throw "Missing Maestro flow: $flowPath"
    }
    $env:QA_BASE_URL = $QaBaseUrl
    $env:QA_USERNAME = $QaUsername
    $env:QA_PASSWORD = $QaPassword
    $env:QA_THEME = $Theme
    $env:QA_OUTPUT_DIR = $OutputDir

    Write-Info ("maestro test {0} theme={1}" -f $FlowName, $Theme)
    & maestro --device $DeviceSerial test $flowPath
    if ($LASTEXITCODE -ne 0) {
        throw ("Maestro flow failed: {0} (theme={1}, exit={2})" -f $FlowName, $Theme, $LASTEXITCODE)
    }
}

try {
    if ($Mode -notin @('Verify', 'Record')) {
        throw "Invalid -Mode '$Mode'. Allowed values: Verify, Record."
    }

    Assert-Command -Name 'adb'
    Assert-Command -Name 'maestro'
    Assert-Java17
    $resolvedSerial = Resolve-EmulatorSerial -Serial $Serial
    Assert-Pixel7Api35Profile -Serial $resolvedSerial

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

    $uiModePrevious = (& adb -s $resolvedSerial shell cmd uimode night).Trim()
    Set-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious

    Write-Info 'Starting mock server on 127.0.0.1:8000'
    $serverProc = Start-Process -FilePath $ToolsBat -ArgumentList @('server', '--host', '127.0.0.1', '--port', '8000') `
        -WorkingDirectory (Split-Path -Parent $ToolsBat) `
        -PassThru -WindowStyle Hidden
    Wait-HttpHealth -Url 'http://127.0.0.1:8000/__qa/health' -TimeoutSeconds 60

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
            Set-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious
            Invoke-MaestroFlow -DeviceSerial $resolvedSerial -FlowName $flow -Theme $theme -OutputDir $captureDir
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
        foreach ($theme in $Themes) {
            Invoke-RecordAll -CaptureDir $themeCaptureDirs[$theme] -Theme $theme
        }
        Write-Info 'Record completed: baselines updated after full light+dark capture success.'
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
                if ($uiModePrevious -match 'yes|true|night') {
                    & adb -s $resolvedSerial shell cmd uimode night yes | Out-Null
                }
                else {
                    & adb -s $resolvedSerial shell cmd uimode night no | Out-Null
                }
            }
            catch {
                Write-Warning ("Failed to restore UI mode: {0}" -f $_.Exception.Message)
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
