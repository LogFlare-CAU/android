#Requires -Version 5.1
<#
.SYNOPSIS
    Fails if the release build carries any QA-only affordance.

.DESCRIPTION
    The visual QA tier deliberately weakens the debug build: cleartext HTTP to a mock server, fixture
    credentials, and QA-only endpoints. None of that may reach a release artifact. This guard builds
    release and asserts:

      - usesCleartextTraffic is explicitly false in the merged manifest
      - no activity/service/receiver whose name mentions 'qa' or 'maestro'
      - the APK contains neither 'qa-password' nor '/__qa/'

    android.permission.INTERNET is expected and allowed.

    The APK is read as a zip and each entry is scanned decompressed -- a raw byte scan of the .apk
    would miss anything stored deflated, which is most of it. Release currently sets
    isMinifyEnabled = false, so identifiers and string constants are not obfuscated and a literal
    scan is meaningful; if minification is ever enabled, revisit the string checks.

.PARAMETER SkipBuild
    Reuse the existing release artifacts instead of running Gradle. For fast re-runs only.

.EXAMPLE
    .\scripts\verify-release-isolation.ps1
#>
param(
    [switch]$SkipBuild,

    # Overridable so the failure paths can be exercised against crafted inputs.
    [string]$ManifestPath,
    [string]$ApkPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path

function Write-Info([string]$Message) {
    Write-Host "[verify-release-isolation] $Message"
}

$Gradlew = Join-Path $RepoRoot 'gradlew.bat'
$DefaultManifest = Join-Path $RepoRoot 'app\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml'
$DefaultApkDir = Join-Path $RepoRoot 'app\build\outputs\apk\release'

# Strings that must never survive into a release artifact.
$ForbiddenStrings = @('qa-password', '/__qa/')
# Component name fragments that would mean a QA-only entry point shipped.
$ForbiddenComponentFragments = @('qa', 'maestro')

$violations = New-Object System.Collections.Generic.List[string]

function Add-Violation([string]$Message) {
    $violations.Add($Message)
    Write-Host "  VIOLATION $Message"
}

function Test-ReleaseManifest {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Merged release manifest not found at $Path. Run without -SkipBuild, or pass -ManifestPath."
    }
    Write-Info ("manifest {0}" -f $Path)

    [xml]$xml = Get-Content -LiteralPath $Path -Raw
    $androidNs = 'http://schemas.android.com/apk/res/android'
    $application = $xml.manifest.application
    if ($null -eq $application) {
        Add-Violation 'merged manifest has no <application> element'
        return
    }

    $cleartext = $application.GetAttribute('usesCleartextTraffic', $androidNs)
    if ($cleartext -ne 'false') {
        # Absent counts as a violation: the platform default varies by targetSdk, so silence is not
        # the same as a guarantee.
        $shown = if ([string]::IsNullOrEmpty($cleartext)) { '<absent>' } else { $cleartext }
        Add-Violation ("usesCleartextTraffic must be explicitly false, found {0}" -f $shown)
    }

    foreach ($kind in 'activity', 'service', 'receiver') {
        foreach ($node in $application.SelectNodes($kind)) {
            $name = $node.GetAttribute('name', $androidNs)
            if ([string]::IsNullOrWhiteSpace($name)) { continue }
            foreach ($fragment in $ForbiddenComponentFragments) {
                if ($name -match "(?i)$([regex]::Escape($fragment))") {
                    Add-Violation ("<{0}> '{1}' mentions '{2}'" -f $kind, $name, $fragment)
                }
            }
        }
    }
}

function Test-ReleaseApk {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Release APK not found at $Path. Run without -SkipBuild, or pass -ApkPath."
    }
    Write-Info ("apk {0} ({1:N0} bytes)" -f $Path, (Get-Item -LiteralPath $Path).Length)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    # Latin-1 maps every byte to the same-numbered char, so decompressed entry bytes can be searched
    # with .NET's own IndexOf. A byte-by-byte scan in PowerShell would take minutes on a 12 MB APK.
    $latin1 = [System.Text.Encoding]::GetEncoding('ISO-8859-1')
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.Length -eq 0) { continue }
            $stream = $entry.Open()
            try {
                $buffer = New-Object System.IO.MemoryStream
                $stream.CopyTo($buffer)
                $text = $latin1.GetString($buffer.ToArray())
                $buffer.Dispose()
            }
            finally {
                $stream.Dispose()
            }

            foreach ($needle in $ForbiddenStrings) {
                if ($text.IndexOf($needle, [System.StringComparison]::Ordinal) -ge 0) {
                    Add-Violation ("APK entry '{0}' contains '{1}'" -f $entry.FullName, $needle)
                }
            }
        }
    }
    finally {
        $zip.Dispose()
    }
}

try {
    if (-not $SkipBuild) {
        Write-Info 'Building release'
        Push-Location $RepoRoot
        try {
            & $Gradlew ':app:assembleRelease'
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle :app:assembleRelease failed with exit $LASTEXITCODE"
            }
        }
        finally {
            Pop-Location
        }
    }

    $manifest = if ([string]::IsNullOrWhiteSpace($ManifestPath)) { $DefaultManifest } else { $ManifestPath }

    $apk = $ApkPath
    if ([string]::IsNullOrWhiteSpace($apk)) {
        # The project has no release signingConfig, so the artifact is app-release-unsigned.apk.
        # Matching on extension keeps the guard working if signing is added later.
        $candidates = @(Get-ChildItem -LiteralPath $DefaultApkDir -Filter '*.apk' -File -ErrorAction SilentlyContinue)
        if ($candidates.Count -eq 0) {
            throw "No release APK found under $DefaultApkDir. Run without -SkipBuild."
        }
        if ($candidates.Count -gt 1) {
            throw ("Multiple release APKs under {0}: [{1}]. Pass -ApkPath to choose one." -f `
                    $DefaultApkDir, (($candidates | ForEach-Object { $_.Name }) -join ', '))
        }
        $apk = $candidates[0].FullName
    }

    Test-ReleaseManifest -Path $manifest
    Test-ReleaseApk -Path $apk

    if ($violations.Count -gt 0) {
        # Deliberately not Write-Error: this script runs under ErrorActionPreference 'Stop', which
        # would turn it into a terminating exception, skip the exit below, and make the guard awkward
        # to call from another script. Failures go to stderr with an explicit exit code instead.
        [Console]::Error.WriteLine("Release isolation FAILED with $($violations.Count) violation(s); see above.")
        exit 1
    }

    Write-Info 'Release isolation passed: no cleartext, no QA components, no QA strings.'
    exit 0
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
