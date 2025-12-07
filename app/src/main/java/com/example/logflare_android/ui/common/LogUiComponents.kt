package com.example.logflare_android.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.logflare_android.enums.LogLevel
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

val StatusBarGray = Color(0xFFF5F5F5)
val CardGray = Color(0xFFEDEDED)
val InfoGray = Color(0xFF616161)
val PrimaryText = Color(0xFF1A1A1A)
val SecondaryText = Color(0xFF353535)
val AccentGreen = Color(0xFF61B075)
val OutlineGray = Color(0xFFBDBDBD)

@Composable
fun CommonFilterDropdown(
    title: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier.Companion,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        // 버튼 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    buttonWidth = coords.size.width
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(0.5.dp, OutlineGray, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
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
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 48.dp.toPx().toInt() }),
                onDismissRequest = { expanded = false }
            ) {
                Column(
                    modifier = Modifier
                        .width(with(density) { buttonWidth.toDp() })
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(0.5.dp, OutlineGray, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun CommonCheckRow(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier.Companion,
    highlightColor: Color = AccentGreen,
    fillWhenSelected: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .border(
                    width = 0.6.dp,
                    color = OutlineGray,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ),
            color = if (selected && fillWhenSelected) highlightColor else Color.Transparent
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) highlightColor else SecondaryText,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

@Composable
fun CommonRadioRow(
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
fun CommonLevelBadge(level: String) {
    val enum = LogLevel.fromCodeByLabel(level)
    Surface(
        color = enum.color,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Text(
            text = enum.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
    }
}

@Composable
fun CommonLevelBadge(level: LogLevel) {
    Surface(
        color = level.color,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Text(
            text = level.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
    }
}

@Composable
fun LoadMoreRow(
    loading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(0.5.dp, OutlineGray, RoundedCornerShape(999.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading more...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Load more",
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Load more logs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Serializable
data class LogCardInfo(
    val level: String,
    val timestamp: String,
    val message: String,
    val prefix: String,
    val suffix: String,
)

/**
 * 로그 카드 컴포저블
 * @param prefix: 프로젝트 이름
 * @param suffix:파일 이름
 * 예를 들어 prefix / suffix 가 "Project Alpha" / "app.log" 라면
 * "Project Alpha / app.log" 형태로 표시됩니다.
 */
@Composable
fun GlobalLogCard(
    log: LogCardInfo,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .padding(24.dp)
            .clickable {
                onClick()
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommonLevelBadge(level = log.level)
            Spacer(Modifier.width(8.dp))
            Text(
                text = displayTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = cropLongText(log.message),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = PrimaryText,
            lineHeight = 24.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = log.prefix,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
            Text(" / ", style = MaterialTheme.typography.bodySmall, color = InfoGray)
            Text(
                text = log.suffix,
                style = MaterialTheme.typography.bodySmall,
                color = InfoGray
            )
        }
    }
}

private fun displayTimestamp(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown"
    return try {
        val instant = Instant.parse(value)
        instant
            .toString()
            .replace('T', ' ')
            .substringBeforeLast('.')
    } catch (_: DateTimeParseException) {
        value
    }
}


@Composable
fun EmptyState(projectFiltered: Boolean = false, filter: List<LogLevel> = emptyList()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (projectFiltered) "No logs available" else "No logs for this Project / LogFile",
                style = MaterialTheme.typography.bodyLarge
            )
            if (filter.isNotEmpty()) {
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
fun TopTitle(
    title: String,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryText,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun cropLongText(text: String, maxLength: Int = 100): String {
    return if (text.length <= maxLength) {
        text
    } else {
        text.take(maxLength) + "..."
    }
}