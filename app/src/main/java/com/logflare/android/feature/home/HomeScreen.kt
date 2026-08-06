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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
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
import com.logflare.android.ui.common.ListEmptyState
import com.logflare.android.ui.common.ListErrorState
import com.logflare.android.ui.common.ListLoadingState
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
            .padding(bottom = AppTheme.roles.layout.contentGap),
    ) {
        item(key = "profile") {
            ProfileCard(authState = authState)
        }

        item(key = "recent_logs_header") {
            SectionHeader(
                title = "Recent Logs",
                actionLabel = "View more",
                onAction = onViewMoreLogs,
                modifier = Modifier.padding(
                    top = AppTheme.roles.layout.sectionGap,
                    start = AppTheme.roles.layout.screenPadding,
                    end = AppTheme.roles.layout.screenPadding,
                ),
            )
        }

        when {
            logsState.loading -> item(key = "logs_loading") {
                ListLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
                )
            }
            logsState.error != null -> item(key = "logs_error") {
                ListErrorState(
                    message = "Logs error: ${logsState.error}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
                )
            }
            recentLogs.isEmpty() -> item(key = "logs_empty") {
                ListEmptyState(
                    title = "No logs found.",
                    subtitle = "Please check your server connection.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
                )
            }
            else -> items(
                items = recentLogs,
                key = { "log_${it.id}" },
            ) { e ->
                LogRowItem(
                    log = e,
                    modifier = Modifier.padding(
                        horizontal = AppTheme.roles.layout.screenPadding,
                        vertical = AppTheme.roles.layout.contentGap / 2,
                    ),
                )
            }
        }

        item(key = "projects_header") {
            SectionHeader(
                title = "Project List",
                actionLabel = "Create Project",
                onAction = onCreateProject,
                actionTestTag = VisualQaTags.CreateProject,
                modifier = Modifier.padding(
                    top = AppTheme.roles.layout.sectionGap,
                    start = AppTheme.roles.layout.screenPadding,
                    end = AppTheme.roles.layout.screenPadding,
                ),
            )
        }

        when {
            projectsState.loading -> item(key = "projects_loading") {
                ListLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
                )
            }
            projectsState.error != null -> item(key = "projects_error") {
                ListErrorState(
                    message = "Projects error: ${projectsState.error}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
                )
            }
            previewProjects.isEmpty() -> item(key = "projects_empty") {
                ListEmptyState(
                    title = "No projects found.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.roles.layout.statePadding * 4)
                        .padding(horizontal = AppTheme.roles.layout.screenPadding),
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
                        .padding(
                            horizontal = AppTheme.roles.layout.screenPadding,
                            vertical = AppTheme.spacing.s2,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = p.name,
                        style = AppTheme.typography.bodyMdMedium,
                        color = AppTheme.colors.onSurface,
                    )
                }
            }
        }

    }
}

@Composable
private fun ProfileCard(authState: AuthUiState) {
    Card(
        modifier = Modifier
            .padding(
                top = AppTheme.roles.layout.screenPadding,
                start = AppTheme.roles.layout.screenPadding,
                end = AppTheme.roles.layout.screenPadding,
            )
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceVariant),
        shape = AppTheme.radius.large,
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
                .padding(AppTheme.roles.layout.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                username?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.titleSection,
                        color = AppTheme.colors.onSurface,
                    )
                }
                authState.profileError?.let { err ->
                    Text(
                        text = err,
                        color = AppTheme.colors.red.default,
                        style = AppTheme.typography.bodySmMedium,
                        modifier = Modifier.padding(top = AppTheme.spacing.s1),
                    )
                }
            }
            Surface(
                color = perm.color(AppTheme.colors),
                shape = AppTheme.radius.medium,
            ) {
                Text(
                    text = perm.label,
                    modifier = Modifier.padding(
                        horizontal = AppTheme.roles.layout.contentGap,
                        vertical = AppTheme.roles.layout.contentGap / 2,
                    ),
                    style = AppTheme.typography.bodySmMedium,
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
        Text(
            text = title,
            style = AppTheme.typography.titleSection,
            color = AppTheme.colors.onSurface,
        )
        AssistChip(
            onClick = onAction,
            label = {
                Text(
                    text = actionLabel,
                    style = AppTheme.typography.bodySmMedium,
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = AppTheme.colors.background,
                labelColor = AppTheme.colors.onSurface,
            ),
            modifier = if (actionTestTag != null) {
                Modifier.testTag(actionTestTag)
            } else {
                Modifier
            },
        )
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
            shape = AppTheme.radius.small,
            modifier = Modifier.size(AppTheme.roles.layout.contentGap),
        ) {}
        Spacer(modifier = Modifier.size(AppTheme.spacing.s2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cropLongText("${log.errortype ?: "Error"}: ${log.message}"),
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = log.level.uppercase(),
                style = AppTheme.typography.captionMdMedium,
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
