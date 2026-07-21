# Android Visual QA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Status (2026-07-21): all 10 tasks are implemented.** Checkboxes below were never ticked and do not
> reflect progress.
>
> Task 10's three entry points exist as `scripts/visual-qa-snapshots.ps1`,
> `scripts/visual-qa-maestro-dev.ps1` and `scripts/verify-release-isolation.ps1`. Two of its acceptance
> criteria were adapted rather than met literally: Step 4's runbook content (emulator, `10.0.2.2`, port 8000)
> is obsolete, and Step 6's "`git status --short` prints nothing" does not hold in a working tree carrying
> local agent tooling. The dev runner also defaults to the login smoke flow only -- the other journeys assert
> mock fixture values and cannot pass against a real server.
>
> **The device-tier sections of this plan are superseded by [`docs/visual-qa.md`](../../visual-qa.md).**
> The device tier now runs on a physical Samsung SM-S948N (API 36, 1440x3120, density 600) instead of a
> Pixel 7 API 35 emulator, reaches the mock server over `adb reverse` at `http://localhost:18000` instead of
> `http://10.0.2.2:8000`, and passes flow variables to Maestro with `-e` instead of ambient environment
> variables. `Assert-Pixel7Api35Profile` is now the parameterized `Assert-DeviceProfile`.
>
> Device `Verify` passes as of 2026-07-21 (18/18 checkpoints). Getting there needed two fixes recorded in
> `docs/visual-qa.md`: captures now drop the 139px status bar band (One UI ignores SystemUI demo mode), and
> `LogFlareTopAppBar` was repainted from absolute palette values onto the `surface` / `onSurface` roles.
> Device baselines are therefore `1440x2981`, and 26 JVM snapshots were re-recorded for the top bar change.

**Goal:** Build a local-only visual regression suite that snapshots every major Compose screen in light and dark themes and verifies real app journeys on a Pixel 7-compatible Android emulator.

**Architecture:** Roborazzi 1.60.0 and Robolectric 4.16.1 provide deterministic JVM snapshots of stateless screen-content composables. Maestro drives the installed debug app against either a deterministic host-side mock server or a caller-supplied development server; a JVM CLI compares mock-mode device captures and emits diff images.

**Tech Stack:** Kotlin 2.2.10, AGP 9.0.1, Gradle Wrapper 9.1.0, Compose, Roborazzi 1.60.0, Robolectric 4.16.1, Maestro CLI, JDK `HttpServer`, JDK `ImageIO`, PowerShell.

## Global Constraints

- Run Android builds and tests with `.\gradlew.bat`; do not require Docker.
- Keep the suite local-only. Do not modify `.github/workflows/android.yml`.
- JVM snapshots use a fixed `412 x 915 dp` viewport at `420 dpi`.
- Device tests use a Pixel 7-compatible API 35 AVD at `1080 x 2400` and `420 dpi`.
- Cover every major screen in explicit light and dark themes.
- Mock-server device captures are pixel-compared; development-server captures are diagnostic only.
- Recording and verification are separate commands. Verification must not modify references.
- E2E selection happens in scripts and the existing login form; add no E2E activity, receiver, deep link, or release BuildConfig flag.
- Release builds must not allow cleartext HTTP and must contain no visual-QA code or credentials.
- Keep generated actual images, diffs, reports, and development-server captures untracked.

---

### Task 1: Prove and configure the JVM screenshot toolchain

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/com/logflare/android/visual/RoborazziCompatibilityTest.kt`
- Create: `app/src/test/snapshots/images/.gitkeep`
- Modify: `.gitignore`

**Interfaces:**
- Produces Gradle tasks `recordRoborazziDebug`, `verifyRoborazziDebug`, and `compareRoborazziDebug`.
- Produces a known-good `RoborazziCompatibilityTest` used as the gate before broad snapshot authoring.

- [ ] **Step 1: Add a failing compatibility test**

```kotlin
package com.logflare.android.visual

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class RoborazziCompatibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersComposeOnAgp9() {
        compose.setContent { Text("LogFlare visual QA") }
        compose.onRoot().captureRoboImage("compatibility.png")
    }
}
```

- [ ] **Step 2: Run the test and confirm the missing-toolchain failure**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*RoborazziCompatibilityTest"
```

Expected: compilation fails because Roborazzi and Robolectric are not configured.

- [ ] **Step 3: Add exact dependency and plugin versions**

Add to `gradle/libs.versions.toml`:

```toml
roborazzi = "1.60.0"
robolectric = "4.16.1"

roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }

roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

Declare `alias(libs.plugins.roborazzi) apply false` in root `build.gradle.kts`, apply it in `app/build.gradle.kts`, and add:

```kotlin
android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    outputDir.set(file("src/test/snapshots/images"))
}

dependencies {
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 4: Record and verify the compatibility image**

Run:

```powershell
.\gradlew.bat :app:recordRoborazziDebug --tests "*RoborazziCompatibilityTest"
.\gradlew.bat :app:verifyRoborazziDebug --tests "*RoborazziCompatibilityTest"
```

Expected: both commands exit 0 and `app/src/test/snapshots/images/compatibility.png` exists.

- [ ] **Step 5: Configure tracked and generated image paths**

Keep `app/src/test/snapshots/images/**/*.png` tracked. Add only generated paths to `.gitignore`:

```gitignore
app/build/outputs/roborazzi/
app/build/reports/roborazzi/
visual-qa/output/
visual-qa/device-diffs/
visual-qa/dev-captures/
```

- [ ] **Step 6: Commit the compatibility gate**

```powershell
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/test .gitignore
git commit -m "test: configure Compose screenshot testing"
```

---

### Task 2: Add real light and dark semantic themes

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Color.kt`
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Theme.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/components/CommonComponents.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/component/common/LogFlareActionTextField.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/common/LogUiComponents.kt`
- Modify: `app/src/main/java/com/logflare/android/components/LogFlareActionTextField.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/auth/LoginScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/main/MainScaffold.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCreateScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCommonUiModels.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/projectdetail/ProjectDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/MyPageScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/AddMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/EditMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/LogoutScreen.kt`
- Create: `app/src/test/java/com/logflare/android/visual/ThemeContractTest.kt`

**Interfaces:**
- Produces `fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.
- Produces `fun LogflareandroidTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.
- Adds semantic `background`, `surface`, `surfaceVariant`, `onBackground`, `onSurface`, `onPrimary`, and `outline` fields to `AppColors`.

- [ ] **Step 1: Write failing palette contract tests**

```kotlin
@RunWith(RobolectricTestRunner::class)
class ThemeContractTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightAndDarkExposeDifferentSurfaces() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        compose.setContent {
            LogflareandroidTheme(darkTheme = false) { light = MaterialTheme.colorScheme.background }
        }
        compose.setContent {
            LogflareandroidTheme(darkTheme = true) { dark = MaterialTheme.colorScheme.background }
        }
        assertNotEquals(light, dark)
        assertTrue(light.luminance() > dark.luminance())
    }
}
```

- [ ] **Step 2: Run the test and confirm the signature failure**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ThemeContractTest"
```

Expected: compilation fails because neither theme accepts `darkTheme`.

- [ ] **Step 3: Define semantic light and dark palettes**

Extend `AppColors` with semantic roles. Keep brand and neutral primitives unchanged. Use these exact role values:

```kotlin
internal fun lightAppColors() = baseAppColors().copy(
    background = Neutral5,
    surface = White,
    surfaceVariant = Neutral10,
    onBackground = Neutral90,
    onSurface = Neutral90,
    onPrimary = Black,
    outline = Neutral40,
)

internal fun darkAppColors() = baseAppColors().copy(
    background = Neutral90,
    surface = Neutral80,
    surfaceVariant = Neutral70,
    onBackground = Neutral5,
    onSurface = Neutral5,
    onPrimary = Black,
    outline = Neutral60,
)
```

Change `AppTheme` to choose one palette from its explicit `darkTheme` argument. Change `LogflareandroidTheme` to pass that argument into `AppTheme` and choose `darkColorScheme` or `lightColorScheme` from the semantic roles.

- [ ] **Step 4: Replace visible hardcoded light-only roles**

Across the listed feature and shared-component files, apply this mapping:

```text
Color.White / AppTheme.colors.neutral.white used as page/card background -> AppTheme.colors.surface
Neutral5 used as page background -> AppTheme.colors.background
Black / Neutral90 used as primary text -> AppTheme.colors.onSurface
Neutral40 used as border -> AppTheme.colors.outline
Color.Red used as validation/error -> AppTheme.colors.red.default
```

Do not replace brand colors or log-level colors.

- [ ] **Step 5: Verify theme contracts and existing unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: all tests pass, and the theme contract confirms distinct light/dark backgrounds.

- [ ] **Step 6: Commit the theme**

```powershell
git add core/designsystem app/src/main app/src/test
git commit -m "feat: add semantic dark theme"
```

---

### Task 3: Extract auth, home, and logs render seams

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/auth/LoginScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogDetailScreen.kt`
- Create: `app/src/main/java/com/logflare/android/ui/VisualQaTags.kt`
- Create: `app/src/test/java/com/logflare/android/visual/CoreScreenRenderTest.kt`

**Interfaces:**
- Produces `LoginFormState` and `LoginScreenContent`.
- Produces `HomeScreenContent`.
- Produces `LogListScreenContent`.
- Keeps `LogDetailScreenContent` public and gives empty-detail rendering an explicit state.

- [ ] **Step 1: Add failing render tests for stable root tags**

```kotlin
@RunWith(RobolectricTestRunner::class)
class CoreScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loginRendersWithoutViewModel() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LoginScreenContent(
                    uiState = AuthUiState(),
                    form = LoginFormState(),
                    onFormChange = { _ -> },
                    onSignIn = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Login).assertExists()
    }

    @Test fun logsEmptyRendersWithoutViewModel() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LogListScreenContent(LogsUiState(), onAction = { _ -> })
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logs).assertExists()
    }
}
```

- [ ] **Step 2: Run tests and confirm missing content APIs**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*CoreScreenRenderTest"
```

Expected: compilation fails on `LoginScreenContent`, `LoginFormState`, `LogListScreenContent`, and `VisualQaTags`.

- [ ] **Step 3: Add stable tags and explicit content contracts**

Create:

```kotlin
object VisualQaTags {
    const val AppRoot = "app_root"
    const val Login = "login_screen"
    const val Home = "home_screen"
    const val Logs = "logs_list"
    const val LogDetail = "log_detail"
    const val Projects = "projects_list"
    const val ProjectCreate = "project_create"
    const val ProjectSettings = "project_settings"
    const val ProjectDetail = "project_detail"
    const val MyPage = "my_page"
    const val AddMember = "add_member"
    const val EditMember = "edit_member"
    const val Logout = "logout_confirmation"
    const val Loading = "state_loading"
    const val Empty = "state_empty"
    const val Error = "state_error"
}
```

Use these exact signatures:

```kotlin
data class LoginFormState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val serverUrlError: String? = null,
)

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    form: LoginFormState,
    onFormChange: (LoginFormState) -> Unit,
    onSignIn: () -> Unit,
)

@Composable
fun HomeScreenContent(
    authState: AuthUiState,
    projectsState: ProjectsUiState,
    logsState: LogsUiState,
    onProjectSelected: (Int) -> Unit,
    onViewMoreLogs: () -> Unit,
    onCreateProject: () -> Unit,
)

sealed interface LogListAction {
    data class OpenLog(val log: ErrorlogDTO) : LogListAction
    data object LoadMore : LogListAction
    data class SelectProject(val projectId: Int?) : LogListAction
    data class ToggleLevel(val level: LogLevel) : LogListAction
    data class ChangeSort(val sort: LogSort) : LogListAction
}

@Composable
fun LogListScreenContent(
    uiState: LogsUiState,
    onAction: (LogListAction) -> Unit,
)
```

Route composables continue collecting ViewModels and owning effects; they only translate state and callbacks into these content functions.

- [ ] **Step 4: Make login URL validation pure**

Move validation out of composition:

```kotlin
internal fun validateServerUrl(input: String): String? =
    if (input.isBlank() || Regex("""^https?://[A-Za-z0-9.\-]+(:\d+)?(/.*)?$""").matches(input.trim())) {
        null
    } else {
        "Invalid URL format"
    }
```

`LoginScreen` updates `LoginFormState.serverUrlError` only inside the server URL change callback; it must not assign mutable state during composition.

- [ ] **Step 5: Run render and existing logic tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*CoreScreenRenderTest" --tests "*ErrorLogsUiFilterTest" --tests "*PendingLogDetailStoreTest"
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit the core render seams**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: expose core screen render seams"
```

---

### Task 4: Extract project render seams and pure project-detail state

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCreateScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectSettingsScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/projectdetail/ProjectDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/projectdetail/ProjectDetailViewModel.kt`
- Create: `app/src/test/java/com/logflare/android/visual/ProjectScreenRenderTest.kt`

**Interfaces:**
- Produces stateless `ProjectListScreenContent`, `ProjectCreateScreenContent`, `ProjectSettingsScreenContent`, and `ProjectDetailScreenContent`.
- Changes `ProjectDetailUiState` to contain data only: `error: String?` and `showMoreLoading: Boolean`; callbacks are supplied to content.

- [ ] **Step 1: Write failing project content render tests**

Create one test per content API. The minimal populated contract is:

```kotlin
@Test fun projectDetailRendersFromStateOnly() {
    compose.setContent {
        LogflareandroidTheme(false) {
            ProjectDetailScreenContent(
                uiState = ProjectDetailUiState(loading = false, projectName = "Payments"),
                onBack = {},
                onOpenProjectSettings = { _ -> },
                onLevelSelected = { _ -> },
                onLogfileSelected = { _ -> },
                onSortSelected = { _ -> },
                onLogClick = { _ -> },
                onLoadMore = {},
            )
        }
    }
    compose.onNodeWithTag(VisualQaTags.ProjectDetail).assertExists()
}
```

- [ ] **Step 2: Run and confirm private/missing API failures**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ProjectScreenRenderTest"
```

Expected: compilation fails because project content functions are private or absent.

- [ ] **Step 3: Expose full-screen project content contracts**

Use these content boundaries:

```kotlin
@Composable
fun ProjectListScreenContent(
    uiState: ProjectsUiState,
    onProjectClick: (Int) -> Unit,
    onRefresh: () -> Unit,
)

sealed interface ProjectEditorAction {
    data class NameChanged(val value: String) : ProjectEditorAction
    data class KeywordChanged(val value: String) : ProjectEditorAction
    data object AddKeyword : ProjectEditorAction
    data class RemoveKeyword(val value: String) : ProjectEditorAction
    data class ToggleLevel(val value: String) : ProjectEditorAction
    data class TogglePermission(val username: String) : ProjectEditorAction
    data object Submit : ProjectEditorAction
    data object CopyToken : ProjectEditorAction
    data object Delete : ProjectEditorAction
}

@Composable
fun ProjectCreateScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
)

@Composable
fun ProjectSettingsScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
)
```

The route composables retain clipboard, snackbar, project initialization, and navigation effects.

- [ ] **Step 4: Remove callbacks from project-detail UI state**

Replace `ShowMoreState(onClick = {})` in `ProjectDetailUiState` with:

```kotlin
data class ProjectDetailUiState(
    val loading: Boolean = true,
    val projectId: Int = 0,
    val projectName: String = "",
    val settingsLabel: String = "Project Settings",
    val logs: List<ProjectDetailLog> = emptyList(),
    val filterState: ProjectDetailFilterState = ProjectDetailFilterState(),
    val showMoreLoading: Boolean = false,
    val error: String? = null,
)
```

Pass `onLoadMore: () -> Unit` directly into `ProjectDetailScreenContent`.

Expose the full content function with this exact signature:

```kotlin
@Composable
fun ProjectDetailScreenContent(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLevelSelected: (LogLevel) -> Unit,
    onLogfileSelected: (Int) -> Unit,
    onSortSelected: (LogSort) -> Unit,
    onLogClick: (ProjectDetailLog) -> Unit,
    onLoadMore: () -> Unit,
)
```

- [ ] **Step 5: Verify project tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ProjectScreenRenderTest" --tests "*ProjectEditorValidationTest"
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit project render seams**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: expose project screen render seams"
```

---

### Task 5: Extract My Page render seams

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/MyPageScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/AddMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/EditMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/LogoutScreen.kt`
- Create: `app/src/test/java/com/logflare/android/visual/MyPageScreenRenderTest.kt`

**Interfaces:**
- Produces public `MyPageContent`, `AddMemberScreenContent`, `EditMemberScreenContent`, and `LogoutScreenContent`.
- Keeps lifecycle refresh and timed snackbar dismissal in route composables.

- [ ] **Step 1: Add failing root-tag render tests**

Render each existing UI state with no ViewModel and assert these tags:

```kotlin
compose.onNodeWithTag(VisualQaTags.MyPage).assertExists()
compose.onNodeWithTag(VisualQaTags.AddMember).assertExists()
compose.onNodeWithTag(VisualQaTags.EditMember).assertExists()
compose.onNodeWithTag(VisualQaTags.Logout).assertExists()
```

- [ ] **Step 2: Run and confirm private/missing content failures**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*MyPageScreenRenderTest"
```

Expected: compilation fails because the full-screen content APIs are private or absent.

- [ ] **Step 3: Expose exact content signatures**

```kotlin
@Composable
fun MyPageContent(
    uiState: MyPageUiState,
    onLogout: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (String) -> Unit,
)

@Composable
fun AddMemberScreenContent(
    uiState: AddMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    onSubmit: () -> Unit,
)

@Composable
fun EditMemberScreenContent(
    uiState: EditMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
)

@Composable
fun LogoutScreenContent(
    uiState: LogoutUiState,
    onBack: () -> Unit,
    onLogout: () -> Unit,
)
```

Each content root applies its `VisualQaTags` tag. Loading, empty, and error presentations apply `Loading`, `Empty`, and `Error`.

- [ ] **Step 4: Verify My Page render tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*MyPageScreenRenderTest"
```

Expected: all four root-tag tests pass.

- [ ] **Step 5: Commit My Page seams**

```powershell
git add app/src/main app/src/test
git commit -m "refactor: expose account screen render seams"
```

---

### Task 6: Build all-screen snapshot fixtures and references

**Files:**
- Create: `app/src/test/java/com/logflare/android/visual/VisualSnapshotTest.kt`
- Create: `app/src/test/java/com/logflare/android/visual/SnapshotFixtures.kt`
- Create: `app/src/test/java/com/logflare/android/visual/LoginSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/HomeSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/LogsSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/ProjectsSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/ProjectDetailSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/MyPageSnapshots.kt`
- Create: `app/src/test/java/com/logflare/android/visual/SharedStateSnapshots.kt`
- Create: reference PNGs under `app/src/test/snapshots/images/`

**Interfaces:**
- Produces `VisualSnapshotTest.capture(name, darkTheme, content)`.
- Produces deterministic fixture objects with fixed IDs, timestamps, ordering, and text.

- [ ] **Step 1: Create the shared light/dark capture harness**

```kotlin
abstract class VisualSnapshotTest {
    @get:Rule val compose = createComposeRule()

    protected fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LogflareandroidTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.requiredSize(412.dp, 915.dp),
                    color = MaterialTheme.colorScheme.background,
                    content = content,
                )
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().captureRoboImage(
            "${name}_${if (darkTheme) "dark" else "light"}.png"
        )
    }
}
```

- [ ] **Step 2: Add deterministic fixture factories**

`SnapshotFixtures` exposes these exact factories:

```kotlin
fun auth(loading: Boolean = false, loginError: String? = null): AuthUiState
fun projects(empty: Boolean = false, loading: Boolean = false, error: String? = null): ProjectsUiState
fun logs(empty: Boolean = false, loading: Boolean = false, error: String? = null): LogsUiState
fun projectEditor(saved: Boolean = false, loading: Boolean = false, error: String? = null): ProjectCreateUiState
fun projectDetail(empty: Boolean = false, loading: Boolean = false, error: String? = null): ProjectDetailUiState
fun myPage(empty: Boolean = false, loading: Boolean = false, error: String? = null): MyPageUiState
fun addMember(valid: Boolean = false, loading: Boolean = false, error: String? = null): AddMemberUiState
fun editMember(showDelete: Boolean = false, disabled: Boolean = false, loading: Boolean = false): EditMemberUiState
```

Use fixed values: project IDs `101` and `202`, usernames `qa-admin` and `qa-member`, timestamps beginning `2026-01-15T10:30:00Z`, and stable log ordering.

- [ ] **Step 3: Add the complete snapshot matrix**

Every case runs with `darkTheme=false` and `darkTheme=true`:

```text
login_empty, login_validation_error, login_loading
home_populated, home_empty, home_error
logs_populated, logs_empty, logs_filtered_empty, logs_error, logs_loading_more
log_detail_populated, log_detail_empty
projects_populated, projects_empty, projects_error, projects_loading
project_create_initial, project_create_invalid, project_create_saved, project_create_loading
project_settings_populated, project_settings_error, project_settings_loading
project_detail_populated, project_detail_empty, project_detail_error, project_detail_loading
my_page_admin, my_page_no_members, my_page_error, my_page_loading
add_member_empty, add_member_validation_error, add_member_valid, add_member_loading, add_member_error
edit_member_normal, edit_member_validation_error, edit_member_disabled, edit_member_loading, edit_member_delete_dialog
logout_confirmation, logout_loading, logout_error
shared_fullscreen_loading, shared_fullscreen_error, shared_empty
```

- [ ] **Step 4: Verify missing-baseline behavior**

Run:

```powershell
.\gradlew.bat :app:verifyRoborazziDebug
```

Expected: task fails and identifies missing reference images; no reference PNG is created.

- [ ] **Step 5: Record, review, and verify references**

Run:

```powershell
.\gradlew.bat :app:recordRoborazziDebug
.\gradlew.bat :app:verifyRoborazziDebug
```

Expected: verification exits 0. Confirm both `_light.png` and `_dark.png` exist for every matrix row.

- [ ] **Step 6: Prove mismatch reporting**

Temporarily change one fixture title, run `:app:verifyRoborazziDebug`, and confirm the task fails with expected/actual/diff output. Revert the temporary fixture change and rerun verification.

- [ ] **Step 7: Commit the all-screen references**

```powershell
git add app/src/test
git commit -m "test: add all-screen visual baselines"
```

---

### Task 7: Make local server selection and navigation deterministic

**Files:**
- Create: `core/network/src/main/kotlin/com/example/logflare/core/network/host/MutableBaseUrlProvider.kt`
- Modify: `app/src/main/java/com/logflare/android/data/ServerConfigRepository.kt`
- Modify: `app/src/main/java/com/logflare/android/data/DataStoreBaseUrlProvider.kt`
- Modify: `app/src/main/java/com/logflare/android/di/AppNetworkBindings.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/auth/AuthViewModel.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/navigation/Routes.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/main/MainScaffold.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/network_security_config.xml`
- Modify: `app/src/debug/AndroidManifest.xml`
- Modify: `app/src/debug/res/xml/network_security_config.xml`
- Create: `app/src/test/java/com/logflare/android/data/BaseUrlSelectionTest.kt`
- Create: `app/src/test/java/com/logflare/android/ui/navigation/RoutesTest.kt`

**Interfaces:**
- Produces `MutableBaseUrlProvider.setBaseUrl(url: String)`.
- Changes log detail to an argument-free route because detail data is already passed through `PendingLogDetailStore`.

- [ ] **Step 1: Write failing synchronous URL and route tests**

```kotlin
@Test fun selectedUrlIsVisibleBeforeSetReturns() = runTest {
    provider.setBaseUrl("http://10.0.2.2:8000")
    assertEquals("http://10.0.2.2:8000/", provider.getBaseUrl())
}

@Test fun logDetailRouteContainsNoUnboundArgument() {
    assertEquals("log/detail", Route.LogDetail.path)
    assertFalse(Route.LogDetail.path.contains("{"))
}
```

- [ ] **Step 2: Run and confirm failures**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BaseUrlSelectionTest" --tests "*RoutesTest"
```

Expected: tests fail because URL cache updates asynchronously and route is `log/{logId}`.

- [ ] **Step 3: Implement a mutable provider**

Create:

```kotlin
interface MutableBaseUrlProvider : BaseUrlProvider {
    suspend fun setBaseUrl(url: String)
}
```

Make `ServerConfigRepository.normalize` internal and make `DataStoreBaseUrlProvider` implement `MutableBaseUrlProvider`:

```kotlin
override suspend fun setBaseUrl(url: String) {
    val normalized = serverConfigRepository.normalize(url)
    cached.set(normalized)
    serverConfigRepository.setNormalizedServerUrl(normalized)
}
```

Bind the implementation to both interfaces. Inject `MutableBaseUrlProvider` into `AuthViewModel` and call `setBaseUrl` before `performLoginInternal`.

- [ ] **Step 4: Remove the unbound log route argument**

Set `Route.LogDetail` to `Route("log/detail")`, remove `createRoute`, and keep both call sites navigating to `Route.LogDetail.path`.

- [ ] **Step 5: Restrict network policy by build type**

Add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Set main/release `android:usesCleartextTraffic="false"`. Keep cleartext enabled only in `src/debug` and restrict the debug network-security config to:

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

- [ ] **Step 6: Verify tests and merged manifests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:processDebugMainManifest :app:processReleaseMainManifest
```

Expected: tests pass; debug manifest allows local cleartext; release manifest contains `usesCleartextTraffic="false"` and `INTERNET`.

- [ ] **Step 7: Commit deterministic app wiring**

```powershell
git add core/network app/src/main app/src/debug app/src/test
git commit -m "fix: make local E2E routing deterministic"
```

---

### Task 8: Build the mock server and device image comparator CLI

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Create: `visual-qa/tools/build.gradle.kts`
- Create: `visual-qa/tools/src/main/kotlin/com/logflare/qa/QaToolMain.kt`
- Create: `visual-qa/tools/src/main/kotlin/com/logflare/qa/server/FixtureState.kt`
- Create: `visual-qa/tools/src/main/kotlin/com/logflare/qa/server/MockServer.kt`
- Create: `visual-qa/tools/src/main/kotlin/com/logflare/qa/image/DeviceImageComparator.kt`
- Create: `visual-qa/tools/src/test/kotlin/com/logflare/qa/server/MockServerContractTest.kt`
- Create: `visual-qa/tools/src/test/kotlin/com/logflare/qa/image/DeviceImageComparatorTest.kt`

**Interfaces:**
- Produces CLI commands `server`, `compare`, and `record-device`.
- Mock server exposes `GET /__qa/health` and `POST /__qa/reset`.
- Comparator uses `channelTolerance=2` and `maxChangedRatio=0.0001`.

- [ ] **Step 1: Add failing server and comparator tests**

Server contract tests call every method/path declared by `LogflareApi` and decode responses with the model serializers. Comparator tests assert:

```kotlin
assertEquals(CompareResult.Match, comparator.compare(expected, identical, diff))
assertIs<CompareResult.DimensionMismatch>(comparator.compare(expected, wrongSize, diff))
assertIs<CompareResult.Changed>(comparator.compare(expected, visiblyChanged, diff))
assertTrue(diff.exists())
```

- [ ] **Step 2: Run and confirm the module is absent**

Run:

```powershell
.\gradlew.bat :visual-qa:tools:test
```

Expected: Gradle fails because the module does not exist.

- [ ] **Step 3: Add the JVM application module**

Add Kotlin JVM and serialization plugin aliases, include `:visual-qa:tools`, and configure:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.logflare.qa.QaToolMainKt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
```

- [ ] **Step 4: Implement deterministic server state**

`FixtureState.reset()` seeds:

```kotlin
@Serializable
data class QaUser(val idx: Int, val username: String, val permission: Int)

@Serializable
data class QaLogFile(
    val id: Int,
    val project_id: Int,
    val file_path: String,
    val file_name: String,
)

@Serializable
data class QaProject(val id: Int, val name: String, val logfiles: List<QaLogFile>)

val users = linkedMapOf(
    1 to QaUser(1, "qa-admin", 0),
    2 to QaUser(2, "qa-member", 2),
)
val projects = linkedMapOf(
    101 to QaProject(101, "Payments", listOf(QaLogFile(1001, 101, "/var/log/payments.log", "payments.log"))),
    202 to QaProject(202, "Checkout", listOf(QaLogFile(2001, 202, "/var/log/checkout.log", "checkout.log"))),
)
```

The server validates `qa-admin` / `qa-password`, returns token `qa-token-fixed`, mutates in-memory users/projects for POST/PATCH/DELETE, returns fixed error logs and raw log strings, and returns `success=false` for `/fcm/data` so Firebase initialization is skipped.

- [ ] **Step 5: Implement all endpoint routes and reset/health**

Dispatch on HTTP method and URI path for this complete contract:

```text
GET /user/
POST /user/
POST /user/auth
GET /user/me
GET /user/name
PATCH /user/{id}
DELETE /user/{id}
GET /project/
POST /project/
DELETE /project/{id}
PATCH /project/{id}
GET /project/{id}/perm
POST /project/perm/batch/reset
GET /log/error
POST /log/error
GET /log/{projectId}/{logFileId}
GET /fcm/data
POST /fcm/token
```

`LogflareApi` declares `/fcm/data` twice with different method names, so one route covers both declarations. Return UTF-8 JSON with `Content-Type: application/json`. Contract tests parse every response with `Json.parseToJsonElement` and assert `success`, `message`, and the expected `data` shape. `POST /__qa/reset` calls `FixtureState.reset()`; `GET /__qa/health` returns:

```json
{"status":"ok"}
```

- [ ] **Step 6: Implement strict PNG comparison**

`DeviceImageComparator.compare`:

1. Loads expected/actual using `ImageIO.read`.
2. Fails immediately when width or height differs.
3. Marks a pixel changed when any RGBA channel delta is greater than `2`.
4. Fails when changed pixels divided by total pixels is greater than `0.0001`.
5. Writes unchanged pixels at 20% opacity and changed pixels as opaque magenta into the diff PNG.
6. Never changes expected images during `compare`.

- [ ] **Step 7: Verify CLI unit tests and distribution**

Run:

```powershell
.\gradlew.bat :visual-qa:tools:test :visual-qa:tools:installDist
```

Expected: all tests pass and `visual-qa/tools/build/install/tools/bin/tools.bat` exists.

- [ ] **Step 8: Commit the local QA tools**

```powershell
git add settings.gradle.kts gradle/libs.versions.toml visual-qa/tools
git commit -m "test: add deterministic visual QA tools"
```

---

### Task 9: Add Maestro mock journeys and stable app semantics

**Files:**
- Modify: `app/src/main/java/com/logflare/android/MainActivity.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/auth/LoginScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCreateScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectSettingsScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCommonUiModels.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/projectdetail/ProjectDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/MyPageScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/AddMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/EditMemberScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/LogoutScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/common/LogUiComponents.kt`
- Create: `.maestro/config.yaml`
- Create: `.maestro/flows/_login.yaml`
- Create: `.maestro/flows/login-home.yaml`
- Create: `.maestro/flows/logs-detail.yaml`
- Create: `.maestro/flows/projects.yaml`
- Create: `.maestro/flows/mypage-members.yaml`
- Create: `.maestro/flows/logout.yaml`
- Create: `scripts/visual-qa-common.ps1`
- Create: `scripts/visual-qa-maestro-mock.ps1`
- Create: `visual-qa/device-baselines/.gitkeep`

**Interfaces:**
- Maestro selectors use Compose test tags exposed as resource IDs.
- Mock runner supports `-Mode Verify|Record` and `-Serial <adb serial>`.

- [ ] **Step 1: Add root semantics and stable interaction tags**

Apply `Modifier.testTag(VisualQaTags.AppRoot).semantics { testTagsAsResourceId = true }` at the app root. Tag custom project cards, log cards, member rows, filter controls, loading/empty/error states, and primary actions with deterministic IDs containing stable fixture IDs, for example:

```text
project_card_101
log_card_5001
member_row_qa-member
open_project_settings
create_project
add_member
logout
```

- [ ] **Step 2: Create reusable login flow**

`.maestro/flows/_login.yaml`:

```yaml
appId: com.logflare.android
---
- launchApp:
    clearState: true
- tapOn: "Server URL"
- inputText: ${QA_BASE_URL}
- tapOn: "Username"
- inputText: ${QA_USERNAME}
- tapOn: "Password"
- inputText: ${QA_PASSWORD}
- tapOn: "Sign In"
- assertVisible:
    id: "home_screen"
```

- [ ] **Step 3: Add five deterministic journeys**

Each top-level flow runs `_login.yaml`, asserts stable IDs, and captures named checkpoints with:

```yaml
- takeScreenshot:
    path: ${QA_OUTPUT_DIR}/logs_detail_${QA_THEME}
    cropOn:
      id: "app_root"
```

Flows cover:

```text
login-home: run _login -> assert home_screen -> capture home
logs-detail: run _login -> tap bottom nav Logs -> assert logs_list -> tap log_card_5001 -> assert log_detail -> capture detail
projects: run _login -> tap bottom nav Projects -> assert projects_list -> capture list -> tap project_card_101 -> assert project_detail -> capture detail -> tap open_project_settings -> assert project_settings -> capture settings
mypage-members: run _login -> tap bottom nav MyPage -> assert my_page -> capture overview -> tap add_member -> assert add_member -> capture add form -> navigate back -> tap member_row_qa-member -> assert edit_member -> capture edit form
logout: run _login -> tap bottom nav MyPage -> tap logout -> assert logout_confirmation -> capture confirmation -> tap confirm logout -> assert login_screen
```

- [ ] **Step 4: Implement PowerShell prerequisite and cleanup helpers**

`visual-qa-common.ps1` provides:

```powershell
Assert-Command adb
Assert-Command maestro
Assert-Java17
Resolve-EmulatorSerial
Assert-Pixel7Api35Profile
Set-AnimationScales
Restore-AnimationScales
Set-UiMode
Wait-HttpHealth
```

The profile assertion checks API `35`, `wm size` `1080x2400`, and density `420`. Always restore animation scales in `finally`.

- [ ] **Step 5: Implement mock verify/record orchestration**

For both light and dark:

1. Build/install app and `:visual-qa:tools:installDist`.
2. Start `tools.bat server --port 8000` and retain its PID.
3. Wait for `http://localhost:8000/__qa/health`.
4. Reset server state before each flow.
5. Clear app data, deny `POST_NOTIFICATIONS`, set UI mode, and disable animations.
6. Run Maestro with `QA_BASE_URL=http://10.0.2.2:8000`, fixed credentials, selected serial, and output directory.
7. In `Verify`, call `tools.bat compare` for every checkpoint.
8. In `Record`, call `tools.bat record-device` only after all flows pass.
9. Stop the server and restore device settings in `finally`.

- [ ] **Step 6: Run mock flows in record and verify modes**

Run:

```powershell
.\scripts\visual-qa-maestro-mock.ps1 -Mode Record -Serial emulator-5554
.\scripts\visual-qa-maestro-mock.ps1 -Mode Verify -Serial emulator-5554
```

Expected: both themes complete; references are created only by Record; Verify exits 0 without modifying tracked files.

- [ ] **Step 7: Prove device mismatch reporting**

Temporarily alter one expected device image, run Verify, and confirm a magenta diff appears under `visual-qa/device-diffs/`. Restore the expected image and rerun Verify.

- [ ] **Step 8: Commit Maestro flows and device references**

```powershell
git add app/src/main .maestro scripts visual-qa/device-baselines
git commit -m "test: add local Maestro visual journeys"
```

---

### Task 10: Add local entry points, development-server mode, and release guards

**Files:**
- Create: `scripts/visual-qa-snapshots.ps1`
- Create: `scripts/visual-qa-maestro-dev.ps1`
- Create: `scripts/verify-release-isolation.ps1`
- Create: `docs/visual-qa.md`
- Modify: `.gitignore`

**Interfaces:**
- Snapshot runner supports `-Mode Verify|Record`.
- Development runner requires `-BaseUrl`, `-Username`, `-Password`, and optional `-Serial`.
- Release guard exits nonzero on cleartext or E2E-only artifacts.

- [ ] **Step 1: Add snapshot wrapper**

Map:

```powershell
Record -> .\gradlew.bat :app:recordRoborazziDebug
Verify -> .\gradlew.bat :app:verifyRoborazziDebug
```

Reject any other mode and print Roborazzi report paths on failure.

- [ ] **Step 2: Add development-server runner**

The script validates non-empty parameters, builds/installs debug, selects one emulator, runs the login-to-Home smoke flow plus requested journeys, and writes captures to `visual-qa/dev-captures/`. It must not invoke `tools.bat compare` or echo the password.

- [ ] **Step 3: Add release isolation guard**

Build release and inspect the merged manifest and APK contents. Fail when:

```text
usesCleartextTraffic is not false
an activity/service/receiver contains "qa" or "maestro"
APK strings contain "qa-password" or "/__qa/"
```

The presence of `android.permission.INTERNET` is allowed.

- [ ] **Step 4: Document exact local commands**

`docs/visual-qa.md` documents:

```powershell
.\scripts\visual-qa-snapshots.ps1 -Mode Record
.\scripts\visual-qa-snapshots.ps1 -Mode Verify
.\scripts\visual-qa-maestro-mock.ps1 -Mode Record -Serial emulator-5554
.\scripts\visual-qa-maestro-mock.ps1 -Mode Verify -Serial emulator-5554
.\scripts\visual-qa-maestro-dev.ps1 -BaseUrl "https://dev.example.com/" -Username "user" -Password $env:LOGFLARE_QA_PASSWORD -Serial emulator-5554
```

Document Maestro native Windows installation, Java 17, API 35 Pixel 7 AVD creation, `10.0.2.2`, firewall/port `8000`, artifact locations, baseline review policy, and recovery if animation scales were not restored.

- [ ] **Step 5: Commit local runners and documentation**

Because `docs/*` is ignored, force-add only this intended document:

```powershell
git add scripts .gitignore
git add -f docs/visual-qa.md
git commit -m "docs: add local visual QA workflow"
```

- [ ] **Step 6: Run final verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :visual-qa:tools:test
.\scripts\visual-qa-snapshots.ps1 -Mode Verify
.\scripts\visual-qa-maestro-mock.ps1 -Mode Verify -Serial emulator-5554
.\scripts\verify-release-isolation.ps1
git status --short
```

Expected: all commands exit 0; device and JVM diffs are empty; `git status --short` prints nothing.
