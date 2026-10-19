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

**These baselines are device-specific.** OEM fonts, punch-hole cutout and rounded corners are all baked in.
They cannot be verified on an emulator, another phone, or CI. Only the JVM tier is portable.

**The navigation mode is part of the reference configuration.** The status bar is cropped off, but the bottom
of the frame still holds the app's own bottom bar and, below it, the system gesture pill. Switching the phone
between gesture and 3-button navigation changes that region and invalidates every baseline. `Assert-DeviceProfile`
does not check this, so it fails as a comparison mismatch rather than a clear message.

Before the rework, nothing else in frame varied with time: two `Verify` runs an hour apart both matched 18/18.
The reworked screens show log times, which do vary -- see [Log times](#log-times-are-relative-to-the-run) below.

### Baselines are pending re-recording (rework, 2026-10-19)

The rework replaced every screen, so the old 18 baselines no longer matched any flow and were deleted.
`visual-qa/device-baselines/` holds only `.gitkeep` until someone records on the reference device; until then
`Verify` stops at `Assert-BaselineManifest` (missing baselines), which is expected.

The flows were proven on an emulator (Pixel-class AVD, API 36, 1080x2400 @ 420): all 5 flows passed in both
themes and the captures were reviewed against the prototype. Emulator captures are **not** baselines (wrong
geometry, fonts and cutout), so none were promoted. To record the set on the reference device:

1. Connect the S26 Ultra as in [Prerequisites](#prerequisites), unlocked, gesture navigation, native resolution.
2. `.\scripts\visual-qa-maestro-mock.ps1 -Mode Record -Serial $serial` -- stages all 22 captures and promotes
   them with `BaselineSetPromoter` only after all 10 flow runs pass.
3. Open every PNG in `visual-qa/device-baselines/` and compare it with the prototype
   (`docs/superpowers/specs/2026-10-18-app-rework-prototype/*.dc.html`) before committing.
4. Run `-Mode Verify` once right after, to confirm the set reproduces.

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
.\scripts\visual-qa-snapshots.ps1 -Mode Record    # rewrite baselines
.\scripts\visual-qa-snapshots.ps1 -Mode Verify    # compare only, never writes baselines
```

A wrapper over `:app:recordRoborazziDebug` / `:app:verifyRoborazziDebug` that prints where the comparison images
and HTML report landed when a run fails. The Gradle tasks work directly too.

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

### Development-server journeys (diagnostic only)

```powershell
.\scripts\visual-qa-maestro-dev.ps1 -BaseUrl "https://dev.example.com/" -Username "user" -Password $env:LOGFLARE_QA_PASSWORD
.\scripts\visual-qa-maestro-dev.ps1 -BaseUrl "https://dev.example.com/" -Username "user" -Password $env:LOGFLARE_QA_PASSWORD -Flows projects.yaml,logs-detail.yaml
```

Drives the debug app against a real server and writes captures to `visual-qa/dev-captures/`. It never compares
and never records: a development server's data changes, so its captures are evidence for a human, not a pass/fail.

Only `login-home.yaml` runs by default, because it depends on nothing but a reachable server and valid
credentials. The other flows assert mock fixture values (member `minsu`, project id `101`, ...) and will fail
against real data -- pass them with `-Flows` only when the server carries matching content. Captures are tagged
with the device's current light/dark state; this runner does not force a theme.

### Release isolation guard

```powershell
.\scripts\verify-release-isolation.ps1
.\scripts\verify-release-isolation.ps1 -SkipBuild   # reuse existing release artifacts
```

Builds release and fails if any QA affordance survived: `usesCleartextTraffic` not explicitly `false` in the
merged manifest, an activity/service/receiver whose name mentions `qa` or `maestro`, or the strings
`qa-password` / `/__qa/` anywhere in the APK. `android.permission.INTERNET` is expected and allowed.

The APK is read as a zip and each entry scanned decompressed -- a raw scan of the `.apk` bytes would miss
anything stored deflated, which is nearly all of it. Release currently sets `isMinifyEnabled = false`, so string
constants are not obfuscated and a literal scan is meaningful; revisit the string checks if minification is
enabled. There is no release `signingConfig`, so the artifact is `app-release-unsigned.apk`.

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
| `visual-qa/device-baselines/` | yes | Device baselines, 11 checkpoints x 2 themes = 22 PNGs (pending, see above) |
| `visual-qa/device-captures/` | no | This run's captures, plus `_maestro/` scratch |
| `visual-qa/device-diffs/` | no | Magenta diffs from a failed `Verify` |
| `visual-qa/device-baselines-staging/` | no | `Record` staging area before promotion |
| `visual-qa/dev-captures/` | no | Development-server captures, plus `_maestro/` scratch |
| `app/build/outputs/roborazzi/` | no | JVM `*_actual.png` / `*_compare.png` from a failed Verify |

Checkpoints, by flow:

| Flow | Journey | Checkpoints |
|---|---|---|
| `login-home.yaml` | Sign in -> Home (summary strip, recent errors, projects) | `home` |
| `logs-detail.yaml` | Logs -> level sheet "Error and above" -> first row -> Log detail | `logs`, `log_detail` |
| `projects.yaml` | Projects -> api-server -> Settings | `projects_list`, `project_detail`, `project_settings` |
| `mypage-members.yaml` | My -> Add member -> back; My -> minsu -> Edit member | `my_page`, `add_member`, `edit_member` |
| `logout.yaml` | My -> Log out sheet -> Log out -> Login | `logout_sheet`, `login` |

`MaestroMockContractTest` fails if the runner's `$ExpectedCheckpoints` and the flows' screenshots drift apart.

---

## Mock server fixture

`visual-qa/tools/.../server/FixtureState.kt` mirrors the rework prototype. `POST /__qa/reset` restores it before
every flow.

- **Users:** `qa-admin` (Super Admin, the signed-in user, password `qa-password`), `jihyun` (Admin), `devops-bot`,
  `minsu`, `yuna` (Members).
- **Projects:** 101 `api-server` (`app.log`, `payment.log`, `db.log`), 102 `worker`, 103 `web-frontend`,
  104 `batch-jobs`.
- **Logs:** the prototype's eleven groups across all five levels, 23 rows with repeats (e.g. the
  NullPointerException in PaymentService x3 with a raw stack trace). Grouped by (project, errortype, message) as
  the backend does.

Routes the app calls: `/user/auth`, `/user/me`, `/user/` (+ `PATCH`/`DELETE /user/{id}`), `/project/`
(+ `PATCH`/`DELETE /project/{id}`, `/project/{id}/perm`, `/project/{id}/token`, `/project/{id}/rotate-token`,
`/project/perm/batch/reset`), `/log/summary`, `/log/events`, `/log/events/{id}`, `/fcm/data` (always "disabled"),
`POST`/`DELETE /fcm/token`. `MockServerRoutesTest` and `ModelCompatibilityTest` decode them with the app's own
`core:model` serializers. The pre-rework routes (`/log/error`, `/log/{project}/{file}`, `/user/name`) are gone.

### Log times are relative to the run

Each row is stored as an age ("1 minute ago"), not a date, and stamped against the server clock on every request.
That keeps the 24h summary counts and the relative labels ("15 min ago", "Last error 22 h ago") the same on every
run. It cannot freeze **absolute** times: the Logs list (`HH:mm`, Today/Yesterday sections) and Log detail
(first/last seen) show wall-clock times, so the `logs` and `log_detail` checkpoints differ between runs at different
times of day. The app has no debug clock override and a non-rooted phone's clock cannot be set, so those two
checkpoints will need masking (or an app-side clock seam) before `Verify` can pass on them across runs. Relative
labels also assume the phone's clock agrees with the host's to within a few seconds.

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

- **`hideKeyboard` presses Back on Android.** On Login with the keyboard already down that closes the app, so
  `_login.yaml` never calls it. It taps fields by test tag (`login_server_url`, ...; the labels are not clickable),
  waits for the scroll-into-view to settle, and types the **password first**: typed last, Maestro's `inputText`
  landed in Username although Password had focus.
- **`pm clear` and `clearState` re-arm the notification prompt.** The app asks at launch, so the runner's
  `pm revoke` cannot pre-empt it and `launchApp permissions:` did not either; `_login.yaml` taps "Don't allow"
  when it is there.
- **Sheets are separate windows.** `app_root` is not in the hierarchy while the Log out sheet is open, so
  `logout_sheet` is captured uncropped. `app_root` is edge to edge, so the geometry is the same and `crop-top`
  still applies.

- **Ambient environment variables are ignored.** Maestro resolves `${...}` only from `--env`/`-e` pairs. Values
  passed via `$env:` silently become the literal string `undefined` — which surfaces as a confusing downstream
  failure (a field "not found") rather than an obvious substitution error.
- **Screenshot paths are always nested and absolute paths are rejected.** Maestro writes to
  `<output dir>/<timestamp>/<flow>/takeScreenshot/<given path>.png`, and an absolute path throws
  `InvalidPathException: Illegal char <:>`. The runner therefore captures into a scratch tree and collects the
  PNGs back out by file name, and fails if a flow reports success but produced no screenshots.

### `MaestroMockContractTest` used to report a false pass — fixed

The test asserts against `scripts/*.ps1` and `.maestro/**`, which live outside the module's source set. Gradle
could not infer them, so the task went `UP-TO-DATE` and skipped after those files changed. `visual-qa/tools/
build.gradle.kts` now declares them with `inputs.files(...)` / `inputs.dir(...)`, and the task re-runs on a
content change. `--rerun-tasks` is no longer needed.

Note that Gradle keys on content, not timestamps: `touch`-ing a script still shows `UP-TO-DATE`, correctly.

---

## Relationship to the implementation plan

`docs/superpowers/plans/2026-07-17-android-visual-qa.md` still describes the original Pixel 7 API 35 emulator
target, `10.0.2.2`, and port 8000. **This document supersedes it for the device tier.** Its Task 10 is now
complete; the three local entry points it called for are the scripts documented above.

Two of its acceptance criteria were adapted rather than met literally: the runbook it specifies (emulator, port
8000) is obsolete, and its "`git status --short` prints nothing" check does not hold in a working tree that
carries local agent tooling.
