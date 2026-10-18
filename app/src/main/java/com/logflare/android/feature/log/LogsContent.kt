package com.logflare.android.feature.log

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.CountBadge
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.HeaderIconButton
import com.example.logflare.core.designsystem.v2.components.LargeTitleHeader
import com.example.logflare.core.designsystem.v2.components.LevelIcon
import com.example.logflare.core.designsystem.v2.components.LfFilterChip
import com.example.logflare.core.designsystem.v2.components.LfPullToRefresh
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.SearchField
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.format.levelLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The Logs tab. [entryState] is the Logs back-stack entry's SavedStateHandle, where Home's stat
 * taps leave a level preset (see `openLogs`); the ViewModel applies it while this is shown.
 */
@Composable
fun LogsRoute(
    entryState: SavedStateHandle,
    onOpenLog: (id: Int) -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val refreshFailed = stringResource(R.string.logs_refresh_failed)
    val loadMoreFailed = stringResource(R.string.logs_load_more_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                LogsEvent.RefreshFailed -> toast.show(refreshFailed)
                LogsEvent.LoadMoreFailed -> toast.show(loadMoreFailed)
            }
        }
    }
    LaunchedEffect(viewModel, entryState) { viewModel.collectPresets(entryState) }
    LogsContent(
        state = state,
        onToggleSearch = viewModel::toggleSearch,
        onSearch = viewModel::onSearch,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onSelectProject = viewModel::setProject,
        onSelectLevel = viewModel::setMinLevel,
        onSelectSort = viewModel::setSort,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onEndReached = viewModel::onEndReached,
        onOpenLog = onOpenLog,
        toast = toast,
    )
}

/** Loading the next page starts this many rows before the end of the list. */
private const val END_THRESHOLD = 5

/** Longer project names are cut in the chip so the other chips stay in reach. */
private const val CHIP_NAME_MAX = 24

@Composable
fun LogsContent(
    state: LogsUiState,
    onToggleSearch: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenSheet: (LogsSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onSelectProject: (Int?) -> Unit,
    onSelectLevel: (LfLevel?) -> Unit,
    onSelectSort: (LogSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onEndReached: () -> Unit,
    onOpenLog: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.Logs)) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleHeader(stringResource(R.string.logs_title)) {
                HeaderIconButton(
                    Icons.Outlined.Search,
                    stringResource(if (state.searchOpen) R.string.logs_search_close else R.string.logs_search_open),
                    onClick = onToggleSearch,
                    modifier = if (state.searchOpen) {
                        Modifier.clip(CircleShape).background(Lf.colors.secondaryAction).semantics { selected = true }
                    } else {
                        Modifier
                    },
                )
            }
            if (state.searchOpen) SearchBar(state.search, onSearch)
            FilterChips(state, onOpenSheet)
            LogList(state, zone, onRefresh, onEndReached, onClearFilters, onOpenLog, Modifier.weight(1f))
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }

    state.sheet?.let { sheet ->
        LogFilterSheet(
            sheet = sheet,
            query = state.query,
            projects = state.projects,
            onSelectProject = onSelectProject,
            onSelectLevel = onSelectLevel,
            onSelectSort = onSelectSort,
            onDismiss = onCloseSheet,
        )
    }
}

@Composable
private fun SearchBar(text: String, onSearch: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    SearchField(
        value = text,
        onValueChange = onSearch,
        description = stringResource(R.string.logs_search_field),
        placeholder = stringResource(R.string.logs_search_placeholder),
        containerColor = Lf.colors.surface,
        onClear = { onSearch("") },
        clearDescription = stringResource(R.string.logs_search_clear),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).focusRequester(focus),
    )
    // Opening search focuses it; a restored open field (recreation) gets focus too, harmlessly.
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun FilterChips(state: LogsUiState, onOpenSheet: (LogsSheet) -> Unit) {
    val q = state.query
    val projectName = q.projectId?.let { id ->
        state.projects.firstOrNull { it.id == id }?.name ?: stringResource(R.string.logs_project_fallback)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LfFilterChip(
            label = projectName?.let(::shortName) ?: stringResource(R.string.logs_all_projects),
            active = q.projectId != null,
            onClick = { onOpenSheet(LogsSheet.Project) },
            modifier = Modifier.testTag(VisualQaTags.FilterProjects),
        )
        LfFilterChip(
            label = levelChipLabel(q.minLevel),
            active = q.minLevel != null,
            onClick = { onOpenSheet(LogsSheet.Level) },
            modifier = Modifier.testTag(VisualQaTags.FilterLogLevel),
        )
        LfFilterChip(
            label = sortLabel(q.sort),
            active = q.sort != LogSortOrder.Newest,
            onClick = { onOpenSheet(LogsSheet.Sort) },
            modifier = Modifier.testTag(VisualQaTags.FilterSort),
        )
    }
}

private fun shortName(name: String): String =
    if (name.length <= CHIP_NAME_MAX) name else name.take(CHIP_NAME_MAX - 1).trimEnd() + "…"

@Composable
private fun levelChipLabel(level: LfLevel?): String = when (level) {
    null -> stringResource(R.string.logs_all_levels)
    LfLevel.Critical -> levelLabel(level)
    else -> stringResource(R.string.logs_level_and_up, levelLabel(level))
}

@Composable
private fun LogList(
    state: LogsUiState,
    zone: ZoneId,
    onRefresh: () -> Unit,
    onEndReached: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenLog: (Int) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    // Driven by layout, not by state: a page that lands below the viewport changes the item
    // count but not what is visible, so it never chains into the next load by itself. A list that
    // still ends on screen after a page lands asks again (the pair changes).
    val hasMore by rememberUpdatedState(state.hasMore)
    val endReached by rememberUpdatedState(onEndReached)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (last, total) -> if (hasMore && total > 0 && last >= total - END_THRESHOLD) endReached() }
    }
    // A new query starts from the top (not a recreated screen: it keeps its scroll position).
    val shownQuery = remember { mutableStateOf(state.query) }
    LaunchedEffect(state.query) {
        if (shownQuery.value != state.query) {
            shownQuery.value = state.query
            listState.scrollToItem(0)
        }
    }

    val names = remember(state.projects) { state.projects.associate { it.id to it.name } }
    LfPullToRefresh(refreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            when {
                state.initialLoad -> item { SkeletonCard(rows = 6) }
                state.loadFailed -> item {
                    EmptyState(
                        title = stringResource(R.string.logs_load_failed_title),
                        body = stringResource(R.string.logs_load_failed_body),
                        actionLabel = stringResource(R.string.logs_retry),
                        onAction = onRefresh,
                    )
                }
                state.count == 0 && state.filtered -> item {
                    EmptyState(
                        title = stringResource(R.string.logs_empty_filtered_title),
                        body = stringResource(R.string.logs_empty_filtered_body),
                        actionLabel = stringResource(R.string.logs_clear_filters),
                        onAction = onClearFilters,
                    )
                }
                state.count == 0 -> item {
                    EmptyState(
                        title = stringResource(R.string.logs_empty_title),
                        body = stringResource(R.string.logs_empty_body),
                    )
                }
                else -> rows(state, names, zone, onOpenLog)
            }
        }
    }
}

private fun LazyListScope.rows(state: LogsUiState, names: Map<Int, String>, zone: ZoneId, onOpenLog: (Int) -> Unit) {
    item(key = "count") { CountLine(state.count, state.hasMore) }
    // Rows of the selected project needn't repeat its name.
    val showProject = state.query.projectId == null
    state.groups.forEach { group ->
        item(key = "day:${group.items.first().id}") { DayHeader(group.label) }
        items(group.items, key = { "log:${it.id}" }) { log ->
            LogRow(
                log = log,
                projectName = if (showProject) names[log.projectId] else null,
                zone = zone,
                first = log === group.items.first(),
                last = log === group.items.last(),
                onClick = { onOpenLog(log.id) },
            )
        }
    }
    if (state.hasMore) item(key = "more") { SkeletonCard(Modifier.padding(top = 8.dp), rows = 1) }
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
        val locale = Locale.getDefault()
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
