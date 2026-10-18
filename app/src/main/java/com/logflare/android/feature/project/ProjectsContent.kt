package com.logflare.android.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfIcons
import com.example.logflare.core.designsystem.v2.components.Avatar
import com.example.logflare.core.designsystem.v2.components.CountBadge
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.HeaderIconButton
import com.example.logflare.core.designsystem.v2.components.HeaderPillButton
import com.example.logflare.core.designsystem.v2.components.LargeTitleHeader
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.ListToolbar
import com.example.logflare.core.designsystem.v2.components.LfPullToRefresh
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.SearchField
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.format.relativeTime
import java.time.Instant

/** The Projects tab. */
@Composable
fun ProjectsRoute(
    onOpenProject: (id: Int) -> Unit,
    onCreateProject: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val refreshFailed = stringResource(R.string.projects_refresh_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProjectsEvent.RefreshFailed -> toast.show(refreshFailed)
            }
        }
    }
    // Relative times ("3 min ago") are re-anchored whenever the data changes.
    val now = remember(state.items) { Instant.now() }
    ProjectsContent(
        state = state,
        now = now,
        onToggleSearch = viewModel::toggleSearch,
        onSearch = viewModel::onSearch,
        onOpenSort = viewModel::openSortSheet,
        onCloseSort = viewModel::closeSortSheet,
        onSelectSort = viewModel::setSort,
        onRefresh = viewModel::refresh,
        onOpenProject = onOpenProject,
        onCreateProject = onCreateProject,
        toast = toast,
    )
}

@Composable
fun ProjectsContent(
    state: ProjectsUiState,
    now: Instant,
    onToggleSearch: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenSort: () -> Unit,
    onCloseSort: () -> Unit,
    onSelectSort: (ProjectSort) -> Unit,
    onRefresh: () -> Unit,
    onOpenProject: (id: Int) -> Unit,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.Projects)) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleHeader(stringResource(R.string.projects_title)) {
                HeaderIconButton(
                    Icons.Outlined.Search,
                    stringResource(if (state.searchOpen) R.string.projects_search_close else R.string.projects_search_open),
                    onClick = onToggleSearch,
                    modifier = if (state.searchOpen) {
                        Modifier.clip(CircleShape).background(Lf.colors.secondaryAction).semantics { selected = true }
                    } else {
                        Modifier
                    },
                )
                if (state.canCreate) {
                    HeaderPillButton(
                        text = stringResource(R.string.projects_new),
                        onClick = onCreateProject,
                        icon = LfIcons.Add,
                        contentDescription = stringResource(R.string.projects_new_description),
                        modifier = Modifier.testTag(VisualQaTags.CreateProject),
                    )
                }
            }
            if (state.searchOpen) SearchBar(state.search, onSearch)
            if (!state.initialLoad && !state.loadFailed && state.total > 0) {
                CountAndSort(state, onOpenSort)
            }
            ProjectList(state, now, onRefresh, onSearch, onOpenProject, onCreateProject, Modifier.weight(1f))
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }

    if (state.sortSheetOpen) {
        SelectionSheet(
            title = stringResource(R.string.projects_sort_title),
            options = ProjectSort.entries.map { SheetOption(it, sortLabel(it)) },
            selected = state.sort,
            onSelect = onSelectSort,
            onDismiss = onCloseSort,
        )
    }
}

@Composable
private fun SearchBar(text: String, onSearch: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    SearchField(
        value = text,
        onValueChange = onSearch,
        description = stringResource(R.string.projects_search_field),
        placeholder = stringResource(R.string.projects_search_placeholder),
        containerColor = Lf.colors.surface,
        onClear = { onSearch("") },
        clearDescription = stringResource(R.string.projects_search_clear),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).focusRequester(focus),
    )
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun CountAndSort(state: ProjectsUiState, onOpenSort: () -> Unit) {
    ListToolbar(
        label = if (state.search.isNotBlank()) {
            stringResource(R.string.projects_count_filtered, state.items.size, state.total)
        } else {
            pluralStringResource(R.plurals.projects_count, state.total, state.total)
        },
        actionText = sortLabel(state.sort),
        onAction = onOpenSort,
        actionIcon = LfIcons.SwapVert,
    )
}

@Composable
private fun sortLabel(sort: ProjectSort): String = stringResource(
    when (sort) {
        ProjectSort.RecentErrors -> R.string.projects_sort_recent
        ProjectSort.NameAsc -> R.string.projects_sort_name
        ProjectSort.MostErrors -> R.string.projects_sort_errors
    },
)

@Composable
private fun ProjectList(
    state: ProjectsUiState,
    now: Instant,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenProject: (Int) -> Unit,
    onCreateProject: () -> Unit,
    modifier: Modifier,
) {
    LfPullToRefresh(refreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            when {
                state.initialLoad -> item { SkeletonCard(rows = 5) }
                state.loadFailed -> item {
                    EmptyState(
                        title = stringResource(R.string.projects_load_failed_title),
                        body = stringResource(R.string.projects_load_failed_body),
                        actionLabel = stringResource(R.string.projects_retry),
                        onAction = onRefresh,
                    )
                }
                state.total == 0 -> item {
                    EmptyState(
                        title = stringResource(R.string.projects_empty_title),
                        body = stringResource(R.string.projects_empty_body),
                        actionLabel = if (state.canCreate) stringResource(R.string.projects_empty_action) else null,
                        onAction = if (state.canCreate) onCreateProject else null,
                        modifier = Modifier.testTag(VisualQaTags.Empty),
                    )
                }
                state.items.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.projects_no_match_title),
                        body = stringResource(R.string.projects_no_match_body, state.search.trim()),
                        actionLabel = stringResource(R.string.projects_clear_search),
                        onAction = { onSearch("") },
                    )
                }
                else -> item {
                    GroupedCard {
                        state.items.forEach { project ->
                            ProjectRow(project, now) { onOpenProject(project.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectItem, now: Instant, onClick: () -> Unit) {
    val errorsToday = pluralStringResource(R.plurals.projects_errors_today, project.errorCount24h, project.errorCount24h)
    ListRow(
        title = project.name,
        subtitle = project.lastErrorAt?.let { stringResource(R.string.projects_last_error, relativeTime(it, now)) }
            ?: stringResource(R.string.projects_no_errors),
        leading = { Avatar(project.name, size = 44.dp, strong = true) },
        trailing = if (project.errorCount24h > 0) {
            { CountBadge(project.errorCount24h, StatTone.Error, Modifier.semantics { contentDescription = errorsToday }) }
        } else {
            null
        },
        showChevron = true,
        onClick = onClick,
        modifier = Modifier.testTag(VisualQaTags.projectCard(project.id)),
    )
}
