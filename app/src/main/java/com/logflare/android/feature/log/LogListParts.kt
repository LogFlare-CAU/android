package com.logflare.android.feature.log

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.CountBadge
import com.example.logflare.core.designsystem.v2.components.LevelIcon
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.logflare.android.R
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.format.appLocale
import com.logflare.android.ui.format.levelLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlinx.coroutines.flow.distinctUntilChanged

// Pieces of a grouped log list shared by the Logs tab and a project's page.

/** Loading the next page starts this many rows before the end of the list. */
private const val END_THRESHOLD = 5

/**
 * Asks for the next page when the list's end is on screen. Driven by layout, not by state: a page
 * that lands below the viewport changes the item count but not what is visible, so it never chains
 * into the next load by itself. A list that still ends on screen after a page lands asks again
 * (the pair changes).
 */
@Composable
internal fun EndReachedEffect(listState: LazyListState, hasMore: Boolean, onEndReached: () -> Unit) {
    val hasMoreNow = rememberUpdatedState(hasMore)
    val endReached = rememberUpdatedState(onEndReached)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (last, total) -> if (hasMoreNow.value && total > 0 && last >= total - END_THRESHOLD) endReached.value() }
    }
}

@Composable
internal fun levelChipLabel(level: LfLevel?): String = when (level) {
    null -> stringResource(R.string.logs_all_levels)
    LfLevel.Critical -> levelLabel(level)
    else -> stringResource(R.string.logs_level_and_up, levelLabel(level))
}

/**
 * The grouped rows of a log list: a count line, then per day a heading and a card of rows, and a
 * skeleton row while [hasMore]. [projectNames] (id to name) adds the project to each row's meta
 * line; null leaves it out (a list of one project needn't repeat its name).
 */
internal fun LazyListScope.logRows(
    groups: List<DayGroup>,
    count: Int,
    hasMore: Boolean,
    projectNames: Map<Int, String>?,
    zone: ZoneId,
    onOpenLog: (Int) -> Unit,
) {
    item(key = "count") { CountLine(count, hasMore) }
    groups.forEach { group ->
        item(key = "day:${group.items.first().id}") { DayHeader(group.label) }
        items(group.items, key = { "log:${it.id}" }) { log ->
            LogRow(
                log = log,
                projectName = projectNames?.get(log.projectId),
                zone = zone,
                first = log === group.items.first(),
                last = log === group.items.last(),
                onClick = { onOpenLog(log.id) },
            )
        }
    }
    if (hasMore) item(key = "more") { SkeletonCard(Modifier.padding(top = 8.dp), rows = 1) }
}

@Composable
private fun CountLine(count: Int, hasMore: Boolean) {
    Text(
        if (hasMore) {
            stringResource(R.string.logs_count_more, count)
        } else {
            pluralStringResource(R.plurals.logs_count, count, count)
        },
        style = Lf.type.caption.copy(fontWeight = FontWeight.SemiBold),
        color = Lf.colors.textMuted,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun DayHeader(label: DayLabel) {
    Text(
        dayLabel(label),
        style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
        color = Lf.colors.textMuted,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun dayLabel(label: DayLabel): String = when (label) {
    DayLabel.Today -> stringResource(R.string.logs_today)
    DayLabel.Yesterday -> stringResource(R.string.logs_yesterday)
    is DayLabel.Date -> {
        val locale = appLocale()
        val pattern = DateFormat.getBestDateTimePattern(locale, if (label.showYear) "MMMdyyyy" else "MMMd")
        label.date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Icon, message, "<project> · HH:mm" and a "×N" badge for repeated rows. A group's rows share one
 * card: [first] and [last] round its ends. The project name gives way (ellipsis) before the time.
 */
@Composable
private fun LogRow(
    log: LogEvent,
    projectName: String?,
    zone: ZoneId,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val corner = 24.dp
    val shape = RoundedCornerShape(
        topStart = if (first) corner else 0.dp,
        topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp,
        bottomEnd = if (last) corner else 0.dp,
    )
    val level = levelLabel(log.level)
    val time = log.lastSeen.atZone(zone).format(TIME)
    // Critical and Debug share their icon with Error and Info: name them.
    val tail = if (log.level == LfLevel.Critical || log.level == LfLevel.Debug) {
        stringResource(R.string.logs_dot_join, time, level)
    } else {
        time
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Lf.colors.surface)
            .padding(top = if (first) 6.dp else 0.dp, bottom = if (last) 6.dp else 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag(VisualQaTags.logCard(log.id))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { stateDescription = level }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LevelIcon(log.level)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    log.message,
                    style = Lf.type.bodyStrong,
                    color = Lf.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (projectName != null) {
                    ProjectThenTail(projectName, stringResource(R.string.logs_meta_after_project, tail))
                } else {
                    Text(tail, style = Lf.type.secondary, color = Lf.colors.textMuted, maxLines = 1)
                }
            }
            if (log.occurrences > 1) CountBadge(log.occurrences, StatTone.Neutral)
        }
    }
}

/**
 * "[project][tail]" on one line: the tail (" · 15:40") always shows in full and the project
 * ellipsizes. The tail follows the project's visible end, not its box: an ellipsized Text keeps
 * the full width offered, which would leave a gap before the tail.
 */
@Composable
private fun ProjectThenTail(project: String, tail: String) {
    val projectEnd = remember { FloatArray(1) }
    Layout(
        content = {
            Text(
                project,
                style = Lf.type.secondary,
                color = Lf.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { projectEnd[0] = it.getLineRight(0) },
            )
            Text(tail, style = Lf.type.secondary, color = Lf.colors.textMuted, maxLines = 1)
        },
    ) { (projectText, tailText), constraints ->
        val loose = constraints.copy(minWidth = 0)
        val tailPlaceable = tailText.measure(loose)
        val projectPlaceable = projectText.measure(
            loose.copy(maxWidth = (constraints.maxWidth - tailPlaceable.width).coerceAtLeast(0)),
        )
        // onTextLayout ran during that measure; it isn't called again when the layout is cached,
        // and then the stored end is still the current one.
        val projectWidth = ceil(projectEnd[0]).toInt().coerceIn(0, projectPlaceable.width)
        val height = maxOf(projectPlaceable.height, tailPlaceable.height)
        layout(projectWidth + tailPlaceable.width, height) {
            projectPlaceable.placeRelative(0, 0)
            tailPlaceable.placeRelative(projectWidth, 0)
        }
    }
}
