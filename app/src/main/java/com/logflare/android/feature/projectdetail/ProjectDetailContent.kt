package com.logflare.android.feature.projectdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.HeaderIconButton
import com.example.logflare.core.designsystem.v2.components.LfFilterChip
import com.example.logflare.core.designsystem.v2.components.LfPullToRefresh
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatItem
import com.example.logflare.core.designsystem.v2.components.StatStrip
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.feature.log.EndReachedEffect
import com.logflare.android.feature.log.LevelSheet
import com.logflare.android.feature.log.SortSheet
import com.logflare.android.feature.log.groupByDay
import com.logflare.android.feature.log.levelChipLabel
import com.logflare.android.feature.log.logRows
import com.logflare.android.feature.log.sortLabel
import com.logflare.android.ui.VisualQaTags
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProjectDetailRoute(
    onBack: () -> Unit,
    onOpenSettings: (projectId: Int) -> Unit,
    onOpenLog: (id: Int) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val refreshFailed = stringResource(R.string.logs_refresh_failed)
    val loadMoreFailed = stringResource(R.string.logs_load_more_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProjectDetailEvent.RefreshFailed -> toast.show(refreshFailed)
                ProjectDetailEvent.LoadMoreFailed -> toast.show(loadMoreFailed)
            }
        }
    }
    ProjectDetailContent(
        state = state,
        onBack = onBack,
        onOpenSettings = { state.project?.let { onOpenSettings(it.id) } },
        onToggleErrors = viewModel::toggleErrors,
        onToggleWarnings = viewModel::toggleWarnings,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onSelectFile = viewModel::setLogFile,
        onSelectLevel = viewModel::setMinLevel,
        onSelectSort = viewModel::setSort,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onEndReached = viewModel::onEndReached,
        onOpenLog = onOpenLog,
        toast = toast,
    )
}

@Composable
fun ProjectDetailContent(
    state: ProjectDetailUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleErrors: () -> Unit,
    onToggleWarnings: () -> Unit,
    onOpenSheet: (ProjectDetailSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onSelectFile: (Int?) -> Unit,
    onSelectLevel: (LfLevel?) -> Unit,
    onSelectSort: (LogSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onEndReached: () -> Unit,
    onOpenLog: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.ProjectDetail)) {
        Column(Modifier.fillMaxSize()) {
            SubpageTopBar(title = state.project?.name.orEmpty(), onNav = onBack) {
                if (state.project != null) {
                    HeaderIconButton(
                        Icons.Outlined.Settings,
                        stringResource(R.string.project_detail_settings),
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag(VisualQaTags.OpenProjectSettings),
                    )
                }
            }
            if (state.notFound) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    EmptyState(
                        title = stringResource(R.string.project_detail_not_found),
                        body = null,
                        actionLabel = stringResource(R.string.project_detail_back_to_projects),
                        onAction = onBack,
                    )
                }
            } else {
                Stats(state, onToggleErrors, onToggleWarnings, onOpenSheet)
                FilterChips(state, onOpenSheet)
                if (state.filtered) ClearFilters(onClearFilters)
                LogList(state, zone, today, onRefresh, onEndReached, onClearFilters, Modifier.weight(1f), onOpenLog)
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }

    val project = state.project
    when (state.sheet) {
        ProjectDetailSheet.File -> SelectionSheet(
            title = stringResource(R.string.project_detail_sheet_file),
            options = listOf(SheetOption<Int?>(null, stringResource(R.string.project_detail_all_files))) +
                project?.logFiles.orEmpty().map { SheetOption<Int?>(it.id, it.name) },
            selected = state.query.logFileId,
            onSelect = onSelectFile,
            onDismiss = onCloseSheet,
        )
        ProjectDetailSheet.Level -> LevelSheet(state.query.minLevel, onSelectLevel, onCloseSheet)
        ProjectDetailSheet.Sort -> SortSheet(state.query.sort, onSelectSort, onCloseSheet)
        null -> Unit
    }
}

@Composable
private fun Stats(
    state: ProjectDetailUiState,
    onToggleErrors: () -> Unit,
    onToggleWarnings: () -> Unit,
    onOpenSheet: (ProjectDetailSheet) -> Unit,
) {
    val unknown = stringResource(R.string.project_detail_stat_unknown)
    val level = state.query.minLevel
    StatStrip(
        listOf(
            StatItem(
                value = state.summary?.errors24h?.toString() ?: unknown,
                label = stringResource(R.string.project_detail_stat_errors),
                tone = StatTone.Error,
                onClick = onToggleErrors,
                selected = level == LfLevel.Error,
            ),
            StatItem(
                value = state.summary?.warnings24h?.toString() ?: unknown,
                label = stringResource(R.string.project_detail_stat_warnings),
                tone = StatTone.Warning,
                onClick = onToggleWarnings,
                selected = level == LfLevel.Warning,
            ),
            StatItem(
                value = state.project?.logFiles?.size?.toString() ?: unknown,
                label = stringResource(R.string.project_detail_stat_files),
                onClick = { onOpenSheet(ProjectDetailSheet.File) },
                selected = state.query.logFileId != null,
            ),
        ),
        Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
    )
}

@Composable
private fun FilterChips(state: ProjectDetailUiState, onOpenSheet: (ProjectDetailSheet) -> Unit) {
    val q = state.query
    val fileLabel = q.logFileId?.let { id ->
        state.project?.logFiles?.firstOrNull { it.id == id }?.name ?: stringResource(R.string.project_detail_file_fallback)
    } ?: stringResource(R.string.project_detail_all_files)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LfFilterChip(fileLabel, q.logFileId != null, { onOpenSheet(ProjectDetailSheet.File) })
        LfFilterChip(levelChipLabel(q.minLevel), q.minLevel != null, { onOpenSheet(ProjectDetailSheet.Level) })
        LfFilterChip(sortLabel(q.sort), q.sort != LogSortOrder.Newest, { onOpenSheet(ProjectDetailSheet.Sort) })
    }
}

@Composable
private fun ClearFilters(onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onClear)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.logs_clear_filters),
                style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                color = Lf.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun LogList(
    state: ProjectDetailUiState,
    zone: ZoneId,
    today: LocalDate,
    onRefresh: () -> Unit,
    onEndReached: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier,
    onOpenLog: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    EndReachedEffect(listState, state.hasMore, onEndReached)
    // A new query starts from the top (not a recreated screen: it keeps its scroll position).
    val shownQuery = remember { mutableStateOf(state.query) }
    LaunchedEffect(state.query) {
        if (shownQuery.value != state.query) {
            shownQuery.value = state.query
            listState.scrollToItem(0)
        }
    }
    val groups = remember(state.items, today) { groupByDay(state.items, today, zone) }

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
                state.items.isEmpty() && state.filtered -> item {
                    EmptyState(
                        title = stringResource(R.string.project_detail_empty_filtered_title),
                        body = stringResource(R.string.project_detail_empty_filtered_body),
                        actionLabel = stringResource(R.string.logs_clear_filters),
                        onAction = onClearFilters,
                    )
                }
                state.items.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.project_detail_empty_title),
                        body = stringResource(R.string.project_detail_empty_body),
                    )
                }
                else -> logRows(groups, state.items.size, state.hasMore, null, zone, onOpenLog)
            }
        }
    }
}
