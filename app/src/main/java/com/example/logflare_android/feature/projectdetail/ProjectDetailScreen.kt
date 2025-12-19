package com.example.logflare_android.feature.projectdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.LogSort
import com.example.logflare_android.ui.common.*
import java.nio.channels.Selector
import com.example.logflare_android.ui.components.BackHeader

private val CardGray = Color(0xFFEEEEEE)
private val LogCardGray = Color(0xFFEDEDED)
private val FatalRed = Color(0xFFB12B38)
private val InfoGray = Color(0xFF616161)
private val PrimaryText = Color(0xFF1A1A1A)
private val SecondaryText = Color(0xFF353535)
private val AccentGreen = Color(0xFF61B075)
private val OutlineGray = Color(0xFFBDBDBD)

@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
    color = AppTheme.colors.neutral.white
    ) {
        when {
            uiState.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> ProjectDetailContent(
                uiState = uiState,
                onBack = onBack,
                onOpenProjectSettings = onOpenProjectSettings,
                onLevelSelected = { level -> viewModel.onLevelSelected(level) },
                onLogfileSelected = { id -> viewModel.onLogfileSelected(id) },
                onSortSelected = { sort -> viewModel.onSortSelected(sort) },
                onLogClick = { log -> viewModel.onLogClick(log); onLogClick() }
            )
        }
    }
}

@Composable
private fun ProjectDetailContent(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit,
    onLevelSelected: (LogLevel) -> Unit,
    onLogfileSelected: (Int) -> Unit,
    onSortSelected: (LogSort) -> Unit,
    onLogClick: (ProjectDetailLog) -> Unit = { }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.neutral.white)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader(title = uiState.projectName, onBack = onBack)
        ProjectSettingsCard(
            label = uiState.settingsLabel,
            onClick = { onOpenProjectSettings(uiState.projectId) }
        )
        FilterPanel(
            filterState = uiState.filterState,
            onLevelSelected = onLevelSelected,
            onLogfileSelected = onLogfileSelected,
            onSortSelected = onSortSelected
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            when {
                uiState.logs.isEmpty() -> EmptyState(uiState.projectId > 0, uiState.filterState.selectedLevel)
                else -> LogsSection(
                    logs = uiState.logs,
                    showMore = uiState.showMoreState,
                    onLogClick = onLogClick
                )
            }
        }
    }
}

@Composable
private fun ProjectSettingsCard(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    color = AppTheme.colors.neutral.s20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTheme.typography.bodyMdMedium,
                color = AppTheme.colors.neutral.black.copy(alpha = 0.86f)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppTheme.colors.neutral.white.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun LogsSection(
    logs: List<ProjectDetailLog>,
    showMore: ShowMoreState,
    onLogClick: (ProjectDetailLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                onClick = { onLogClick(log) }
            )
        }
        if (showMore.hasMore) {
            item(key = "load_more") {
                LoadMoreRow(
                    loading = showMore.loading,
                    onClick = showMore.onClick
                )
            }
        }
    }
}

@Composable
private fun LevelBadge(level: LogLevel) {
    val badgeColor = when (level) {
        LogLevel.CRITICAL -> FatalRed
        LogLevel.ERROR -> Color(0xFFD84534)
        LogLevel.WARNING -> Color(0xFFFFB74D)
        LogLevel.INFO -> Color(0xFF1976D2)
        LogLevel.DEBUG -> Color(0xFF388E3C)
    }
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = level.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = AppTheme.typography.captionSmMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
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
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Log Level 필터
            CommonFilterDropdown(
                title = "Log Level",
                isActive = filterState.selectedLevel.isNotEmpty(),
                modifier = Modifier.weight(1f)
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

            // Log File 필터
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
            // Sort By 필터
            CommonFilterDropdown(
                title = "Sort By",
                isActive = filterState.sortBy != LogSort.NEWEST,
                modifier = Modifier.weight(1f)
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
