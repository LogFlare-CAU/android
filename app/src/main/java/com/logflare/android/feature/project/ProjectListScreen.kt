package com.logflare.android.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.common.ListEmptyState
import com.logflare.android.ui.common.ListErrorState
import com.logflare.android.ui.common.ListLoadingState

/**
 * Project list screen showing all user projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Int) -> Unit,
    onCreateProject: () -> Unit = {},
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.ui.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ProjectListScreenContent(
        uiState = uiState,
        onProjectClick = onProjectClick,
        onCreateProject = onCreateProject,
        onRefresh = { viewModel.refresh(fromPull = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreenContent(
    uiState: ProjectsUiState,
    onProjectClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onCreateProject: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .testTag(VisualQaTags.Projects),
    ) {
        when {
            uiState.loading && uiState.items.isEmpty() -> {
                ListLoadingState()
            }

            uiState.error != null && uiState.items.isEmpty() -> {
                ListErrorState(
                    message = uiState.error ?: "Unable to load projects",
                    onRetry = onRefresh,
                )
            }

            uiState.items.isEmpty() -> {
                ListEmptyState(
                    title = "No projects yet",
                    subtitle = "Create a project to start collecting logs",
                    actionLabel = "Create Project",
                    actionTestTag = VisualQaTags.CreateProject,
                    onAction = onCreateProject,
                )
            }

            else -> {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.refreshing,
                    onRefresh = onRefresh,
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AppTheme.roles.layout.screenPadding,
                            vertical = AppTheme.roles.layout.contentGap,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap),
                    ) {
                        item(key = "projects_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = AppTheme.spacing.s1),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Projects",
                                    style = AppTheme.typography.bodyLgBold,
                                    color = AppTheme.colors.onSurface,
                                )
                                TextButton(
                                    onClick = onCreateProject,
                                    modifier = Modifier.testTag(VisualQaTags.CreateProject),
                                ) {
                                    Text(
                                        text = "Create",
                                        color = AppTheme.colors.primary.default,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        if (uiState.error != null) {
                            item(key = "projects_inline_error") {
                                Text(
                                    text = uiState.error.orEmpty(),
                                    color = AppTheme.colors.red.default,
                                    style = AppTheme.typography.bodySmMedium,
                                    modifier = Modifier.padding(bottom = AppTheme.spacing.s1),
                                )
                            }
                        }
                        items(
                            items = uiState.items,
                            key = { it.id },
                        ) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.id) },
                                connectionHealthy = null,
                                modifier = Modifier.testTag(VisualQaTags.projectCard(project.id)),
                            )
                        }
                    }
                }
            }
        }
    }
}
