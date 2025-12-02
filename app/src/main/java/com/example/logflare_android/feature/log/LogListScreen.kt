package com.example.logflare_android.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.model.ErrorlogDTO
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val StatusBarGray = Color(0xFFF5F5F5)
private val CardGray = Color(0xFFEDEDED)
private val FatalRed = Color(0xFFB12B38)
private val InfoGray = Color(0xFF616161)
private val PrimaryText = Color(0xFF1A1A1A)
private val SecondaryText = Color(0xFF353535)
private val AccentGreen = Color(0xFF61B075)
private val OutlineGray = Color(0xFFBDBDBD)

enum class LogSort { NEWEST, OLDEST, LEVEL_DESC, LEVEL_ASC }

data class ToggleOption(
    val key: String,
    val label: String,
    val selected: Boolean
)

@Composable
fun LogListScreen(
    projectId: Int?,
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    val sortSelection = remember { mutableStateOf(LogSort.NEWEST) }
    val showOnlyOptions = remember {
        mutableStateListOf(
            ToggleOption("timestamp", "Timestamp", true),
            ToggleOption("level", "Log Level", true),
            ToggleOption("source", "Source", true),
            ToggleOption("message", "Message", true)
        )
    }

    LaunchedEffect(projectId) {
        if (projectId == null) {
            viewModel.refreshAllProjects()
        } else {
            viewModel.refresh(projectId)
        }
    }

    val displayedLogs = remember(uiState.items, sortSelection.value) {
        when (sortSelection.value) {
            LogSort.NEWEST -> uiState.items.sortedByDescending { it.parseEpochMillis() }
            LogSort.OLDEST -> uiState.items.sortedBy { it.parseEpochMillis() }
            LogSort.LEVEL_DESC -> uiState.items.sortedByDescending { severityRank(it.level) }
            LogSort.LEVEL_ASC -> uiState.items.sortedBy { severityRank(it.level) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        FakeStatusBar()
        LogHeader()
        FilterDropdownRow(
            selectedLevel = uiState.filter,
            onLevelSelected = viewModel::setFilter,
            showOnlyOptions = showOnlyOptions,
            onToggleShowOnly = { key ->
                val index = showOnlyOptions.indexOfFirst { it.key == key }
                if (index >= 0) {
                    val current = showOnlyOptions[index]
                    showOnlyOptions[index] = current.copy(selected = !current.selected)
                }
            },
            sortSelection = sortSelection.value,
            onSortSelected = { selection -> sortSelection.value = selection }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            when {
                uiState.loading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!)
                displayedLogs.isEmpty() -> EmptyState(projectId, uiState.filter)
                else -> LogListContent(
                    logs = displayedLogs,
                    projectNames = uiState.projectNames
                )
            }
        }
    }
}

@Composable
private fun FakeStatusBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StatusBarGray)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterStart),
            color = SecondaryText
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(8.dp)
                .width(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SecondaryText.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun LogHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LOG",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryText
        )
    }
}

@Composable
private fun FilterDropdownRow(
    selectedLevel: LogLevel?,
    onLevelSelected: (LogLevel?) -> Unit,
    showOnlyOptions: List<ToggleOption>,
    onToggleShowOnly: (String) -> Unit,
    sortSelection: LogSort,
    onSortSelected: (LogSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterDropdown(
            title = "Log Level",
            isActive = selectedLevel != null,
            modifier = Modifier.weight(1f)
        ) {
            LogLevel.entries.forEach { level ->
                CheckRow(
                    label = level.name,
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(if (selectedLevel == level) null else level) }
                )
            }
        }

        FilterDropdown(
            title = "Show only",
            isActive = showOnlyOptions.any { it.selected },
            modifier = Modifier.weight(1f)
        ) {
            showOnlyOptions.forEach { option ->
                CheckRow(
                    label = option.label,
                    selected = option.selected,
                    onClick = { onToggleShowOnly(option.key) },
                    highlightText = option.selected
                )
            }
        }

        FilterDropdown(
            title = "Sort By",
            isActive = sortSelection != LogSort.NEWEST,
            modifier = Modifier.weight(1f)
        ) {
            SortRow(
                label = "Newest",
                selected = sortSelection == LogSort.NEWEST,
                onClick = { onSortSelected(LogSort.NEWEST) }
            )
            SortRow(
                label = "Oldest",
                selected = sortSelection == LogSort.OLDEST,
                onClick = { onSortSelected(LogSort.OLDEST) }
            )
            SortRow(
                label = "Level ↓",
                selected = sortSelection == LogSort.LEVEL_DESC,
                onClick = { onSortSelected(LogSort.LEVEL_DESC) }
            )
            SortRow(
                label = "Level ↑",
                selected = sortSelection == LogSort.LEVEL_ASC,
                onClick = { onSortSelected(LogSort.LEVEL_ASC) }
            )
        }
    }
}

@Composable
private fun FilterDropdown(
    title: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(width = 0.5.dp, color = OutlineGray, shape = RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive || expanded) AccentGreen else SecondaryText,
                    fontWeight = FontWeight.Medium
                )
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = AccentGreen
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(width = 0.5.dp, color = OutlineGray, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlightText: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(width = 0.6.dp, color = OutlineGray, shape = RoundedCornerShape(4.dp)),
            color = if (selected) AccentGreen else Color.Transparent
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected || highlightText) AccentGreen else SecondaryText,
                fontWeight = if (selected || highlightText) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun SortRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) AccentGreen else SecondaryText,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        )
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
private fun EmptyState(projectId: Int?, filter: LogLevel?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (projectId == null) "No logs available" else "No logs for this project",
                style = MaterialTheme.typography.bodyLarge
            )
            if (filter != null) {
                Text(
                    text = "Try adjusting the filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogListContent(
    logs: List<ErrorlogDTO>,
    projectNames: Map<Int, String>,
    modifier: Modifier = Modifier
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
                log = log,
                projectName = projectNames[log.project_id] ?: "Project #${log.project_id}",
                fileName = log.errortype ?: "Unknown"
            )
        }
    }
}

@Composable
private fun GlobalLogCard(
    log: ErrorlogDTO,
    projectName: String,
    fileName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level = log.level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = displayTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryText,
            lineHeight = 24.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
            Text(" / ", style = MaterialTheme.typography.bodySmall, color = InfoGray)
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
        }
    }
}

@Composable
private fun LevelBadge(level: String) {
    val badgeColor = when (level.uppercase()) {
        "FATAL" -> FatalRed
        "ERROR" -> Color(0xFFD84534)
        "WARN", "WARNING" -> Color(0xFFFFB74D)
        "INFO" -> Color(0xFF1976D2)
        "DEBUG" -> Color(0xFF388E3C)
        else -> SecondaryText
    }
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = level.uppercase(),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

private fun ErrorlogDTO.parseEpochMillis(): Long {
    return runCatching { Instant.parse(this.timestamp).toEpochMilli() }
        .getOrElse { Long.MIN_VALUE }
}

private fun severityRank(level: String?): Int {
    return when (level?.uppercase()) {
        "FATAL" -> 5
        "ERROR" -> 4
        "WARN", "WARNING" -> 3
        "INFO" -> 2
        "DEBUG" -> 1
        else -> 0
    }
}

private fun displayTimestamp(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown"
    return try {
        val instant = Instant.parse(value)
        instant.toString().replace('T', ' ').substringBeforeLast('.')
    } catch (e: DateTimeParseException) {
        value
    }
}
