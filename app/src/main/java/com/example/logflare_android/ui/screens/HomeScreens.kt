package com.example.logflare_android.ui.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.logflare_android.ui.navigation.Route
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings

@Composable
fun HomeScaffold() {
    val navController = rememberNavController()
    val items = listOf(Route.Projects, Route.Settings)
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val current = navBackStackEntry?.destination?.route
                items.forEach { route ->
                    NavigationBarItem(
                        selected = current == route.path,
                        onClick = {
                            navController.navigate(route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (route) {
                                Route.Projects -> Icon(Icons.Default.List, contentDescription = null)
                                Route.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                                else -> Icon(Icons.Default.List, contentDescription = null)
                            }
                        },
                        label = { Text(route.path) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Route.Projects.path, modifier = Modifier.then(Modifier)) {
            composable(Route.Projects.path) { ProjectTabsScreen() }
            composable(Route.Settings.path) { SettingsScreen() }
        }
    }
}

@Composable
fun ProjectTabsScreen(
    projectsVm: com.example.logflare_android.viewmodel.ProjectsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    selectVm: com.example.logflare_android.viewmodel.SelectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val projectsState by projectsVm.ui.collectAsState()
    val selectedProjectId by selectVm.projectId.collectAsState()

    LaunchedEffect(Unit) { projectsVm.refresh() }

    when {
        projectsState.loading -> Text("Loading projects…")
        projectsState.error != null -> Text("Projects error: ${projectsState.error}")
        projectsState.items.isEmpty() -> Text("프로젝트가 없습니다. 먼저 프로젝트를 생성하세요.")
        else -> {
            val projects = projectsState.items
            val selectedIndex = projects.indexOfFirst { it.id == selectedProjectId }.let { if (it >= 0) it else 0 }
            if (selectedProjectId == null && projects.isNotEmpty()) {
                LaunchedEffect(projects) { selectVm.selectProject(projects.first().id) }
            }
            TabRow(selectedTabIndex = selectedIndex) {
                projects.forEachIndexed { idx, proj ->
                    Tab(
                        selected = idx == selectedIndex,
                        onClick = { selectVm.selectProject(proj.id) },
                        text = { Text(proj.name) }
                    )
                }
            }
            Text("프로젝트 탭 전환만 구현됨. 로그 목록은 별도 기능에서 제공합니다.", modifier = androidx.compose.ui.Modifier.padding(12.dp))
        }
    }
}

@Composable fun SettingsScreen() { Text("Settings") }
