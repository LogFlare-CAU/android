package com.example.logflare.core.designsystem.components.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

enum class LogItemType { Summary, Full }
enum class LogCardType { Single, Multiple }

@Composable
private fun logLevelColor(level: String): Color = when (level.uppercase()) {
    "FATAL", "ERROR" -> AppTheme.colors.red.pressed
    "WARN" -> Color(0xFFFFB74D)
    "INFO" -> Color(0xFF1976D2)
    "DEBUG" -> AppTheme.colors.primary.default
    else -> AppTheme.colors.neutral.s50
}

@Composable
fun LogLevelChip(
    level: String,
    modifier: Modifier = Modifier
) {
    val background = logLevelColor(level)
    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.size(width = 44.dp, height = 20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.uppercase(),
                style = AppTheme.typography.captionSmMedium,
                color = AppTheme.colors.neutral.white
            )
        }
    }
}

@Composable
fun LogItem(
    level: String,
    message: String,
    timestamp: String,
    path: String,
    type: LogItemType,
    modifier: Modifier = Modifier
) {
    when (type) {
        LogItemType.Summary -> SummaryLogItem(level, message, path, modifier)
        LogItemType.Full -> FullLogItem(level, message, timestamp, path, modifier)
    }
}

@Composable
private fun SummaryLogItem(
    level: String,
    message: String,
    path: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogLevelChip(level = level)
        Spacer(Modifier.width(AppTheme.spacing.s2))
        Text(
            text = path,
            style = AppTheme.typography.bodySmLight,
            color = AppTheme.colors.neutral.s60
        )
        Spacer(Modifier.width(AppTheme.spacing.s1))
        Text(
            text = message,
            style = AppTheme.typography.bodySmMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.neutral.black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FullLogItem(
    level: String,
    message: String,
    timestamp: String,
    path: String,
    modifier: Modifier = Modifier
) {
    val (project, file) = parsePath(path)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s1)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogLevelChip(level = level)
            Spacer(Modifier.width(AppTheme.spacing.s2))
            Text(
                text = timestamp,
                style = AppTheme.typography.bodySmLight,
                color = AppTheme.colors.neutral.s60
            )
        }
        Text(
            text = message,
            style = AppTheme.typography.bodyMdBold,
            color = AppTheme.colors.neutral.black
        )
        if (file != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project,
                    style = AppTheme.typography.bodySmLight,
                    color = AppTheme.colors.neutral.s60
                )
                Spacer(Modifier.width(AppTheme.spacing.s1))
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .width(1.dp)
                        .background(AppTheme.colors.neutral.s40)
                )
                Spacer(Modifier.width(AppTheme.spacing.s1))
                Text(
                    text = file,
                    style = AppTheme.typography.bodySmLight,
                    color = AppTheme.colors.neutral.s60
                )
            }
        } else {
            Text(
                text = project,
                style = AppTheme.typography.bodySmLight,
                color = AppTheme.colors.neutral.s60
            )
        }
    }
}

@Composable
fun LogCard(
    type: LogCardType,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = when (type) {
        LogCardType.Single -> AppTheme.spacing.s4
        LogCardType.Multiple -> AppTheme.spacing.s6
    }
    val arrangement = when (type) {
        LogCardType.Single -> Arrangement.Top
        LogCardType.Multiple -> Arrangement.spacedBy(AppTheme.spacing.s3)
    }

    Surface(
        color = AppTheme.colors.neutral.s20,
        shape = AppTheme.radius.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = arrangement,
            content = content
        )
    }
}

private fun parsePath(path: String): Pair<String, String?> {
    val parts = path.split('/', limit = 2).map { it.trim() }.filter { it.isNotEmpty() }
    return when (parts.size) {
        0 -> "" to null
        1 -> parts.first() to null
        else -> parts[0] to parts[1]
    }
}
