package com.logflare.android.ui.nav

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.logflare.core.designsystem.v2.components.BottomNavBar
import com.example.logflare.core.designsystem.v2.components.LfScaffold
import com.example.logflare.core.designsystem.v2.components.NavItem
import com.logflare.android.R

/**
 * App frame: the [BottomNavBar] while a tab is on top, nothing on sub-screens (they have their
 * own top bar with Back). Insets are applied once here, by [LfScaffold].
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
    LfScaffold(
        modifier = modifier,
        bottomBar = tab?.let { selected ->
            { BottomNavBar(items = items, selected = selected, onSelect = navController::navigateToTab) }
        },
    ) { content() }
}

/**
 * Switches tabs keeping each tab's back stack and screen state: the tab left is saved when
 * popped to Home and restored when selected again; reselecting the current tab does nothing.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Destinations.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun tabItems() = listOf(
    NavItem(Destinations.Home, stringResource(R.string.tab_home), Icons.Outlined.Home, Icons.Filled.Home),
    NavItem(Destinations.Logs, stringResource(R.string.tab_logs), Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    NavItem(Destinations.Projects, stringResource(R.string.tab_projects), FolderOutlined, FolderFilled),
    NavItem(Destinations.My, stringResource(R.string.tab_my), Icons.Outlined.Person, Icons.Filled.Person),
)

// Material "Folder" icons; the extended icon set is not a dependency. The tint replaces the fill.
private val FolderFilled: ImageVector by lazy {
    folderIcon(
        "Filled.Folder",
        "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z",
    )
}

private val FolderOutlined: ImageVector by lazy {
    folderIcon(
        "Outlined.Folder",
        "M9.17,6l2,2H20v10H4V6h5.17M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z",
    )
}

private fun folderIcon(name: String, path: String) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
        .build()
