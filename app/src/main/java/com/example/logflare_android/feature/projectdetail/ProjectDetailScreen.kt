package com.example.logflare_android.feature.projectdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
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
                onOpenProjectSettings = onOpenProjectSettings
            )
        }
    }
}

@Composable
private fun ProjectDetailContent(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenProjectSettings: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
    BackHeader(title = uiState.projectName, onBack = onBack)
        ProjectSettingsCard(
            label = uiState.settingsLabel,
            onClick = { onOpenProjectSettings(uiState.projectId) }
        )
        FilterPanel(filterState = uiState.filterState)
        LogsSection(logs = uiState.logs)
        BottomSpacerBar()
    }
}

// Local ProjectHeader is now standardized to use BackHeader

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
        color = CardGray
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
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryText.copy(alpha = 0.86f)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun LogsSection(logs: List<ProjectDetailLog>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Logs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            logs.forEach { log ->
                ProjectLogCard(log)
            }
        }
    }
}

@Composable
private fun ProjectLogCard(log: ProjectDetailLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LogCardGray)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level = log.level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = log.timestamp,
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
                text = log.projectName,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
            Text(" / ", style = MaterialTheme.typography.bodySmall, color = InfoGray)
            Text(
                text = log.fileName,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
        }
    }
}

@Composable
private fun LevelBadge(level: ProjectLogLevel) {
    val badgeColor = when (level) {
        ProjectLogLevel.CIRITCAL -> FatalRed
        ProjectLogLevel.ERROR -> Color(0xFFD84534)
        ProjectLogLevel.WARNING -> Color(0xFFFFB74D)
        ProjectLogLevel.INFO -> Color(0xFF1976D2)
        ProjectLogLevel.DEBUG -> Color(0xFF388E3C)
//        ProjectLogLevel.TRACE -> Color(0xFF455A64)
    }
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = level.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
    }
}

@Composable
private fun FilterPanel(filterState: ProjectDetailFilterState) {
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
            FilterDropdown(
                title = "Log Level",
                modifier = Modifier.weight(1f)
            ) {
                ProjectLogLevel.values().forEach { level ->
                    CheckRow(
                        label = level.displayName,
                        selected = filterState.selectedLevel == level,
                        highlight = AccentGreen
                    )
                }
            }
            FilterDropdown(
                title = "Show only",
                modifier = Modifier.weight(1f)
            ) {
                filterState.showOnlyOptions.forEach { option ->
                    CheckRow(
                        label = option.label,
                        selected = option.selected,
                        highlight = AccentGreen,
                        fillWhenSelected = false
                    )
                }
            }
            FilterDropdown(
                title = "Sort By",
                modifier = Modifier.weight(1f)
            ) {
                filterState.sortOptions.forEach { option ->
                    CheckRow(
                        label = option.label,
                        selected = option.selected,
                        highlight = AccentGreen,
                        fillWhenSelected = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    title: String,
    modifier: Modifier = Modifier,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        FilterHeader(
            title = title,
            expanded = expanded,
            onClick = { expanded = !expanded }
        )
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(width = 0.5.dp, color = OutlineGray, shape = RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dropdownContent()
            }
        }
    }
}

@Composable
private fun FilterHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(width = 0.5.dp, color = OutlineGray, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = AccentGreen,
                fontWeight = FontWeight.Medium
            )
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = AccentGreen
        )
    }
}

@Composable
private fun CheckRow(
    label: String,
    selected: Boolean,
    highlight: Color,
    fillWhenSelected: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(width = 0.6.dp, color = OutlineGray, shape = RoundedCornerShape(4.dp))
                .background(if (selected && fillWhenSelected) highlight else Color.Transparent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected && fillWhenSelected) highlight else SecondaryText,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun BottomSpacerBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        HorizontalDivider(color = CardGray, thickness = 48.dp)
    }
}
