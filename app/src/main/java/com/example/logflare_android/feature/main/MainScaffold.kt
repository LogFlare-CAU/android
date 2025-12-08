package com.example.logflare_android.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.logflare_android.feature.home.HomeScreen
import com.example.logflare_android.feature.log.LogDetailScreen
import com.example.logflare_android.feature.log.LogListScreen
import com.example.logflare_android.feature.mypage.MyPageScreen
import com.example.logflare_android.feature.mypage.AddMemberScreen
import com.example.logflare_android.feature.mypage.EditMemberScreen
import com.example.logflare_android.feature.mypage.LogoutScreen
import com.example.logflare_android.feature.project.ProjectCreateScreen
import com.example.logflare_android.feature.project.ProjectListScreen
import com.example.logflare_android.feature.projectdetail.ProjectDetailScreen
import com.example.logflare_android.feature.projectdetail.ProjectSettingsScreen
import com.example.logflare_android.ui.navigation.Route

/**
 * Main app scaffold with bottom navigation.
 * Contains the MainGraph with Home, Logs, Projects, and MyPage tabs.
 */
@Composable
fun MainScaffold(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentEntry = navBackStackEntry

    val projectDetailTitle: String? = if (currentRoute == Route.ProjectDetail.path) {
        currentEntry?.savedStateHandle?.get<String>("projectName")
    } else {
        null
    }

    Scaffold(
        topBar = {
            LogFlareTopAppBar(
                titleType = when (currentRoute) {
                    Route.Home.path -> TopAppBarTitleType.Default
                    Route.Logs.path -> TopAppBarTitleType.Title
                    Route.Projects.path -> TopAppBarTitleType.Title
                    Route.MyPage.path -> TopAppBarTitleType.Title
                    Route.ProjectCreate.path -> TopAppBarTitleType.Title
                    Route.ProjectDetail.path -> TopAppBarTitleType.Title
                    Route.ProjectSettings.path -> TopAppBarTitleType.Title
                    Route.MyPageAddMember.path -> TopAppBarTitleType.Title
                    Route.MyPageEditMember.path -> TopAppBarTitleType.Title
                    Route.MyPageLogout.path -> TopAppBarTitleType.Title
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
                    Route.MyPageAddMember.path -> "ADD MEMBER"
                    Route.MyPageEditMember.path -> "EDIT MEMBER"
                    Route.MyPageLogout.path -> "LOG OUT"
                    Route.LogDetail.path -> "LOG DETAILS"
                    else -> null
                },
                onBack = when (currentRoute) {
                    Route.Home.path,
                    Route.Logs.path,
                    Route.Projects.path,
                    Route.MyPage.path -> null
                    null -> null
                    else -> { { navController.popBackStack() } }
                },
                onClose = null
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onLogout = onLogout
        )
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        GnbItem(route = Route.Home, iconRes = DesignSystemR.drawable.ic_home, label = "Home"),
        GnbItem(route = Route.Logs, iconRes = DesignSystemR.drawable.ic_log, label = "Logs"),
        GnbItem(route = Route.Projects, iconRes = DesignSystemR.drawable.ic_project, label = "Projects"),
        GnbItem(route = Route.MyPage, iconRes = DesignSystemR.drawable.ic_mypage, label = "MyPage")
    )

    NavigationBar(containerColor = AppTheme.colors.neutral.white) {
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
                label = item.label
            )
        }
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
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
            ProjectListScreen(onProjectClick = { projectId ->
                navController.navigate(Route.ProjectDetail.createRoute(projectId))
            })
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
                }
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
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Route.LogDetail.path) {
            LogDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
