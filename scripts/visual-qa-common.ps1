#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) {
        throw "Required command '$Name' was not found on PATH. Install it or add it to PATH, then retry."
    }
}

function Assert-Java17 {
    Assert-Command -Name 'java'
    $versionText = & java -version 2>&1 | Out-String
    if ($versionText -notmatch 'version\s+"?(1[7-9]|[2-9]\d)[\."]') {
        throw "Java 17+ is required for visual QA tools. Detected: $($versionText.Trim())"
    }
}

function Resolve-EmulatorSerial {
    param(
        [string]$Serial
    )
    Assert-Command -Name 'adb'
    $raw = & adb devices -l 2>&1 | Out-String
    $lines = $raw -split "`r?`n" | Where-Object { $_ -match '^\S+\s+device\b' }
    $serials = @()
    foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+device\b') {
            $serials += $Matches[1]
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        if ($serials -notcontains $Serial) {
            throw "Requested serial '$Serial' is not a connected 'device' entry. Connected: [$($serials -join ', ')]. Raw adb output:`n$raw"
        }
        return $Serial
    }

    if ($serials.Count -eq 0) {
        throw "No connected adb devices found (state=device). Connect the QA device, then retry. Raw adb output:`n$raw"
    }
    if ($serials.Count -gt 1) {
        throw "Multiple adb devices connected ($($serials.Count)): [$($serials -join ', ')]. Pass -Serial <adb serial> to select one."
    }
    return $serials[0]
}

function Assert-DeviceProfile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedApi,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedSize,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedDensity
    )
    Assert-Command -Name 'adb'

    $api = (& adb -s $Serial shell getprop ro.build.version.sdk).Trim()
    if ($api -ne $ExpectedApi) {
        throw "Device API level must be $ExpectedApi. Found API='$api' on serial '$Serial'."
    }

    $sizeRaw = (& adb -s $Serial shell wm size) | Out-String
    $physical = $null
    $override = $null
    if ($sizeRaw -match 'Physical size:\s*(\d+)x(\d+)') {
        $physical = "{0}x{1}" -f $Matches[1], $Matches[2]
    }
    if ($sizeRaw -match 'Override size:\s*(\d+)x(\d+)') {
        $override = "{0}x{1}" -f $Matches[1], $Matches[2]
    }
    $effective = if ($null -ne $override) { $override } else { $physical }
    if ($effective -ne $ExpectedSize) {
        throw "Device size must be $ExpectedSize (physical/override effective). Found effective='$effective' from:`n$sizeRaw"
    }

    $densityRaw = (& adb -s $Serial shell wm density) | Out-String
    $density = $null
    if ($densityRaw -match 'Override density:\s*(\d+)') {
        $density = $Matches[1]
    }
    elseif ($densityRaw -match 'Physical density:\s*(\d+)') {
        $density = $Matches[1]
    }
    if ($density -ne $ExpectedDensity) {
        throw "Device density must be $ExpectedDensity. Found density='$density' from:`n$densityRaw"
    }
}

function Get-AnimationScaleValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [ValidateSet('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')]
        [string]$Key
    )
    $value = (& adb -s $Serial shell settings get global $Key).Trim()
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq 'null') {
        return '1.0'
    }
    return $value
}

function Capture-AnimationScales {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [hashtable]$Previous
    )
    Assert-Command -Name 'adb'
    $Previous['window_animation_scale'] = Get-AnimationScaleValue -Serial $Serial -Key 'window_animation_scale'
    $Previous['transition_animation_scale'] = Get-AnimationScaleValue -Serial $Serial -Key 'transition_animation_scale'
    $Previous['animator_duration_scale'] = Get-AnimationScaleValue -Serial $Serial -Key 'animator_duration_scale'
}

function Disable-AnimationScales {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial
    )
    Assert-Command -Name 'adb'
    & adb -s $Serial shell settings put global window_animation_scale 0 | Out-Null
    & adb -s $Serial shell settings put global transition_animation_scale 0 | Out-Null
    & adb -s $Serial shell settings put global animator_duration_scale 0 | Out-Null
}

function Restore-AnimationScales {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [hashtable]$Previous
    )
    Assert-Command -Name 'adb'
    foreach ($key in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
        if ($Previous.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace([string]$Previous[$key])) {
            & adb -s $Serial shell settings put global $key $Previous[$key] | Out-Null
        }
    }
}

function Get-ScreenOffTimeout {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial
    )
    Assert-Command -Name 'adb'
    $value = (& adb -s $Serial shell settings get system screen_off_timeout | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq 'null') {
        return $null
    }
    return $value
}

function Set-ScreenOffTimeout {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [string]$Milliseconds
    )
    Assert-Command -Name 'adb'
    & adb -s $Serial shell settings put system screen_off_timeout $Milliseconds | Out-Null
    # A locked or dozing screen breaks every Maestro flow; wake before capturing.
    & adb -s $Serial shell input keyevent KEYCODE_WAKEUP | Out-Null
}

function Resolve-UiModeNightState {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Raw
    )
    $text = $Raw.Trim()
    if ($text -match '(?i)Night mode:\s*yes\b') { return 'yes' }
    if ($text -match '(?i)Night mode:\s*no\b') { return 'no' }
    if ($text -eq 'yes') { return 'yes' }
    if ($text -eq 'no') { return 'no' }
    return $null
}

function Set-UiMode {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Serial,
        [Parameter(Mandatory = $true)]
        [ValidateSet('light', 'dark')]
        [string]$Theme
    )
    Assert-Command -Name 'adb'
    $expected = if ($Theme -eq 'dark') { 'yes' } else { 'no' }
    & adb -s $Serial shell cmd uimode night $expected | Out-Null

    $deadline = (Get-Date).AddSeconds(20)
    $currentRaw = $null
    $parsed = $null
    do {
        Start-Sleep -Milliseconds 250
        $currentRaw = (& adb -s $Serial shell cmd uimode night | Out-String).Trim()
        $parsed = Resolve-UiModeNightState -Raw $currentRaw
        if ($parsed -eq $expected) {
            return
        }
    } while ((Get-Date) -lt $deadline)

    throw ("Timed out waiting for UI mode '{0}' (expected Night mode: {1}) on serial '{2}'. Last raw='{3}' parsed='{4}'" -f `
            $Theme, $expected, $Serial, $currentRaw, $parsed)
}

function Wait-HttpHealth {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = $null
    while ((Get-Date) -lt $deadline) {
        try {
            if ($PSVersionTable.PSVersion.Major -ge 6) {
                $response = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 2 -UseBasicParsing
            }
            else {
                $response = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
            }
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return
            }
            $lastError = "HTTP $($response.StatusCode)"
        }
        catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for health at $Url. Last error: $lastError"
}
