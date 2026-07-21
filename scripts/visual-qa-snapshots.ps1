#Requires -Version 5.1
<#
.SYNOPSIS
    Runs the JVM (Roborazzi) snapshot tier.

.DESCRIPTION
    A thin wrapper over the Roborazzi Gradle tasks. This tier needs no device and is reproducible on
    any machine; the device tier lives in visual-qa-maestro-mock.ps1.

    Record rewrites the tracked baselines under app/src/test/snapshots/images. Verify never writes
    baselines -- on a mismatch it leaves *_actual.png and *_compare.png behind for review.

.EXAMPLE
    .\scripts\visual-qa-snapshots.ps1 -Mode Verify
    .\scripts\visual-qa-snapshots.ps1 -Mode Record
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Verify', 'Record')]
    [string]$Mode
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
. (Join-Path $ScriptDir 'visual-qa-common.ps1')

function Write-Info([string]$Message) {
    Write-Host "[visual-qa-snapshots] $Message"
}

$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
$ReportsDir = Join-Path $RepoRoot 'app\build\reports\roborazzi'
$OutputsDir = Join-Path $RepoRoot 'app\build\outputs\roborazzi'
$BaselinesDir = Join-Path $RepoRoot 'app\src\test\snapshots\images'

# ValidateSet already rejects anything else, but the check is repeated so the contract survives an
# edit to the param block.
$GradleTask = switch ($Mode) {
    'Record' { ':app:recordRoborazziDebug' }
    'Verify' { ':app:verifyRoborazziDebug' }
    default { throw "Invalid -Mode '$Mode'. Allowed values: Verify, Record." }
}

try {
    Assert-Java17
    if (-not (Test-Path -LiteralPath $Gradlew)) {
        throw "Missing Gradle wrapper: $Gradlew"
    }

    Write-Info ("Mode={0} Task={1}" -f $Mode, $GradleTask)
    Push-Location $RepoRoot
    try {
        & $Gradlew $GradleTask
        $gradleExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($gradleExit -ne 0) {
        # The failure text alone does not say where to look, and the comparison images are the whole
        # point of a failed Verify.
        Write-Warning ("Roborazzi {0} failed with exit {1}. Artifacts:" -f $Mode, $gradleExit)
        Write-Warning ("  comparison images : {0}" -f $OutputsDir)
        Write-Warning ("  HTML report       : {0}" -f (Join-Path $ReportsDir 'index.html'))
        Write-Warning ("  tracked baselines : {0}" -f $BaselinesDir)
        throw ("Roborazzi {0} failed with exit {1}." -f $Mode, $gradleExit)
    }

    if ($Mode -eq 'Record') {
        Write-Info ("Recorded baselines under {0}. Review the diff before committing." -f $BaselinesDir)
    }
    else {
        Write-Info 'Verify completed: all JVM snapshots matched.'
    }
    exit 0
}
catch {
    # Not Write-Error: under ErrorActionPreference 'Stop' that terminates before the exit below and
    # surfaces as an exception to any caller instead of a plain nonzero exit.
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
