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
import com.example.logflare.core.designsystem.AppTheme
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

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
                .clip(AppTheme.radius.large)
                .background(AppTheme.colors.neutral.white)
                .border(0.5.dp, AppTheme.colors.neutral.s40, AppTheme.radius.large)
                .clickable { expanded = !expanded }
                .padding(horizontal = AppTheme.spacing.s3, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AppTheme.typography.captionSmMedium.copy(
                    color = if (isActive || expanded) AppTheme.colors.primary.default else AppTheme.colors.neutral.s80
                )
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = AppTheme.colors.primary.default
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
                        .clip(AppTheme.radius.large)
                        .background(AppTheme.colors.neutral.white)
                        .border(0.5.dp, AppTheme.colors.neutral.s40, AppTheme.radius.large)
                        .padding(horizontal = AppTheme.spacing.s3, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s1),
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
    highlightColor: Color = AppTheme.colors.primary.default,
    fillWhenSelected: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.spacing.s1, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .border(
                    width = 0.6.dp,
                    color = AppTheme.colors.neutral.s40,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ),
            color = if (selected && fillWhenSelected) highlightColor else Color.Transparent
        ) {}
        Spacer(Modifier.width(AppTheme.spacing.s2))
        Text(
            text = label,
            style = AppTheme.typography.captionSmMedium.copy(
                color = if (selected) highlightColor else AppTheme.colors.neutral.s80,
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
            .padding(horizontal = AppTheme.spacing.s1, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTheme.typography.captionSmMedium.copy(
                color = if (selected) AppTheme.colors.primary.default else AppTheme.colors.neutral.s80,
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
            .padding(top = AppTheme.spacing.s1)
            .clip(AppTheme.radius.full)
            .background(AppTheme.colors.neutral.white)
            .border(0.5.dp, AppTheme.colors.neutral.s40, AppTheme.radius.full)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppTheme.colors.primary.default
                )
                Text(
                    text = "Loading more...",
                    style = AppTheme.typography.bodySmMedium,
                    color = AppTheme.colors.neutral.s80
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
                    tint = AppTheme.colors.primary.default,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Load more logs",
                    style = AppTheme.typography.bodySmMedium.copy(
                        color = AppTheme.colors.primary.default,
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
            .clip(AppTheme.radius.large)
            .background(AppTheme.colors.neutral.s20)
            .padding(AppTheme.spacing.s6)
            .clickable {
                onClick()
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommonLevelBadge(level = log.level)
            Spacer(Modifier.width(AppTheme.spacing.s2))
            Text(
                text = displayTimestamp(log.timestamp),
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70
            )
        }
        Spacer(Modifier.height(AppTheme.spacing.s3))
        Text(
            text = cropLongText(log.message),
            style = AppTheme.typography.bodyLgBold,
            color = AppTheme.colors.neutral.black,
            lineHeight = 24.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(AppTheme.spacing.s3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = log.prefix,
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70
            )
            Text(
                " / ",
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70
            )
            Text(
                text = log.suffix,
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70
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
            .clip(AppTheme.radius.large)
            .background(AppTheme.colors.neutral.s20)
            .padding(AppTheme.spacing.s6),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
        ) {
            Text(
                text = if (projectFiltered) "No logs available" else "No logs for this Project / LogFile",
                style = AppTheme.typography.bodyLgBold,
                color = AppTheme.colors.neutral.black
            )
            if (filter.isNotEmpty()) {
                Text(
                    text = "Try adjusting the filters",
                    style = AppTheme.typography.bodyMdMedium,
                    color = AppTheme.colors.neutral.s60
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
                        contentDescription = "Back",
                        tint = AppTheme.colors.neutral.black
                    )
                }
            }
        }
        Text(
            text = title,
            style = AppTheme.typography.bodyLgBold,
            color = AppTheme.colors.neutral.black,
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