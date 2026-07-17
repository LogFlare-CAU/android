package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.logflare.android.ui.VisualQaTags

/**
 * Project list screen showing all user projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Int) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
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
        onRefresh = { viewModel.refresh(fromPull = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreenContent(
    uiState: ProjectsUiState,
    onProjectClick: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.Projects),
    ) {
        when {
            uiState.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(VisualQaTags.Loading),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(VisualQaTags.Error),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(VisualQaTags.Empty),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "No projects found",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Create a project to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id },
                        ) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.id) },
                                connectionHealthy = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
