#Requires -Version 5.1
<#
.SYNOPSIS
    Drives the debug app against a real development server and collects diagnostic captures.

.DESCRIPTION
    This is NOT a regression tier. It never compares against baselines and never writes them --
    a development server's data changes, so its captures are only ever evidence for a human.
    Use visual-qa-maestro-mock.ps1 for anything that has to pass or fail.

    Only the login-to-Home smoke flow runs by default, because it is the one journey that depends
    on nothing but a reachable server and valid credentials. The other flows assert fixture values
    served by the mock (user 'qa-admin', project 'Payments', ...) and will fail against real data;
    pass them explicitly with -Flows when you know the server carries matching content.

    Captures land in visual-qa/dev-captures/ (gitignored) and are tagged with the device's current
    light/dark state, since this script does not force a theme.

.EXAMPLE
    .\scripts\visual-qa-maestro-dev.ps1 -BaseUrl "https://dev.example.com/" -Username "user" -Password $env:LOGFLARE_QA_PASSWORD

.EXAMPLE
    .\scripts\visual-qa-maestro-dev.ps1 -BaseUrl "https://dev.example.com/" -Username "user" -Password $env:LOGFLARE_QA_PASSWORD -Flows projects.yaml,logs-detail.yaml -Serial emulator-5554
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$Username,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$Serial,

    [string[]]$Flows = @('login-home.yaml')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
. (Join-Path $ScriptDir 'visual-qa-common.ps1')

function Write-Info([string]$Message) {
    Write-Host "[visual-qa-maestro-dev] $Message"
}

$CapturesDir = Join-Path $RepoRoot 'visual-qa\dev-captures'
# Scratch area for Maestro's own artifact tree; screenshots are collected out of it by file name.
$MaestroOutputRoot = Join-Path $CapturesDir '_maestro'
$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
$ApkPath = Join-Path $RepoRoot 'app\build\outputs\apk\debug\app-debug.apk'
$FlowsDir = Join-Path $RepoRoot '.maestro\flows'

$resolvedSerial = $null
$animationPrevious = @{}
$exitCode = 1

function Assert-NotBlank {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [AllowEmptyString()]
        [string]$Value
    )
    if ([string]::IsNullOrWhiteSpace($Value)) {
        # Never include the value: this runs for -Password too.
        throw "-$Name must be a non-empty value."
    }
}

function Invoke-DevFlow {
    param(
        [string]$DeviceSerial,
        [string]$FlowName,
        [string]$Theme
    )
    $flowPath = Join-Path $FlowsDir $FlowName
    if (-not (Test-Path -LiteralPath $flowPath)) {
        throw "Missing Maestro flow: $flowPath"
    }
    $relativeFlow = ConvertTo-MaestroRelativePath -AbsolutePath $flowPath
    $relativeOutput = ConvertTo-MaestroRelativePath -AbsolutePath $CapturesDir

    $flowOutDir = Join-Path $MaestroOutputRoot ([System.IO.Path]::GetFileNameWithoutExtension($FlowName))
    if (Test-Path -LiteralPath $flowOutDir) {
        Remove-Item -LiteralPath $flowOutDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $flowOutDir | Out-Null

    # Deliberately logs the flow and theme but never the credentials.
    Write-Info ("maestro test {0} theme={1}" -f $FlowName, $Theme)
    Push-Location $RepoRoot
    try {
        # Maestro resolves ${...} only from --env pairs; ambient environment variables are ignored
        # and silently become the literal string "undefined" inside the flow.
        & maestro --device $DeviceSerial test --test-output-dir $flowOutDir `
            -e "QA_BASE_URL=$BaseUrl" `
            -e "QA_USERNAME=$Username" `
            -e "QA_PASSWORD=$Password" `
            -e "QA_THEME=$Theme" `
            -e "QA_OUTPUT_DIR=$relativeOutput" `
            $relativeFlow
        if ($LASTEXITCODE -ne 0) {
            throw ("Maestro flow failed: {0} (exit={1}). Captures kept under {2} for diagnosis." -f `
                    $FlowName, $LASTEXITCODE, $CapturesDir)
        }
    }
    finally {
        Pop-Location
    }

    $shots = @(Get-ChildItem -LiteralPath $flowOutDir -Recurse -Filter '*.png' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]takeScreenshot[\\/]' })
    foreach ($shot in $shots) {
        Move-Item -LiteralPath $shot.FullName -Destination (Join-Path $CapturesDir $shot.Name) -Force
    }
    Write-Info ("collected {0} screenshot(s) from {1}" -f $shots.Count, $FlowName)
}

try {
    Assert-NotBlank -Name 'BaseUrl' -Value $BaseUrl
    Assert-NotBlank -Name 'Username' -Value $Username
    Assert-NotBlank -Name 'Password' -Value $Password
    if ($null -eq $Flows -or $Flows.Count -eq 0) {
        throw '-Flows must name at least one flow, or be omitted to run the login smoke flow.'
    }

    Assert-Command -Name 'adb'
    Assert-Command -Name 'maestro'
    Assert-Java17
    $resolvedSerial = Resolve-EmulatorSerial -Serial $Serial
    # No Assert-DeviceProfile here on purpose: nothing is compared, so any device is usable.

    Write-Info ("Serial={0} Flows=[{1}]" -f $resolvedSerial, ($Flows -join ', '))
    Write-Info ("BaseUrl={0} Username={1}" -f $BaseUrl, $Username)

    Write-Info 'Building and installing the debug APK'
    Push-Location $RepoRoot
    try {
        & $Gradlew ':app:assembleDebug'
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle :app:assembleDebug failed with exit $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
    if (-not (Test-Path -LiteralPath $ApkPath)) {
        throw "Debug APK not found at $ApkPath"
    }
    & adb -s $resolvedSerial install -r $ApkPath | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed with exit $LASTEXITCODE"
    }

    New-Item -ItemType Directory -Force -Path $CapturesDir | Out-Null
    Capture-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious
    Disable-AnimationScales -Serial $resolvedSerial

    # The theme is reported, not forced: a diagnostic run should show the device as the tester has it.
    $nightState = (& adb -s $resolvedSerial shell cmd uimode night | Out-String)
    # Resolve-UiModeNightState returns 'yes' / 'no' / $null, not a boolean.
    $theme = if ((Resolve-UiModeNightState -Raw $nightState) -eq 'yes') { 'dark' } else { 'light' }
    Write-Info ("Device theme detected as '{0}'" -f $theme)

    foreach ($flow in $Flows) {
        # Same per-flow reset the mock runner uses, minus the fixture reset: clear app state, then
        # deny notifications so the system dialog cannot cover the login screen, then re-zero the
        # animation scales in case something restored them.
        Write-Info ("Preparing device for flow={0}" -f $flow)
        & adb -s $resolvedSerial shell pm clear com.logflare.android | Out-Null
        Deny-PostNotificationsIfPresent -DeviceSerial $resolvedSerial
        Disable-AnimationScales -Serial $resolvedSerial
        Invoke-DevFlow -DeviceSerial $resolvedSerial -FlowName $flow -Theme $theme
    }

    Write-Info ("Dev run completed. Captures under {0} -- diagnostic only, nothing was compared." -f $CapturesDir)
    $exitCode = 0
}
catch {
    # Not Write-Error: under ErrorActionPreference 'Stop' that terminates the script before the
    # finally block's cleanup can be reasoned about, and surfaces as an exception to any caller.
    [Console]::Error.WriteLine($_.Exception.Message)
    $exitCode = 1
}
finally {
    if ($null -ne $resolvedSerial) {
        try {
            Restore-AnimationScales -Serial $resolvedSerial -Previous $animationPrevious
        }
        catch {
            Write-Warning ("Failed to restore animation scales: {0}" -f $_.Exception.Message)
        }
    }
    # Belt and braces: nothing here sets these, but a leftover from another runner in the same shell
    # would otherwise outlive this script.
    Remove-Item Env:QA_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:QA_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:QA_BASE_URL -ErrorAction SilentlyContinue
}

exit $exitCode
