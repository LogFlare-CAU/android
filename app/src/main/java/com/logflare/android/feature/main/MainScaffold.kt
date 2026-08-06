package com.logflare.android.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.navigation.LogFlareGnbItem
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.example.logflare.core.designsystem.R as DesignSystemR
import com.logflare.android.feature.home.HomeScreen
import com.logflare.android.feature.log.LogDetailScreen
import com.logflare.android.feature.log.LogListScreen
import com.logflare.android.feature.mypage.AddMemberScreen
import com.logflare.android.feature.mypage.EditMemberScreen
import com.logflare.android.feature.mypage.LogoutScreen
import com.logflare.android.feature.mypage.MyPageScreen
import com.logflare.android.feature.project.ProjectCreateScreen
import com.logflare.android.feature.project.ProjectListScreen
import com.logflare.android.feature.project.ProjectSettingsScreen
import com.logflare.android.feature.projectdetail.ProjectDetailScreen
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.navigation.Route
import com.logflare.android.viewmodel.ThemeViewModel

/**
 * Main app scaffold with bottom navigation.
 * Contains the MainGraph with Home, Logs, Projects, and MyPage tabs.
 */

data class GnbItem(
    val route: Route,
    @androidx.annotation.DrawableRes val iconRes: Int,
    val label: String,
    val testTag: String,
)


@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    intentProjectId: Int? = null,
    intentErrorId: Int? = null,
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        if (intentProjectId != null || intentErrorId != null) {
            navController.navigate(Route.Logs.path) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val projectDetailTitle by navBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("projectName", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    val hideScaffoldTopBar = shouldHideScaffoldTopBar(currentRoute)

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentColor = AppTheme.colors.onBackground,
        topBar = {
            if (!hideScaffoldTopBar) {
                LogFlareTopAppBar(
                    titleType = when (currentRoute) {
                        Route.Home.path -> TopAppBarTitleType.Default
                        Route.Logs.path -> TopAppBarTitleType.Title
                        Route.Projects.path -> TopAppBarTitleType.Title
                        Route.MyPage.path -> TopAppBarTitleType.Title
                        Route.ProjectCreate.path -> TopAppBarTitleType.Title
                        Route.ProjectDetail.path -> TopAppBarTitleType.Title
                        Route.ProjectSettings.path -> TopAppBarTitleType.Title
                        Route.LogDetail.path -> TopAppBarTitleType.Title
                        else -> TopAppBarTitleType.Default
                    },
                    titleText = when (currentRoute) {
                        Route.Logs.path -> "LOGS"
                        Route.Projects.path -> "PROJECTS"
                        Route.MyPage.path -> "MYPAGE"
                        Route.ProjectCreate.path -> "CREATE PROJECT"
                        Route.ProjectDetail.path -> projectDetailTitle ?: "PROJECT DETAIL"
                        Route.ProjectSettings.path -> "PROJECT SETTINGS"
                        Route.LogDetail.path -> "LOG DETAILS"
                        else -> null
                    },
                    onBack = when (currentRoute) {
                        Route.Home.path,
                        Route.Logs.path,
                        Route.Projects.path,
                        Route.MyPage.path -> null

                        null -> null
                        else -> {
                            { navController.popBackStack() }
                        }
                    },
                    onClose = null,
                    backTestTag = VisualQaTags.NavigateBack,
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        val extraTop = mainNavHostExtraTopPaddingDp(currentRoute)
        MainNavHost(
            navController = navController,
            modifier = Modifier
                .padding(paddingValues)
                .then(
                    if (extraTop > 0) Modifier.padding(top = extraTop.dp) else Modifier,
                ),
            onLogout = onLogout,
            themeViewModel = themeViewModel,
        )
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        GnbItem(route = Route.Home, iconRes = DesignSystemR.drawable.ic_home, label = "Home", testTag = VisualQaTags.NavHome),
        GnbItem(route = Route.Logs, iconRes = DesignSystemR.drawable.ic_log, label = "Logs", testTag = VisualQaTags.NavLogs),
        GnbItem(route = Route.Projects, iconRes = DesignSystemR.drawable.ic_project, label = "Projects", testTag = VisualQaTags.NavProjects),
        GnbItem(route = Route.MyPage, iconRes = DesignSystemR.drawable.ic_mypage, label = "MyPage", testTag = VisualQaTags.NavMyPage),
    )

    NavigationBar(containerColor = AppTheme.colors.surface) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.route.path
            } == true

            LogFlareGnbItem(
                selected = selected,
                onClick = {
                    // Special-case Home: clear its saved state so transient screens (eg. Create) don't persist when returning
                    if (item.route == Route.Home) {
                        navController.navigate(item.route.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                // remove inclusive to clear any nested destinations under startDestination
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(item.route.path) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                iconRes = item.iconRes,
                label = item.label,
                modifier = Modifier.testTag(item.testTag),
            )
        }
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    themeViewModel: ThemeViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.path,
        modifier = modifier
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onProjectSelected = { pid ->
                    navController.navigate(Route.ProjectDetail.createRoute(pid))
                },
                onViewMoreLogs = {
                    // navigate to Logs tab
                    navController.navigate(Route.Logs.path) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateProject = {
                    navController.navigate(Route.ProjectCreate.path)
                }
            )
        }
        composable(Route.Logs.path) {
            LogListScreen(
                onLogClick = { navController.navigate(Route.LogDetail.path) }
            )
        }
        composable(Route.Projects.path) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Route.ProjectDetail.createRoute(projectId))
                },
                onCreateProject = {
                    navController.navigate(Route.ProjectCreate.path)
                },
            )
        }
        composable(Route.ProjectCreate.path) {
            ProjectCreateScreen(onCreated = { navController.navigate(Route.Projects.path) })
        }
        composable(Route.MyPage.path) {
            MyPageScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigate(Route.MyPageLogout.path) },
                onAddMember = { navController.navigate(Route.MyPageAddMember.path) },
                onEditMember = { username ->
                    navController.navigate(Route.MyPageEditMember.createRoute(username))
                },
                themeViewModel = themeViewModel,
            )
        }
        composable(Route.MyPageAddMember.path) {
            AddMemberScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Route.MyPageEditMember.path,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) {
            EditMemberScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.MyPageLogout.path) {
            LogoutScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable(
            route = Route.ProjectDetail.path,
            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
        ) { backStackEntry ->
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenProjectSettings = { projectId ->
                    navController.navigate(Route.ProjectSettings.createRoute(projectId))
                },
                onLogClick = { navController.navigate(Route.LogDetail.path) },
                onProjectNameResolved = { name ->
                    backStackEntry.savedStateHandle["projectName"] = name
                }
            )
        }
        composable(
            route = Route.ProjectSettings.path,
            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getInt("projectId") ?: return@composable
            ProjectSettingsScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onDelete = { navController.popBackStack(Route.Projects.path, inclusive = false) }
            )
        }
        composable(route = Route.LogDetail.path) {
            LogDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
