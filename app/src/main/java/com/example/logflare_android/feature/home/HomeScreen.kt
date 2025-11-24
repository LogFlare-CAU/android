package com.example.logflare_android.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.feature.log.LogViewModel
import com.example.logflare_android.feature.project.ProjectsViewModel

/**
 * Home screen: dashboard style summary showing recent logs and a few projects.
 * Pulls first project to load recent logs; lightweight overview.
 */
@Composable
fun HomeScreen(
    onProjectSelected: (Int) -> Unit,
    projectsVm: ProjectsViewModel = hiltViewModel(),
    logsVm: LogViewModel = hiltViewModel()
) {
    val projectsState by projectsVm.ui.collectAsState()
    val logsState by logsVm.ui.collectAsState()

    LaunchedEffect(Unit) { projectsVm.refresh() }
    LaunchedEffect(projectsState.items) {
        projectsState.items.firstOrNull()?.let { p -> logsVm.refresh(p.id) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text("최근 로그", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
        when {
            logsState.loading -> Text("Loading logs…")
            logsState.error != null -> Text("Logs error: ${logsState.error}", color = MaterialTheme.colorScheme.error)
            else -> LazyColumn(modifier = Modifier.padding(bottom = 12.dp)) {
                items(logsState.items.take(5)) { e ->
                    Text("[${e.level}] ${e.errortype ?: "Error"}: ${e.message}", modifier = Modifier.padding(4.dp))
                }
            }
        }

        Text("Projects", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
        when {
            projectsState.loading -> Text("Loading projects…")
            projectsState.error != null -> Text("Projects error: ${projectsState.error}", color = MaterialTheme.colorScheme.error)
            else -> LazyColumn {
                items(projectsState.items.take(5)) { p ->
                    Text(p.name, modifier = Modifier
                        .padding(vertical = 6.dp)
                        .padding(horizontal = 4.dp)
                        .then(Modifier))
                }
            }
        }
    }
}
