package com.example.logflare_android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
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
    val items = listOf(Route.Projects, Route.Logs, Route.Settings)
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
                                Route.Logs -> Icon(Icons.Default.List, contentDescription = null)
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
        NavHost(navController, startDestination = Route.Projects.path, modifier = Modifier.padding(padding)) {
            composable(Route.Projects.path) { ProjectListScreen(onProjectSelected = { navController.navigate(Route.Logs.path) }) }
            composable(Route.Logs.path) { LogListScreen() }
            composable(Route.Settings.path) { SettingsScreen() }
        }
    }
}

@Composable fun ProjectListScreen(
    onProjectSelected: (Int) -> Unit,
    vm: com.example.logflare_android.viewmodel.ProjectsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    selectVm: com.example.logflare_android.viewmodel.SelectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    when {
        state.loading -> Text("Loading…")
        state.error != null -> Text("Error: ${state.error}")
        else -> LazyColumn(contentPadding = PaddingValues()) {
            items(state.items) { p ->
                Text(p.name, modifier = Modifier
                    .clickable {
                        selectVm.selectProject(p.id)
                        onProjectSelected(p.id)
                    }
                    .padding(12.dp)
                )
            }
        }
    }
}
@Composable fun LogListScreen(
    vm: com.example.logflare_android.viewmodel.LogViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    selectVm: com.example.logflare_android.viewmodel.SelectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by vm.ui.collectAsState()
    val projectId by selectVm.projectId.collectAsState()
    LaunchedEffect(projectId) { projectId?.let { vm.refresh(it) } }
    when {
        projectId == null -> Text("Select a project first")
        state.loading -> Text("Loading…")
        state.error != null -> Text("Error: ${state.error}")
        else -> LazyColumn(contentPadding = PaddingValues()) {
            items(state.items) { e ->
                Text("[${e.level}] ${e.errortype ?: "Error"}: ${e.message}", modifier = Modifier.padding(12.dp))
            }
        }
    }
}
@Composable fun SettingsScreen() { Text("Settings") }
