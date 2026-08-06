package com.logflare.android.feature.projectdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.LogSort
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.common.*
import com.logflare.android.ui.components.BackHeader

@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLogClick: () -> Unit,
    onProjectNameResolved: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    LaunchedEffect(uiState.projectName) {
        if (uiState.projectName.isNotBlank()) {
            onProjectNameResolved(uiState.projectName)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colors.surface
    ) {
        ProjectDetailScreenContent(
            uiState = uiState,
            onBack = onBack,
            onOpenProjectSettings = onOpenProjectSettings,
            onLevelSelected = { level -> viewModel.onLevelSelected(level) },
            onLogfileSelected = { id -> viewModel.onLogfileSelected(id) },
            onSortSelected = { sort -> viewModel.onSortSelected(sort) },
            onLogClick = { log -> viewModel.onLogClick(log); onLogClick() },
            onLoadMore = { viewModel.loadMoreLogs() },
        )
    }
}

@Composable
fun ProjectDetailScreenContent(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLevelSelected: (LogLevel) -> Unit,
    onLogfileSelected: (Int) -> Unit,
    onSortSelected: (LogSort) -> Unit,
    onLogClick: (ProjectDetailLog) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.ProjectDetail)
            .background(AppTheme.colors.surface)
            .navigationBarsPadding(),
    ) {
        when {
            uiState.loading -> ListLoadingState()

            // Only block the screen for fatal project-load errors.
            // Log-load failures use logsError and keep Settings reachable.
            uiState.error != null -> ListErrorState(message = uiState.error!!)

            else -> ProjectDetailBody(
                uiState = uiState,
                onBack = onBack,
                onOpenProjectSettings = onOpenProjectSettings,
                onLevelSelected = onLevelSelected,
                onLogfileSelected = onLogfileSelected,
                onSortSelected = onSortSelected,
                onLogClick = onLogClick,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun ProjectDetailBody(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLevelSelected: (LogLevel) -> Unit,
    onLogfileSelected: (Int) -> Unit,
    onSortSelected: (LogSort) -> Unit,
    onLogClick: (ProjectDetailLog) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProjectSettingsCard(
            label = uiState.settingsLabel,
            onClick = { onOpenProjectSettings(uiState.projectId) },
            modifier = Modifier.testTag(VisualQaTags.OpenProjectSettings),
        )
        FilterPanel(
            filterState = uiState.filterState,
            onLevelSelected = onLevelSelected,
            onLogfileSelected = onLogfileSelected,
            onSortSelected = onSortSelected
        )
        uiState.logsError?.let { message ->
            Text(
                text = message,
                color = AppTheme.colors.red.default,
                style = AppTheme.typography.bodyMdMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTheme.roles.layout.screenPadding,
                        vertical = AppTheme.spacing.s2,
                    )
                    .testTag(VisualQaTags.Error),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            when {
                uiState.logs.isEmpty() -> ListEmptyState(
                    title = if (uiState.projectId > 0) {
                        "No logs available"
                    } else {
                        "No logs for this Project / LogFile"
                    },
                    subtitle = if (uiState.filterState.selectedLevel.isNotEmpty()) {
                        "Try adjusting the filters"
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LogsSection(
                    logs = uiState.logs,
                    showMoreLoading = uiState.showMoreLoading,
                    showMoreHasMore = uiState.showMoreHasMore,
                    onLoadMore = onLoadMore,
                    onLogClick = onLogClick,
                )
            }
        }
    }
}

@Composable
private fun ProjectSettingsCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = AppTheme.roles.layout.screenPadding)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppTheme.radius.large,
        color = AppTheme.colors.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.roles.layout.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTheme.typography.bodyMdMedium,
                color = AppTheme.colors.onSurface.copy(alpha = 0.86f)
            )
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.s6)
                    .clip(AppTheme.radius.medium)
                    .background(AppTheme.colors.surface.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun LogsSection(
    logs: List<ProjectDetailLog>,
    showMoreLoading: Boolean,
    showMoreHasMore: Boolean,
    onLoadMore: () -> Unit,
    onLogClick: (ProjectDetailLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.roles.layout.screenPadding)
            .padding(
                top = AppTheme.roles.layout.screenPadding,
                bottom = AppTheme.roles.layout.screenPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap)
    ) {
        items(items = logs, key = { it.id }) { log ->
            GlobalLogCard(
                log = LogCardInfo(
                    log.level.label,
                    log.timestamp,
                    log.message,
                    log.projectName,
                    log.fileName,
                ),
                onClick = { onLogClick(log) },
                modifier = Modifier.testTag(VisualQaTags.logCard(log.id)),
            )
        }
        if (showMoreHasMore) {
            item(key = "load_more") {
                LoadMoreRow(
                    loading = showMoreLoading,
                    onClick = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    filterState: ProjectDetailFilterState,
    onLevelSelected: (LogLevel) -> Unit,
    onLogfileSelected: (Int) -> Unit,
    onSortSelected: (LogSort) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.roles.layout.screenPadding)
            .padding(horizontal = AppTheme.roles.layout.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap)
        ) {
            CommonFilterDropdown(
                title = "Log Level",
                isActive = filterState.selectedLevel.isNotEmpty(),
                modifier = Modifier.weight(1f),
                interactionTag = VisualQaTags.FilterLogLevel,
            ) {
                LogLevel.entries.forEach { level ->
                    CommonCheckRow(
                        label = level.label,
                        selected = filterState.selectedLevel.contains(level),
                        highlightColor = AppTheme.colors.primary.default,
                        onClick = { onLevelSelected(level) }
                    )
                }
            }

            CommonFilterDropdown(
                title = "Log File",
                isActive = filterState.logfileOptions.any { it.selected },
                modifier = Modifier.weight(1f)
            ) {
                filterState.logfileOptions.forEach { option ->
                    CommonRadioRow(
                        label = option.fileName,
                        selected = option.selected,
                        onClick = { onLogfileSelected(option.id) }
                    )
                }
            }
            CommonFilterDropdown(
                title = "Sort By",
                isActive = filterState.sortBy != LogSort.NEWEST,
                modifier = Modifier.weight(1f),
                interactionTag = VisualQaTags.FilterSort,
            ) {
                CommonRadioRow(
                    label = "Newest",
                    selected = filterState.sortBy == LogSort.NEWEST,
                    onClick = { onSortSelected(LogSort.NEWEST) }
                )
                CommonRadioRow(
                    label = "Oldest",
                    selected = filterState.sortBy == LogSort.OLDEST,
                    onClick = { onSortSelected(LogSort.OLDEST) }
                )
            }
        }
    }
}
