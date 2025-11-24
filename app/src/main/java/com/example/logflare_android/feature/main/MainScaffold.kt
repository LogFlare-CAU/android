package com.example.logflare_android.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
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
import com.example.logflare_android.feature.project.ProjectListScreen
import com.example.logflare_android.feature.settings.SettingsScreen
import com.example.logflare_android.ui.navigation.Route

/**
 * Main app scaffold with bottom navigation.
 * Contains the MainGraph with Projects, Logs, and Settings tabs.
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
        BottomNavItem(
            route = Route.Projects,
            icon = Icons.Default.Home,
            label = "Projects"
        ),
        BottomNavItem(
            route = Route.Logs,
            icon = Icons.Default.List,
            label = "Logs"
        ),
        BottomNavItem(
            route = Route.Settings,
            icon = Icons.Default.Settings,
            label = "Settings"
        )
    )
    
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { 
                it.route == item.route.path 
            } == true
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route.path) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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
        startDestination = Route.Projects.path,
        modifier = modifier
    ) {
        // Projects tab - shows project list
        composable(Route.Projects.path) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Route.LogDetail.createRoute(projectId))
                }
            )
        }
        
        // Logs tab - shows all logs (or recent logs)
        composable(Route.Logs.path) {
            LogListScreen(projectId = null) // null = show all projects or recent
        }
        
        // Settings tab
        composable(Route.Settings.path) {
            SettingsScreen(
                onLogout = onLogout
            )
        }
        
        // Detail screen: Logs for a specific project
        composable(
            route = Route.LogDetail.path,
            arguments = listOf(
                navArgument("projectId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getInt("projectId")
            if (projectId != null) {
                LogListScreen(projectId = projectId)
            }
        }
    }
}

private data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val label: String
)
