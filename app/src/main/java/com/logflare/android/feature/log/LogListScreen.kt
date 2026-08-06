package com.logflare.android.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.logflare.core.designsystem.AppTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.model.ErrorlogDTO
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.LogSort
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.common.*

data class ProjectToggleOption(
    val id: Int,
    val label: String,
    val selected: Boolean
)

sealed interface LogListAction {
    data class OpenLog(val log: ErrorlogDTO) : LogListAction
    data object LoadMore : LogListAction
    data class SelectProject(val projectId: Int?) : LogListAction
    data class ToggleLevel(val level: LogLevel) : LogListAction
    data class ChangeSort(val sort: LogSort) : LogListAction
}

@Composable
fun LogListScreen(
    onLogClick: () -> Unit,
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    LogListScreenContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is LogListAction.OpenLog -> {
                    viewModel.onLogClick(action.log)
                    onLogClick()
                }
                LogListAction.LoadMore -> viewModel.loadMore()
                is LogListAction.SelectProject -> {
                    action.projectId?.let { viewModel.toggleProjectOption(it) }
                }
                is LogListAction.ToggleLevel -> viewModel.setFilter(action.level)
                is LogListAction.ChangeSort -> viewModel.setSortBy(action.sort)
            }
        },
    )
}

@Composable
fun LogListScreenContent(
    uiState: LogsUiState,
    onAction: (LogListAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VisualQaTags.Logs)
            .background(AppTheme.colors.surface)
    ) {
        FilterDropdownRow(
            selectedLevels = uiState.filter,
            onLevelSelected = { level -> onAction(LogListAction.ToggleLevel(level)) },
            projectOptions = uiState.projectOptions,
            onToggleProjects = { id -> onAction(LogListAction.SelectProject(id)) },
            sortSelection = uiState.sortBy,
            onSortSelected = { selection -> onAction(LogListAction.ChangeSort(selection)) }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        ) {
            when {
                uiState.loading -> ListLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = AppTheme.roles.layout.statePadding * 2),
                )
                uiState.error != null -> ListErrorState(
                    message = uiState.error!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = AppTheme.roles.layout.statePadding * 2),
                )
                uiState.errorLogs.isEmpty() -> ListEmptyState(
                    title = if (uiState.selectedProject != null) {
                        "No logs available"
                    } else {
                        "No logs for this Project / LogFile"
                    },
                    subtitle = if (uiState.filter.isNotEmpty()) "Try adjusting the filters" else null,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LogListContent(
                    logs = uiState.errorLogs,
                    projectNames = uiState.projectNames,
                    showLoadMore = uiState.hasMore,
                    loadingMore = uiState.loadingMore,
                    onLoadMore = { onAction(LogListAction.LoadMore) },
                    onLogClick = { log -> onAction(LogListAction.OpenLog(log)) }
                )
            }
        }
    }
}


@Composable
private fun FilterDropdownRow(
    selectedLevels: List<LogLevel>,
    onLevelSelected: (LogLevel) -> Unit,
    projectOptions: List<ProjectToggleOption>,
    onToggleProjects: (Int) -> Unit,
    sortSelection: LogSort,
    onSortSelected: (LogSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.roles.layout.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap)
    ) {
        CommonFilterDropdown(
            title = "Log Level",
            isActive = selectedLevels.isNotEmpty(),
            modifier = Modifier.weight(1f),
            interactionTag = VisualQaTags.FilterLogLevel,
        ) {
            LogLevel.entries.forEach { level ->
                CommonCheckRow(
                    label = level.name,
                    selected = selectedLevels.contains(level),
                    onClick = {
                        onLevelSelected(level)
                    }
                )
            }
        }

        CommonFilterDropdown(
            title = "Projects",
            isActive = projectOptions.any { it.selected },
            modifier = Modifier.weight(1f),
            interactionTag = VisualQaTags.FilterProjects,
        ) {
            projectOptions.forEach { option ->
                CommonRadioRow(
                    label = option.label,
                    selected = option.selected,
                    onClick = { onToggleProjects(option.id) }
                )
            }
        }

        CommonFilterDropdown(
            title = "Sort By",
            isActive = sortSelection != LogSort.NEWEST,
            modifier = Modifier.weight(1f),
            interactionTag = VisualQaTags.FilterSort,
        ) {
            CommonRadioRow(
                label = "Newest",
                selected = sortSelection == LogSort.NEWEST,
                onClick = { onSortSelected(LogSort.NEWEST) }
            )
            CommonRadioRow(
                label = "Oldest",
                selected = sortSelection == LogSort.OLDEST,
                onClick = { onSortSelected(LogSort.OLDEST) }
            )
            CommonRadioRow(
                label = "Level ↓",
                selected = sortSelection == LogSort.LEVEL_DESC,
                onClick = { onSortSelected(LogSort.LEVEL_DESC) }
            )
            CommonRadioRow(
                label = "Level ↑",
                selected = sortSelection == LogSort.LEVEL_ASC,
                onClick = { onSortSelected(LogSort.LEVEL_ASC) }
            )
        }
    }
}


@Composable
private fun LogListContent(
    logs: List<ErrorlogDTO>,
    projectNames: Map<Int, String>,
    showLoadMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    onLogClick: (ErrorlogDTO) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.roles.layout.screenPadding)
            .padding(
                top = AppTheme.roles.layout.contentGap,
                bottom = AppTheme.roles.layout.screenPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap)
    ) {
        items(items = logs, key = { it.id }) { log ->
            GlobalLogCard(
                log = LogCardInfo(
                    log.level,
                    log.timestamp,
                    log.message,
                    projectNames[log.project_id] ?: "Project #${log.project_id}",
                    log.errortype ?: "Unknown"
                ),
                onClick = { onLogClick(log) },
                modifier = Modifier.testTag(VisualQaTags.logCard(log.id)),
            )
        }
        if (showLoadMore) {
            item(key = "load_more") {
                LoadMoreRow(
                    loading = loadingMore,
                    onClick = onLoadMore
                )
            }
        }
    }
}
