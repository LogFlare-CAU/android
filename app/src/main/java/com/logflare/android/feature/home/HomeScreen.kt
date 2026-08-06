package com.logflare.android.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.model.ErrorlogDTO
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.enums.color
import com.logflare.android.feature.auth.AuthUiState
import com.logflare.android.feature.auth.AuthViewModel
import com.logflare.android.feature.log.LogViewModel
import com.logflare.android.feature.log.LogsUiState
import com.logflare.android.feature.project.ProjectsUiState
import com.logflare.android.feature.project.ProjectsViewModel
import com.logflare.android.ui.VisualQaTags

private const val HomeRecentLogLimit = 5
private const val HomeProjectPreviewLimit = 5
private const val HomeLogPreviewMaxChars = 96

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
        logsVm.getLogs(HomeRecentLogLimit)
    }

    HomeScreenContent(
        authState = userState,
        projectsState = projectsState,
        logsState = logsState,
        onProjectSelected = onProjectSelected,
        onViewMoreLogs = onViewMoreLogs,
        onCreateProject = onCreateProject,
    )
}

@Composable
fun HomeScreenContent(
    authState: AuthUiState,
    projectsState: ProjectsUiState,
    logsState: LogsUiState,
    onProjectSelected: (Int) -> Unit,
    onViewMoreLogs: () -> Unit,
    onCreateProject: () -> Unit,
) {
    val recentLogs = logsState.errorLogs.take(HomeRecentLogLimit)
    val previewProjects = projectsState.items.take(HomeProjectPreviewLimit)

    // Single LazyColumn so Project List stays reachable when recent logs are long.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.Home)
            .padding(bottom = 12.dp),
    ) {
        item(key = "profile") {
            ProfileCard(authState = authState)
        }

        item(key = "recent_logs_header") {
            SectionHeader(
                title = "Recent Logs",
                actionLabel = "View more",
                onAction = onViewMoreLogs,
                modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            )
        }

        when {
            logsState.loading -> item(key = "logs_loading") {
                Text(
                    "Loading logs…",
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .testTag(VisualQaTags.Loading),
                )
            }
            logsState.error != null -> item(key = "logs_error") {
                Text(
                    "Logs error: ${logsState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .testTag(VisualQaTags.Error),
                )
            }
            recentLogs.isEmpty() -> item(key = "logs_empty") {
                EmptyStateCard(
                    text = "No logs found.\nPlease check your server connection.",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag(VisualQaTags.Empty),
                )
            }
            else -> items(
                items = recentLogs,
                key = { "log_${it.id}" },
            ) { e ->
                LogRowItem(
                    log = e,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        item(key = "projects_header") {
            SectionHeader(
                title = "Project List",
                actionLabel = "Create Project",
                onAction = onCreateProject,
                actionTestTag = VisualQaTags.CreateProject,
                modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            )
        }

        when {
            projectsState.loading -> item(key = "projects_loading") {
                Text(
                    "Loading projects…",
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .testTag(VisualQaTags.Loading),
                )
            }
            projectsState.error != null -> item(key = "projects_error") {
                Text(
                    "Projects error: ${projectsState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .testTag(VisualQaTags.Error),
                )
            }
            previewProjects.isEmpty() -> item(key = "projects_empty") {
                EmptyStateCard(
                    text = "No projects found.",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag(VisualQaTags.Empty),
                )
            }
            else -> items(
                items = previewProjects,
                key = { "project_${it.id}" },
            ) { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(VisualQaTags.projectCard(p.id))
                        .clickable { onProjectSelected(p.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

    }
}

@Composable
private fun ProfileCard(authState: AuthUiState) {
    Card(
        modifier = Modifier
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        val username = when {
            authState.loading -> "Loading..."
            authState.username == null -> "Guest"
            else -> authState.username
        }

        val perm = UserPermission.fromCode(authState.permission)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                username?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                authState.profileError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Surface(
                color = perm.color(AppTheme.colors),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = perm.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionTestTag: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        AssistChip(
            onClick = onAction,
            label = { Text(actionLabel) },
            colors = AssistChipDefaults.assistChipColors(containerColor = Color.Transparent),
            modifier = if (actionTestTag != null) {
                Modifier.testTag(actionTestTag)
            } else {
                Modifier
            },
        )
    }
}

@Composable
private fun EmptyStateCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogRowItem(
    log: ErrorlogDTO,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badgeColor = LogLevel.fromLabel(log.level).color(AppTheme.colors)
        Surface(
            color = badgeColor,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(12.dp),
        ) {}
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cropLongText("${log.errortype ?: "Error"}: ${log.message}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = log.level.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
            )
        }
    }
}

private fun cropLongText(text: String, maxLength: Int = HomeLogPreviewMaxChars): String {
    return if (text.length <= maxLength) {
        text
    } else {
        text.take(maxLength) + "…"
    }
}
