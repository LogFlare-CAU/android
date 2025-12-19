package com.example.logflare_android.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.LogSort
import com.example.logflare_android.ui.common.*

data class ProjectToggleOption(
    val id: Int,
    val label: String,
    val selected: Boolean
)

@Composable
fun LogListScreen(
    onLogClick: () -> Unit,
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopTitle("Logs")
        FilterDropdownRow(
            selectedLevels = uiState.filter,
            onLevelSelected = viewModel::setFilter,
            projectOptions = uiState.projectOptions,
            onToggleProjects = { id -> viewModel.toggleProjectOption(id) },
            sortSelection = uiState.sortBy,
            onSortSelected = { selection -> viewModel.setSortBy(selection) }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            when {
                uiState.loading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!)
                uiState.errorLogs.isEmpty() -> EmptyState(uiState.selectedProject != null, uiState.filter)
                else -> LogListContent(
                    logs = uiState.errorLogs,
                    projectNames = uiState.projectNames,
                    showLoadMore = uiState.hasMore,
                    loadingMore = uiState.loadingMore,
                    onLoadMore = { viewModel.loadMore() },
                    onLogClick = { log -> viewModel.onLogClick(log); onLogClick() }
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
//            .padding(top = 12.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CommonFilterDropdown(
            title = "Log Level",
            isActive = selectedLevels.isNotEmpty(),
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
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
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
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
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                onClick = { onLogClick(log) }
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
