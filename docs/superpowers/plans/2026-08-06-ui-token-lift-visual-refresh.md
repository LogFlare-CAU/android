# UI Token Lift + Visual Refresh Implementation Plan

> **Status:** Done (2026-08-06). Landed on `main` through `a77aa1f`. CI: https://github.com/LogFlare-CAU/android/actions/runs/31100751538

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lift LogFlare Android UI onto a Primitive → Semantic → Role token stack, polish chrome (status bar / TopBar / GNB) and migrate primary screens to Calm Ops consistency with Title Case labels, shared state views, and refreshed Roborazzi baselines.

**Architecture:** Extend `core/designsystem` with `AppRoles` + title typography roles; wire them through `AppTheme` CompositionLocals. Update chrome components first, then migrate feature screens off magic `dp` / `MaterialTheme` styling. Keep Material3 bridge as a compatibility adapter only. Presentation-layer cleanup only — no API/auth/repo/nav-graph redesign.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (bridge), Robolectric + Roborazzi, Gradle Wrapper (`./gradlew` / `.\gradlew.bat`).

**Spec:** `docs/superpowers/specs/2026-08-06-ui-token-lift-visual-refresh-design.md`

## Global Constraints

- Tone: Calm Ops (polish current green + Pretendard identity; no Signal Desk density, no Soft Product consumer look).
- Feature/app UI uses **Semantic + Role only** — no `Color(0x…)`, no primitive Neutral/Green imports, no magic `16.dp` / `RoundedCornerShape(12.dp)` in migrated screens.
- Titles/labels: **Title Case** (`Logs`, `Projects`, `My Page`, …) — drop ALL CAPS.
- Prefer `AppTheme.*` over `MaterialTheme.colorScheme` / `MaterialTheme.typography` in migrated code.
- Logic scope: presentation only (UI state shaping, shared Loading/Empty/Error, remove dual UI paths). No domain/data/API/nav rewrite.
- Keep Visual QA `testTag`s (`VisualQaTags.*`) and `*ScreenContent` snapshot entry points.
- Builds/tests: in-repo Gradle Wrapper; before CI-bound verify, prefer Linux-aligned Roborazzi goldens (record or adopt CI actuals).
- Agent Gate: if MCP is up, `claim_scope` before edits and `check_build_safety` before Gradle; if unreachable, proceed and note it.

---

## File map

| File | Responsibility |
|------|----------------|
| `core/designsystem/.../Dimens.kt` | Add `AppRoles` + `LocalAppRoles` |
| `core/designsystem/.../Typography.kt` | Add `titleAppBar`, `titleSection` |
| `core/designsystem/.../Theme.kt` | Provide roles + new type styles |
| `core/designsystem/.../theme/AppTheme.kt` | Expose `AppTheme.roles` |
| `core/designsystem/.../navigation/TopAppBar.kt` | Consume `roles.chrome` |
| `core/designsystem/.../navigation/Gnb.kt` | Token tints; selected/unselected |
| `core/designsystem/.../components/feedback/` or new `state/` | Promote Loading/Empty/Error |
| `app/.../ui/common/ListStateViews.kt` | Thin wrappers or delete after promote |
| `app/.../feature/main/MainScaffold.kt` | Title Case copy + GNB labels |
| `app/.../feature/main/MainScaffoldTopBarPolicy.kt` | Extra top padding from roles |
| Feature screens (Login, Home, Logs, Projects, MyPage, detail/create/settings) | Token migration |
| `app/.../visual/ThemeContractTest.kt` | Role contract tests |
| `app/src/test/snapshots/images/*.png` | Refresh after intentional visual change |

---

### Task 1: AppRoles + title typography + Theme wiring

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Dimens.kt`
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Typography.kt`
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Theme.kt`
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/theme/AppTheme.kt`
- Test: `app/src/test/java/com/logflare/android/visual/ThemeContractTest.kt`

**Interfaces:**
- Consumes: existing `AppSpacing`, `AppRadius`, `AppDimens`, `AppTypography`, `CompositionLocalProvider` in `Theme.kt`
- Produces:
  - `data class AppChromeRoles(val topBarHeight: Dp, val topBarHorizontalPadding: Dp, val gnbSelectedUsesPrimary: Boolean)`
  - `data class AppLayoutRoles(val screenPadding: Dp, val sectionGap: Dp, val contentGap: Dp, val statePadding: Dp)`
  - `data class AppSurfaceRoles` — document that card/field resolve via `AppTheme.colors` + `AppTheme.radius` (no duplicate colors)
  - `data class AppRoles(val chrome: AppChromeRoles, val layout: AppLayoutRoles)`
  - `LocalAppRoles`, `AppTheme.roles`
  - `AppTypography.titleAppBar`, `AppTypography.titleSection`

- [x] **Step 1: Write the failing contract tests**

Append to `ThemeContractTest.kt`:

```kotlin
@Test
fun rolesExposePositiveLayoutTokens() {
    var screenPadding = 0.dp
    var sectionGap = 0.dp
    var topBarHeight = 0.dp
    compose.setContent {
        LogflareandroidTheme(darkTheme = false) {
            screenPadding = AppTheme.roles.layout.screenPadding
            sectionGap = AppTheme.roles.layout.sectionGap
            topBarHeight = AppTheme.roles.chrome.topBarHeight
        }
    }
    assertTrue(screenPadding > 0.dp)
    assertTrue(sectionGap > 0.dp)
    assertTrue(topBarHeight >= 56.dp)
}

@Test
fun titleTypographyRolesExist() {
    var appBarSize = 0f
    var sectionSize = 0f
    compose.setContent {
        LogflareandroidTheme(darkTheme = false) {
            appBarSize = AppTheme.typography.titleAppBar.fontSize.value
            sectionSize = AppTheme.typography.titleSection.fontSize.value
        }
    }
    assertTrue(appBarSize >= 14f)
    assertTrue(sectionSize >= 16f)
}
```

Add imports: `androidx.compose.ui.unit.dp`, `org.junit.Assert.assertTrue` (already present).

- [x] **Step 2: Run tests to verify they fail**

Run (Windows):

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.logflare.android.visual.ThemeContractTest.rolesExposePositiveLayoutTokens" --tests "com.logflare.android.visual.ThemeContractTest.titleTypographyRolesExist"
```

Expected: FAIL (unresolved `AppTheme.roles` / `titleAppBar` / `titleSection`).

- [x] **Step 3: Add `AppRoles` to Dimens.kt**

Append to `Dimens.kt`:

```kotlin
@Immutable
data class AppChromeRoles(
    val topBarHeight: Dp = 56.dp,
    val topBarHorizontalPadding: Dp = 16.dp,
)

@Immutable
data class AppLayoutRoles(
    val screenPadding: Dp = 16.dp,
    val sectionGap: Dp = 32.dp,
    val contentGap: Dp = 12.dp,
    val statePadding: Dp = 24.dp,
)

@Immutable
data class AppRoles(
    val chrome: AppChromeRoles = AppChromeRoles(),
    val layout: AppLayoutRoles = AppLayoutRoles(),
)

val LocalAppRoles = staticCompositionLocalOf { AppRoles() }
```

Keep existing `AppSpacing` / `AppRadius` / `AppDimens` unchanged. Role defaults may equal spacing values; screens must read **roles**, not raw `16.dp`.

- [x] **Step 4: Extend `AppTypography`**

In `Typography.kt`, add fields to `AppTypography`:

```kotlin
val titleAppBar: TextStyle,
val titleSection: TextStyle,
```

Update the `staticCompositionLocalOf` default with `TextStyle.Default` for both.

- [x] **Step 5: Wire Theme.kt**

In `AppTheme` composable, when building `appTypography`, add:

```kotlin
titleAppBar = TextStyle(
    fontFamily = Pretendard,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 24.sp,
),
titleSection = TextStyle(
    fontFamily = Pretendard,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 28.sp,
),
```

Provide roles:

```kotlin
val appRoles = AppRoles()
CompositionLocalProvider(
    LocalAppColors provides appColors,
    LocalAppTypography provides appTypography,
    LocalAppSpacing provides appSpacing,
    LocalAppRadius provides appRadius,
    LocalAppDimens provides appDimens,
    LocalAppRoles provides appRoles,
    content = content,
)
```

- [x] **Step 6: Expose `AppTheme.roles`**

In both `core/designsystem/.../Theme.kt` `object AppTheme` and `core/designsystem/.../theme/AppTheme.kt` wrapper:

```kotlin
val roles: AppRoles
    @Composable
    @ReadOnlyComposable
    get() = LocalAppRoles.current
```

Import `AppRoles` / `LocalAppRoles` in the wrapper file.

- [x] **Step 7: Re-run contract tests**

Same Gradle command as Step 2. Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Dimens.kt \
  core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Typography.kt \
  core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/Theme.kt \
  core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/theme/AppTheme.kt \
  app/src/test/java/com/logflare/android/visual/ThemeContractTest.kt
git commit -m "feat: add AppRoles and title typography tokens"
```

---

### Task 2: Chrome — TopAppBar + GNB + MainScaffold Title Case

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/navigation/TopAppBar.kt`
- Modify: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/navigation/Gnb.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/main/MainScaffold.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/main/MainScaffoldTopBarPolicy.kt`
- Test: `app/src/test/java/com/logflare/android/visual/VisualQaSemanticsTest.kt` (or add assertions if titles are tagged; otherwise rely on string update + unit compile)

**Interfaces:**
- Consumes: `AppTheme.roles.chrome`, `AppTheme.typography.titleAppBar`, `AppTheme.colors.surface|onSurface|muted|primary`
- Produces: Title Case route titles; GNB labels `Home`, `Logs`, `Projects`, `My Page`; icon tint from tokens

**Title Case map (exact strings):**

| Route | Title |
|-------|--------|
| Home | (wordmark — no text) |
| Logs | `Logs` |
| Projects | `Projects` |
| MyPage | `My Page` |
| ProjectCreate | `Create Project` |
| ProjectDetail | `projectDetailTitle ?: "Project Detail"` |
| ProjectSettings | `Project Settings` |
| LogDetail | `Log Details` |
| GNB MyPage label | `My Page` |

- [x] **Step 1: Update TopAppBar to use roles + titleAppBar**

In `LogFlareTopAppBar`, replace hardcoded `56.dp` / `AppTheme.spacing.s4` and title style:

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .height(AppTheme.roles.chrome.topBarHeight)
        .background(AppTheme.colors.surface)
        .statusBarsPadding()
        .padding(horizontal = AppTheme.roles.chrome.topBarHorizontalPadding),
    contentAlignment = Alignment.Center,
) {
    // ... back / close unchanged ...
    when (titleType) {
        TopAppBarTitleType.Default -> LogFlareWordmark(modifier = Modifier.align(Alignment.Center))
        TopAppBarTitleType.Title -> Text(
            text = titleText.orEmpty(),
            style = AppTheme.typography.titleAppBar,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
```

Keep wordmark tint = `AppTheme.colors.onSurface`.

- [x] **Step 2: Update GNB item colors**

In `LogFlareGnbItem`:

```kotlin
val selectedColor = AppTheme.colors.primary.default
val unselectedColor = AppTheme.colors.muted
// ...
icon = {
    androidx.compose.material3.Icon(
        painter = painterResource(id = iconRes),
        contentDescription = label,
        tint = when {
            !enabled -> disabledColor
            selected -> selectedColor
            else -> unselectedColor
        },
    )
},
colors = NavigationBarItemDefaults.colors(
    indicatorColor = Color.Transparent,
    selectedIconColor = selectedColor,
    selectedTextColor = selectedColor,
    unselectedIconColor = unselectedColor,
    unselectedTextColor = unselectedColor,
    disabledIconColor = disabledColor,
    disabledTextColor = disabledColor,
)
```

Remove `Color.Unspecified` icon tint.

- [x] **Step 3: Title Case in MainScaffold**

Replace title strings and GNB MyPage label per the table above. Example:

```kotlin
titleText = when (currentRoute) {
    Route.Logs.path -> "Logs"
    Route.Projects.path -> "Projects"
    Route.MyPage.path -> "My Page"
    Route.ProjectCreate.path -> "Create Project"
    Route.ProjectDetail.path -> projectDetailTitle ?: "Project Detail"
    Route.ProjectSettings.path -> "Project Settings"
    Route.LogDetail.path -> "Log Details"
    else -> null
},
// ...
GnbItem(..., label = "My Page", ...)
```

- [x] **Step 4: Role-based extra top padding**

In `MainScaffoldTopBarPolicy.kt`:

```kotlin
fun mainNavHostExtraTopPaddingDp(route: String?): Int =
    if (shouldHideScaffoldTopBar(route)) 0 else 16
```

Change call site in `MainScaffold` to use roles instead of the Int helper where possible:

```kotlin
val extraTop = if (shouldHideScaffoldTopBar(currentRoute)) {
    0.dp
} else {
    AppTheme.roles.layout.contentGap // or keep 16 via roles.layout.screenPadding if that matches current UX
}
```

Prefer deleting `mainNavHostExtraTopPaddingDp` if all call sites migrate; if tests reference it, update them to assert the composable path or keep a thin wrapper that documents the role default (`AppLayoutRoles.contentGap` / `screenPadding`). **Do not leave a magic `16` without tying it to `AppRoles`.**

Recommended: change helper to take no magic literal by reading a constant from roles defaults:

```kotlin
// Non-composable helper for tests: mirrors AppLayoutRoles defaults
fun mainNavHostExtraTopPaddingDp(route: String?): Int =
    if (shouldHideScaffoldTopBar(route)) 0 else AppLayoutRoles().contentGap.value.toInt()
```

Import `AppLayoutRoles` from designsystem.

- [x] **Step 5: Compile / run focused tests**

```powershell
.\gradlew.bat :app:compileDebugKotlin :core:designsystem:compileDebugKotlin :app:testDebugUnitTest --tests "com.logflare.android.visual.ThemeContractTest"
```

Expected: BUILD SUCCESSFUL / tests PASS.

- [x] **Step 6: Commit**

```bash
git add core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/navigation/TopAppBar.kt \
  core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/navigation/Gnb.kt \
  app/src/main/java/com/logflare/android/feature/main/MainScaffold.kt \
  app/src/main/java/com/logflare/android/feature/main/MainScaffoldTopBarPolicy.kt
git commit -m "feat: role-driven chrome and Title Case nav labels"
```

---

### Task 3: Promote Loading / Empty / Error to designsystem (Role padding)

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/state/StateViews.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/common/ListStateViews.kt`
- Modify any feature imports if signatures change
- Test: existing snapshot / semantics tests that use `VisualQaTags.Loading|Empty|Error` — tags must remain on app wrappers if designsystem cannot depend on app tags

**Interfaces:**
- Consumes: `AppTheme.roles.layout.statePadding`, `AppTheme.colors`, `AppTheme.typography`, `LogFlareButton`
- Produces:
  - `LogFlareLoadingState(modifier)`
  - `LogFlareErrorState(message, modifier, onRetry)`
  - `LogFlareEmptyState(title, subtitle?, modifier, actionLabel?, onAction?)`
- App wrappers keep `testTag(VisualQaTags.*)` so Visual QA unchanged

- [x] **Step 1: Implement designsystem state views**

Create `StateViews.kt`:

```kotlin
package com.example.logflare.core.designsystem.components.state

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton

@Composable
fun LogFlareLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppTheme.colors.primary.default)
    }
}

@Composable
fun LogFlareErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.roles.layout.statePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap),
        ) {
            Text(
                text = message,
                color = AppTheme.colors.red.default,
                style = AppTheme.typography.bodyMdMedium,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                LogFlareButton(
                    text = "Retry",
                    onClick = onRetry,
                    type = ButtonType.Outline,
                    variant = ButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
fun LogFlareEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.roles.layout.statePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.bodyLgBold,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodyMdMedium,
                    color = AppTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier = Modifier.height(AppTheme.spacing.s2))
                LogFlareButton(
                    text = actionLabel,
                    onClick = onAction,
                    type = ButtonType.Filled,
                    variant = ButtonVariant.Primary,
                    modifier = actionModifier,
                )
            }
        }
    }
}
```

- [x] **Step 2: Thin app wrappers**

Rewrite `ListStateViews.kt` to delegate:

```kotlin
@Composable
fun ListLoadingState(modifier: Modifier = Modifier) {
    LogFlareLoadingState(modifier = modifier.testTag(VisualQaTags.Loading))
}

@Composable
fun ListErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    LogFlareErrorState(
        message = message,
        onRetry = onRetry,
        modifier = modifier.testTag(VisualQaTags.Error),
    )
}

@Composable
fun ListEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onAction: (() -> Unit)? = null,
) {
    LogFlareEmptyState(
        title = title,
        subtitle = subtitle,
        onAction = onAction,
        actionLabel = actionLabel,
        actionModifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier,
        modifier = modifier.testTag(VisualQaTags.Empty),
    )
}
```

- [x] **Step 3: Compile + run SharedState / MyPage / Projects unit snapshot tests if cheap**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.logflare.android.visual.SharedStateSnapshots" --tests "com.logflare.android.visual.ThemeContractTest"
```

Expected: PASS, or intentional pixel FAIL if visuals changed — if FAIL only on pixels, defer golden refresh to Task 7 (do not block compile).

- [x] **Step 4: Commit**

```bash
git add core/designsystem/src/main/kotlin/com/example/logflare/core/designsystem/components/state/StateViews.kt \
  app/src/main/java/com/logflare/android/ui/common/ListStateViews.kt
git commit -m "refactor: promote list state views into designsystem roles"
```

---

### Task 4: Login screen token migration

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/auth/LoginScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/ui/theme/TextFieldColors.kt` (only if still using non-semantic colors)
- Test: `app/src/test/java/com/logflare/android/visual/LoginSnapshots.kt`

**Interfaces:**
- Consumes: `AppTheme.roles.layout.screenPadding`, `AppTheme.colors`, `AppTheme.typography`, logo drawable
- Produces: Login content with no magic `dp` / Material colorScheme for layout chrome

- [x] **Step 1: Replace hardcoded paddings/spacers in `LoginScreenContent`**

Use `AppTheme.roles.layout.screenPadding` for horizontal padding, `AppTheme.spacing.s*` or `roles.layout.contentGap` / `sectionGap` for vertical rhythm. Keep logo size via `AppTheme.dimens` or a single role if already sensible — if logo uses `size(N.dp)`, introduce `AppRoles` field only if reused; otherwise map to nearest `AppSpacing` multiple (document in commit).

Replace any `MaterialTheme.colorScheme.*` text/button colors with `AppTheme.colors.*`.

- [x] **Step 2: Run Login snapshots (verify mode)**

```powershell
.\gradlew.bat :app:verifyRoborazziDebug --tests "com.logflare.android.visual.LoginSnapshots"
```

Expected: may FAIL pixels — note failures; do not record yet unless only Login changed and you will commit goldens in Task 7.

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/logflare/android/feature/auth/LoginScreen.kt
git commit -m "refactor: migrate LoginScreen to AppTheme roles"
```

---

### Task 5: Home screen token + section rhythm migration

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/home/HomeScreen.kt`
- Test: `app/src/test/java/com/logflare/android/visual/HomeSnapshots.kt`

**Interfaces:**
- Consumes: `AppTheme.roles.layout.*`, `AppTheme.radius.large`, `AppTheme.colors.surfaceVariant|onSurface|muted|red`, `AppTheme.typography.titleSection|body*`, `List*State` wrappers
- Produces: Home without `MaterialTheme.typography/colorScheme` and without raw `16.dp` / `32.dp` / `RoundedCornerShape(12.dp)`

- [x] **Step 1: Migrate paddings**

Replace patterns like `.padding(top = 32.dp, start = 16.dp, end = 16.dp)` with:

```kotlin
.padding(
    top = AppTheme.roles.layout.sectionGap,
    start = AppTheme.roles.layout.screenPadding,
    end = AppTheme.roles.layout.screenPadding,
)
```

- [x] **Step 2: Migrate cards / text styles**

```kotlin
colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceVariant),
shape = AppTheme.radius.large,
// ...
Text(..., style = AppTheme.typography.titleSection, color = AppTheme.colors.onSurface)
```

Section headers use `titleSection`. Body uses `bodyMd*` / `bodySm*`. Errors use `AppTheme.colors.red.default` (not only Material error).

- [x] **Step 3: Prefer shared list state components** for loading/empty/error branches already inline if duplicated.

- [x] **Step 4: Compile Home-related tests**

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.logflare.android.visual.HomeSnapshots" --tests "com.logflare.android.visual.ThemeContractTest"
```

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/logflare/android/feature/home/HomeScreen.kt
git commit -m "refactor: migrate HomeScreen to role tokens"
```

---

### Task 6: Logs, Projects, MyPage + nested project/log screens

**Files:**
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/log/LogDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectListScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectCreateScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectSettingsScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/project/ProjectEditorForm.kt` / `EditorSection.kt` if they hardcode spacing
- Modify: `app/src/main/java/com/logflare/android/feature/projectdetail/ProjectDetailScreen.kt`
- Modify: `app/src/main/java/com/logflare/android/feature/mypage/MyPageScreen.kt` (+ Add/Edit/Logout screens if they use magic `dp` / MaterialTheme)
- Test: corresponding `*Snapshots.kt`

**Interfaces:**
- Consumes: same Role/Semantic tokens as Task 5
- Produces: primary tabs + nested screens on tokens; still presentation-only

- [x] **Step 1: Sweep each file for forbidden patterns**

Search and replace in the files above:

```powershell
rg "MaterialTheme\.(colorScheme|typography)|RoundedCornerShape\(|padding\([^\)]*\d+\.dp" app/src/main/java/com/logflare/android/feature
```

For each hit in scope: switch to `AppTheme.roles` / `AppTheme.spacing` / `AppTheme.radius` / `AppTheme.colors` / `AppTheme.typography`.

- [x] **Step 2: Ensure list screens use `ListLoadingState` / `ListErrorState` / `ListEmptyState`** consistently (no one-off centered Text duplicates for the same states).

- [x] **Step 3: Compile + focused unit tests**

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.logflare.android.visual.ThemeContractTest" --tests "com.logflare.android.visual.VisualQaSemanticsTest"
```

Expected: PASS (semantics). Snapshot pixel diffs OK until Task 7.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/logflare/android/feature
git commit -m "refactor: migrate feature screens to AppTheme roles"
```

---

### Task 7: Full verify + refresh Roborazzi baselines + CI

**Files:**
- Modify: `app/src/test/snapshots/images/*.png` (as needed)
- Possibly no code changes

**Interfaces:**
- Consumes: all prior UI changes
- Produces: green `:app:verifyRoborazziDebug` on Linux CI

- [x] **Step 1: Local verify**

```powershell
.\gradlew.bat :app:verifyRoborazziDebug
```

Expected: FAIL on changed screens (list failures).

- [x] **Step 2: Record or adopt Linux actuals**

Preferred for CI parity:

1. Push a commit with code-only changes if baselines still old, let CI fail, download `roborazzi-comparisons` artifact `*_actual.png`, copy over goldens (strip `_actual`), commit.
2. Or locally: `.\gradlew.bat :app:recordRoborazziDebug` then push — if CI still fails due to OS raster differences, fall back to (1).

Use existing helper if preferred: `scripts/visual-qa-snapshots.ps1 -Mode Record`.

- [x] **Step 3: Commit baselines**

```bash
git add app/src/test/snapshots/images
git commit -m "test: refresh Roborazzi baselines after token lift"
```

- [x] **Step 4: Push and watch CI**

```bash
git push origin HEAD
gh run watch --exit-status
```

Expected: Android CI success (Build Debug, Unit tests, Verify JVM snapshots).

- [x] **Step 5: Spec / plan checkbox sync**

Mark design migration checklist done in the plan file; if Agent Gate plan exists, `edit_plan(status=done, completion_summary=...)`.

---

## Spec coverage self-review

| Spec requirement | Task |
|------------------|------|
| Primitive → Semantic → Role | Task 1 |
| Chrome status/TopBar/GNB + Title Case | Task 2 |
| Typography title roles | Task 1–2 |
| Shared Loading/Empty/Error | Task 3 |
| Login migration | Task 4 |
| Home / Logs / Projects / MyPage | Task 5–6 |
| Detail/create/settings | Task 6 |
| Presentation-only logic boundary | Global + Tasks 3–6 (no API/repo) |
| Roborazzi light/dark refresh | Task 7 |
| ThemeContract expansion | Task 1 |

## Placeholder scan

No TBD/TODO implementation steps; exact type names (`AppRoles`, `titleAppBar`, Title Case strings) are fixed above.

## Type consistency

- `AppTheme.roles.layout.screenPadding|sectionGap|contentGap|statePadding`
- `AppTheme.roles.chrome.topBarHeight|topBarHorizontalPadding`
- `AppTheme.typography.titleAppBar|titleSection`
- Designsystem: `LogFlareLoadingState|LogFlareErrorState|LogFlareEmptyState`
- App wrappers: `ListLoadingState|ListErrorState|ListEmptyState` keep Visual QA tags

---

## Completion summary (2026-08-06)

Executed via Subagent-Driven Development on `main` (user-requested single-branch workspace).

| Task | Result | Tip commit |
|------|--------|------------|
| 1 Roles + title typography | Done | `44b7386` |
| 2 Chrome + Title Case | Done | `2b86c88` |
| 3 State views → designsystem | Done | `29e5b48` |
| 4 Login migration | Done | `9cbf3ec` |
| 5 Home migration | Done | `1e2661c` |
| 6 Feature screens | Done | `c8e4463` |
| 7 Roborazzi + CI | Done | `a77aa1f` |

**Range:** `8516a23..a77aa1f` (10 commits). **CI:** Android CI success (Build / Unit tests / Verify JVM snapshots).

### Fast-follow (not blocking)

Documented from final whole-branch review; do as a separate small PR if desired:

1. Replace cross-role size arithmetic (e.g. `chrome.topBarHeight` used for form controls in `ProjectCommonUiModels`, `screenPadding * N` for MyPage widths) with named role fields or honest literals.
2. Add Roborazzi coverage that includes TopAppBar + GNB (spec asked for chrome-visible snapshots).
3. Delete dead `mainNavHostExtraTopPaddingDp` (or call it from `MainScaffold`) and its orphaned test-only path.
4. Unify remaining `EmptyState` (`LogUiComponents`) onto `LogFlareEmptyState` / `ListEmptyState`.
5. Optional cleanup: unused `AppSurfaceRoles` consumers, `VisualQaSemanticsTest` fixture `"LOGS"` → `"Logs"`.
