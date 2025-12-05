package com.example.logflare_android.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.logflare_android.feature.log.LogListScreen
import com.example.logflare_android.feature.home.HomeScreen
import com.example.logflare_android.feature.mypage.MyPageScreen
import com.example.logflare_android.feature.project.ProjectListScreen
import com.example.logflare_android.feature.project.ProjectCreateScreen
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
    
    Scaffold(
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
        BottomNavItem(route = Route.Home, icon = Icons.Default.Home, label = "Home"),
    BottomNavItem(route = Route.Logs, icon = Icons.AutoMirrored.Filled.List, label = "Logs"),
    BottomNavItem(route = Route.Projects, icon = Icons.AutoMirrored.Filled.List, label = "Projects"),
        BottomNavItem(route = Route.MyPage, icon = Icons.Default.Person, label = "MyPage")
    )
    
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { 
                it.route == item.route.path 
            } == true
            
            NavigationBarItem(
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
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
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
            LogListScreen()
        }
        composable(Route.Projects.path) {
            ProjectListScreen(onProjectClick = { projectId ->
                navController.navigate(Route.ProjectDetail.createRoute(projectId))
            })
        }
        composable(Route.ProjectCreate.path) {
            // Simple project creation screen; on success navigate back to Projects
            ProjectCreateScreen(onCreated = { navController.navigate(Route.Projects.path) })
        }
        composable(Route.MyPage.path) { MyPageScreen(onLogout = onLogout) }
        composable(
            route = Route.ProjectDetail.path,
            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
        ) {
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenProjectSettings = { projectId ->
                    navController.navigate(Route.ProjectSettings.createRoute(projectId))
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
    }
}

private data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val label: String
)
