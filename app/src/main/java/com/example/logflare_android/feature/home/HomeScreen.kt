package com.example.logflare_android.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.auth.AuthViewModel
import com.example.logflare_android.feature.log.LogViewModel
import com.example.logflare_android.feature.project.ProjectsViewModel

/**
 * Home screen: dashboard style summary showing recent logs and a few projects.
 * Pulls first project to load recent logs; lightweight overview.
 */
@Composable
fun HomeScreen(
    onProjectSelected: (Int) -> Unit,
    onViewMoreLogs: () -> Unit = {},
    onCreateProject: () -> Unit = {},
    projectsVm: ProjectsViewModel = hiltViewModel(),
    logsVm: LogViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
) {
    val projectsState by projectsVm.ui.collectAsState()
    val logsState by logsVm.ui.collectAsState()
    val userState by authVm.ui.collectAsState()

    LaunchedEffect(Unit) {
        authVm.getMe()
        projectsVm.refresh()
    }
    LaunchedEffect(projectsState.items) {
        // Fetch only a small recent subset of logs (limit=5) for Home dashboard context
        logsVm.getLogs(5)
        // projectsState.items.firstOrNull()?.let { p -> logsVm.refresh(p.id, limit = 5) }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)) {
        // User info card
        Card(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            val username = when {
                userState.loading -> "Loading..."
                userState.username == null -> "Guest"
                else -> userState.username
            }

            val perm = UserPermission.fromCode(userState.permission)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    username?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Surface(
                    color = perm.color,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = perm.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF9F9F9)
                    )
                }
            }
        }

        // Recent Logs header
        Row(
            modifier = Modifier
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Logs", style = MaterialTheme.typography.titleMedium)
            AssistChip(
                onClick = onViewMoreLogs,
                label = { Text("View more") },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent)
            )
        }
        // Recent Logs content (empty state when no logs)
        when {
            logsState.loading -> Text("Loading logs…", modifier = Modifier.padding(start = 16.dp))
            logsState.error != null -> Text("Logs error: ${logsState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
            logsState.errorLogs.isEmpty() -> EmptyStateCard(
                text = "No logs found.\nPlease check your server connection.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                items(logsState.errorLogs.take(5)) { e ->
                    LogRowItem(e)
                }
            }
        }

        // Project List header
        Row(
            modifier = Modifier
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Project List", style = MaterialTheme.typography.titleMedium)
            AssistChip(
                onClick = onCreateProject,
                label = { Text("Create Project") },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent)
            )
        }
        when {
            projectsState.loading -> Text("Loading projects…", modifier = Modifier.padding(start = 16.dp))
            projectsState.error != null -> Text("Projects error: ${projectsState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
            projectsState.items.isEmpty() -> EmptyStateCard(
                text = "No projects found.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                items(projectsState.items.take(5)) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProjectSelected(p.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyStateCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp),
        color = Color(0xFFEEEEEE),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
        }
    }
}

@Composable
private fun LogRowItem(log: com.example.logflare.core.model.ErrorlogDTO) {
    // Color badge based on level
    val badgeColor = when (log.level.uppercase()) {
        "ERROR", "FATAL" -> Color(0xFFD32F2F)
        "WARN", "WARNING" -> Color(0xFFFFA000)
        "INFO" -> Color(0xFF1976D2)
        "DEBUG" -> Color(0xFF388E3C)
        else -> Color(0xFF616161)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = badgeColor,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${log.errortype ?: "Error"}: ${log.message}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF212121)
            )
            Text(
                text = log.level.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor
            )
        }
    }
}
