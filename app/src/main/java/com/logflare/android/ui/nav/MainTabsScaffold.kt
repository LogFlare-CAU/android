package com.logflare.android.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.logflare.core.designsystem.v2.LfIcons
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.BottomNavBar
import com.example.logflare.core.designsystem.v2.components.LfScaffold
import com.example.logflare.core.designsystem.v2.components.NavItem
import com.logflare.android.R

/**
 * App frame: the [BottomNavBar] while a tab is on top, nothing on sub-screens (they have their
 * own top bar with Back). Insets are applied once here, by [LfScaffold], except on
 * [Destinations.FullBleed] routes, which paint edge to edge and apply insets themselves.
 */
@Composable
fun MainTabsScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val tab = entry?.destination?.route?.takeIf { it in Destinations.Tabs }
    val items = tabItems()
    // Full-bleed routes (Login) get no scaffold insets and apply their own, once, over their own surface.
    val insets = if (entry?.destination?.route in Destinations.FullBleed) WindowInsets(0) else WindowInsets.safeDrawing
    LfScaffold(
        modifier = modifier,
        contentInsets = insets,
        bottomBar = tab?.let { selected ->
            { BottomNavBar(items = items, selected = selected, onSelect = { navController.navigateToTab(it) }) }
        },
    ) { content() }
}

/**
 * Switches tabs keeping each tab's back stack and screen state: the tab left is saved when
 * popped to Home and restored when selected again.
 *
 * Selecting the tab the user is already in never rebuilds it: inside one of its sub-screens it
 * pops back to the tab's root; on the root it only calls [onReselect] (e.g. scroll to top).
 */
fun NavController.navigateToTab(route: String, onReselect: (route: String) -> Unit = {}) {
    if (currentTab() == route) {
        if (currentDestination?.route == route) onReselect(route) else popBackStack(route, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(Destinations.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens the Logs tab like [navigateToTab] (its state and back stack kept) and presets its level
 * filter to [minLevel] (null: no preset) via [Destinations.LogsMinLevelArg].
 */
fun NavController.openLogs(minLevel: LfLevel?) {
    navigateToTab(Destinations.Logs)
    val entry = runCatching { getBackStackEntry(Destinations.Logs) }.getOrNull() ?: return
    entry.savedStateHandle[Destinations.LogsMinLevelArg] = minLevel?.name?.uppercase()
}

/**
 * The tab whose stack is showing. Switching tabs pops back to Home, so at most one other tab's
 * root is on the back stack; without one, everything sits on Home.
 */
private fun NavController.currentTab(): String =
    Destinations.Tabs.firstOrNull { it != Destinations.Home && hasEntry(it) } ?: Destinations.Home

private fun NavController.hasEntry(route: String): Boolean =
    runCatching { getBackStackEntry(route) }.isSuccess

@Composable
private fun tabItems() = listOf(
    NavItem(Destinations.Home, stringResource(R.string.tab_home), Icons.Outlined.Home, Icons.Filled.Home),
    NavItem(Destinations.Logs, stringResource(R.string.tab_logs), Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    NavItem(Destinations.Projects, stringResource(R.string.tab_projects), LfIcons.FolderOutlined, LfIcons.Folder),
    NavItem(Destinations.My, stringResource(R.string.tab_my), Icons.Outlined.Person, Icons.Filled.Person),
)
