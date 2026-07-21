# Visual QA

Local-only visual regression suite. Two independent tiers:

| Tier | Tool | Runs on | Baselines |
|---|---|---|---|
| Screen snapshots | Roborazzi + Robolectric | JVM, no device | `app/src/test/snapshots/images/` (tracked) |
| Device journeys | Maestro + mock server | Physical Android device | `visual-qa/device-baselines/` (tracked) |

The JVM tier renders stateless `*Content` composables at a fixed `412 x 915 dp @ 420 dpi` viewport and is fully
reproducible on any machine. The device tier drives the real installed app through real navigation against a
deterministic host-side mock server, and is reproducible **only on the reference device below**.

---

## Reference device

The device tier is pinned to one physical phone. `Assert-DeviceProfile` fails the run if any value differs:

| Property | Required value |
|---|---|
| API level | `36` (Android 16) |
| Effective size | `1440x3120` |
| Density | `600` |

Recorded on: Samsung SM-S948N (Galaxy S26 Ultra), native resolution, no `wm` override.

**Why native and not a downscaled profile.** Forcing `wm size 1080x2400` would change the aspect ratio
(19.5:9 → 20:9) and distort layout. Asserting the native geometry instead means that if the phone's display
resolution is later changed in Samsung settings, an `Override size` line appears and the run **fails loudly**
rather than silently rewriting the baseline set.

**These baselines are device-specific.** OEM fonts, status bar icons, punch-hole cutout and rounded corners are
all baked in. They cannot be verified on an emulator, another phone, or CI. Only the JVM tier is portable.

---

## Prerequisites

### 1. JDK 17+

```powershell
java -version
```

Java 21 is fine. `Assert-Java17` accepts 17 and above.

### 2. Maestro CLI (native Windows)

Download `maestro.zip` from the [releases page](https://github.com/mobile-dev-inc/Maestro/releases/latest),
extract it, and put its `bin` directory on your **user** PATH:

```powershell
$bin = "$env:LOCALAPPDATA\maestro\bin"
$userPath = [Environment]::GetEnvironmentVariable('Path','User')
[Environment]::SetEnvironmentVariable('Path', "$userPath;$bin", 'User')
```

Verified with Maestro **2.7.0**. Do not use `setx PATH "%PATH%;..."` — it flattens system PATH into user PATH
and truncates at 1024 characters.

### 3. adb from the Android SDK — check this before anything else

```powershell
(Get-Command adb).Source
adb version
```

You need **adb 30+** (wireless debugging) — the Android SDK ships 37.x at
`%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

> **Trap:** other software installs its own ancient adb into the **system** PATH. On this machine, Touch Portal
> puts adb **28.0.3** at `C:\Program Files (x86)\Touch Portal\plugins\adb\platform-tools`. Because Windows
> resolves system PATH before user PATH, adding the SDK path to your user PATH is **not enough**. An adb 28
> client that contacts an adb 37 server kills that server and starts its own — dropping the wireless connection
> mid-run. The scripts call bare `adb`, so the resolved binary must be the modern one:
>
> ```powershell
> $env:PATH = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH"
> ```
>
> Prepend it in the shell you run the scripts from, or remove the stale entry from the system PATH (admin).

### 4. Connect the device

Wired needs only USB debugging plus a **data-capable** cable. Wireless debugging (Android 11+):

```powershell
# Phone: 개발자 옵션 > 무선 디버깅 > 페어링 코드로 기기 페어링
adb pair <ip>:<pairing-port> <6-digit-code>

# Phone: 무선 디버깅 main screen shows the connect port; or discover it:
adb mdns services
adb connect <ip>:<connect-port>
```

Pairing is keyed to the device GUID, not its IP — after an IP change, `adb connect` again, no re-pairing.
Give the phone a static IP or DHCP reservation so the connect step stays stable.

> **`-Serial` is mandatory in practice.** A wirelessly connected device appears **twice** in `adb devices` —
> once by IP and once by its mDNS name — and `Resolve-EmulatorSerial` refuses to guess between them. Pass the
> mDNS name, which survives IP changes:
>
> ```
> adb-R5KL102RX3M-ThQwRn._adb-tls-connect._tcp
> ```

### 5. Unlock the phone and keep it unlocked

The runner sets `screen_off_timeout` to 30 minutes and restores it afterwards, and issues `KEYCODE_WAKEUP`.
That wakes the screen but **does not dismiss a lock screen** — every flow fails if the phone is locked. Do not
touch the phone while flows run; stray taps break the journey.

---

## Commands

### JVM snapshots

```powershell
.\gradlew.bat :app:recordRoborazziDebug    # rewrite baselines
.\gradlew.bat :app:verifyRoborazziDebug    # compare only, never writes baselines
```

### Device journeys

```powershell
$env:PATH = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:LOCALAPPDATA\maestro\bin;$env:PATH"
$serial = "adb-R5KL102RX3M-ThQwRn._adb-tls-connect._tcp"

.\scripts\visual-qa-maestro-mock.ps1 -Mode Record -Serial $serial
.\scripts\visual-qa-maestro-mock.ps1 -Mode Verify -Serial $serial
```

`Record` stages every capture first and promotes the **complete** set only after all 10 flow runs pass, so a
mid-run failure never leaves a half-updated baseline set. `Verify` never writes baselines; on mismatch it writes
magenta diff images to `visual-qa/device-diffs/`.

A full run is 5 flows x 2 themes and takes several minutes over wireless adb.

### What the runner does per theme

1. `:app:assembleDebug` and `:visual-qa:tools:installDist`
2. `adb install -r` the debug APK
3. `adb reverse tcp:18000 tcp:18000`
4. Start the mock server on `127.0.0.1:18000`, wait for `/__qa/health`
5. Per flow: `POST /__qa/reset`, `pm clear`, revoke `POST_NOTIFICATIONS`, set UI mode, zero animation scales
6. Run the Maestro flow, collect its screenshots
7. Compare (`Verify`) or stage then promote (`Record`)
8. Restore animation scales, UI mode, screen timeout; remove the reverse binding

---

## Artifacts

| Path | Tracked | Contents |
|---|---|---|
| `app/src/test/snapshots/images/` | yes | JVM snapshot baselines |
| `visual-qa/device-baselines/` | yes | Device baselines, 9 checkpoints x 2 themes = 18 PNGs |
| `visual-qa/device-captures/` | no | This run's captures, plus `_maestro/` scratch |
| `visual-qa/device-diffs/` | no | Magenta diffs from a failed `Verify` |
| `visual-qa/device-baselines-staging/` | no | `Record` staging area before promotion |

Checkpoints: `home`, `logs_detail`, `projects_list`, `project_detail`, `project_settings`, `my_page`,
`add_member`, `edit_member`, `logout_confirmation`.

---

## Resolved issues

These were the blockers of 2026-07-21; the notes stay because the reasoning explains the current design.

### The status bar band is cropped off every capture

Flows crop on `app_root`, which is edge-to-edge and so reaches under the system status bar. Every capture
carried the live clock, battery level and notification icons, and no two runs agreed:

```
compare home_light.png          RESULT Changed ratio=0.00066551 count=2990
compare logs_detail_light.png   RESULT Changed ratio=0.00066551 count=2990
compare projects_list_light.png RESULT Changed ratio=0.00076678 count=3445
```

Identical change counts across unrelated screens were the tell: the differing region was screen-independent
chrome. Measuring the magenta in all nine diffs put **every** changed pixel in `y = 57..107`, with no app
content affected.

**SystemUI demo mode does not work on One UI.** `settings put global sysui_demo_allowed 1` followed by the
`com.android.systemui.demo` broadcasts is accepted (`Broadcast completed: result=0`) but changes nothing —
the bar keeps showing the real clock and battery. Do not spend time on it again.

So the band is discarded instead. The runner calls `tools crop-top` on each collected screenshot:

```
tools crop-top --image <png> --pixels 139 --expect-height 3120
```

`139` is the device's reported top inset — `adb shell dumpsys window displays` prints it as
`DisplayCutout{insets=Rect(0, 139 - 0, 0)}` and `overrideNonDecorInsets=[0,139]`. Rows `108..142` are empty on
this device, so the cut lands in the gap between the status bar and the app's first pixel. Baselines are
therefore **2981px tall, not 3120**.

`--expect-height` is what keeps this honest: cropping an already-cropped file fails loudly instead of eating
real content, as does a capture from a device with different geometry.

Verified on the artefacts that produced the failures above — crop alone took them from 1/18 to **18/18 Match**.

Do **not** raise `maxChangedRatio` if something like this recurs. The threshold `0.0001` is what makes the
comparison meaningful; those diffs were 2-11x over it.

### Top app bar now follows the theme

`core/designsystem/.../navigation/TopAppBar.kt` painted itself with absolute palette values, so the bar stayed
a white slab with dark ink while the rest of the screen went dark. Now:

- background `neutral.white` → `AppTheme.colors.surface` (light `#FFFFFF` unchanged, dark `#424242`)
- title `neutral.black` → `AppTheme.colors.onSurface` (light `#212121`, dark `#FAFAFA`)
- the wordmark is drawn with `Icon(tint = onSurface)` instead of `Image`. The drawable is single-ink at
  `#212121`, so tinting it as a mask serves both themes from one asset and leaves the light rendering
  byte-identical — no themed asset needed.

`iconTint` stays `neutral.s50` on purpose. It is a fixed muted grey that reads as secondary against both
surfaces (2.6:1 on light, 3.7:1 on dark, above the 3:1 non-text minimum where it is tightest); promoting it to
`onSurface` would restyle the light bar rather than fix a bug.

26 of 147 JVM snapshots changed, and every changed pixel sat inside the 56dp (147px) bar: dark repainted the
whole band (`y = 0..146`), light moved only the title glyphs (`y = 58..97`) from `#1A1A1A` to `#212121`.

The wordmark itself appears only in `MainScaffold`, which the JVM tier does not render, so that part of the
change is covered by the device tier alone.

## Known issues

### Port 18000, not 8000

The mock server and the reverse binding use **18000**. Port 8000 was unusable on both ends of this setup: Docker
Desktop holds it on the host, and something on the phone already listens on it, so `adb reverse` failed with
`cannot bind listener: Address already in use`. If 18000 ever collides, change `$QaServerPort` in
`scripts/visual-qa-maestro-mock.ps1` — everything else derives from it.

### The app reaches the server over `adb reverse`, not `10.0.2.2`

`10.0.2.2` is an emulator-only alias for the host loopback and does nothing on a physical device. The base URL
is `http://localhost:18000`, typed into the login form by `_login.yaml`. It must be `localhost` and not
`127.0.0.1`: the debug `network_security_config.xml` whitelists cleartext by hostname, and
`ServerConfigRepository` special-cases `localhost` for port handling.

### Maestro quirks this suite works around

- **Ambient environment variables are ignored.** Maestro resolves `${...}` only from `--env`/`-e` pairs. Values
  passed via `$env:` silently become the literal string `undefined` — which surfaces as a confusing downstream
  failure (a field "not found") rather than an obvious substitution error.
- **Screenshot paths are always nested and absolute paths are rejected.** Maestro writes to
  `<output dir>/<timestamp>/<flow>/takeScreenshot/<given path>.png`, and an absolute path throws
  `InvalidPathException: Illegal char <:>`. The runner therefore captures into a scratch tree and collects the
  PNGs back out by file name, and fails if a flow reports success but produced no screenshots.
- **The soft keyboard hides fields.** `_login.yaml` calls `hideKeyboard` after each `inputText`; without it the
  Password field sits under the keyboard and is not found.

### `MaestroMockContractTest` used to report a false pass — fixed

The test asserts against `scripts/*.ps1` and `.maestro/**`, which live outside the module's source set. Gradle
could not infer them, so the task went `UP-TO-DATE` and skipped after those files changed. `visual-qa/tools/
build.gradle.kts` now declares them with `inputs.files(...)` / `inputs.dir(...)`, and the task re-runs on a
content change. `--rerun-tasks` is no longer needed.

Note that Gradle keys on content, not timestamps: `touch`-ing a script still shows `UP-TO-DATE`, correctly.

---

## Not yet built

Task 10 of `docs/superpowers/plans/2026-07-17-android-visual-qa.md` is only partly done. Still missing:

- `scripts/visual-qa-snapshots.ps1` — thin wrapper over the Roborazzi Gradle tasks
- `scripts/visual-qa-maestro-dev.ps1` — run journeys against a real development server, diagnostic captures only
- `scripts/verify-release-isolation.ps1` — assert release builds carry no cleartext, QA activities, or credentials

That plan also still describes the original Pixel 7 API 35 emulator target, `10.0.2.2`, and port 8000. This
document supersedes it for the device tier.
