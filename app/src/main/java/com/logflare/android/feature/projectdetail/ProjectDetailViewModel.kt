package com.logflare.android.feature.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.Resource
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogPage
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProjectDetailSheet { File, Level, Sort }

data class ProjectDetailUiState(
    val project: ProjectItem? = null,
    /** Last-24h counts of this project; null until loaded. */
    val summary: HomeSummary? = null,
    val query: LogQuery = LogQuery(),
    val items: List<LogEvent> = emptyList(),
    val hasMore: Boolean = false,
    val initialLoad: Boolean = true,
    /** The server's project list has no such project (deleted, or no access). */
    val notFound: Boolean = false,
    /** Nothing cached and the first load failed: show the retry state, not "no logs". */
    val loadFailed: Boolean = false,
    val refreshing: Boolean = false,
    val sheet: ProjectDetailSheet? = null,
) {
    /** A file or level filter is applied (offers "Clear filters"); the sort order is not a filter. */
    val filtered: Boolean get() = query.minLevel != null || query.logFileId != null
}

sealed interface ProjectDetailEvent {
    data object RefreshFailed : ProjectDetailEvent
    data object LoadMoreFailed : ProjectDetailEvent
}

/**
 * One project's page: its name, last-24h counts and log files from [ProjectStore], and its events
 * from [LogStore] filtered by log file, minimum level and sort, with infinite scroll. The project
 * comes from the nav argument; the filters live in [savedState] so they survive process death (an
 * open sheet does not).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val logStore: LogStore,
    private val projectStore: ProjectStore,
) : ViewModel() {

    private val projectId: Int = checkNotNull(savedState[Destinations.ProjectIdArg]) { "project id missing" }

    private val filters = MutableStateFlow(readFilters())
    private val sheet = MutableStateFlow<ProjectDetailSheet?>(null)
    private val refreshing = MutableStateFlow(false)
    private var loadingMore = false

    private val _events = Channel<ProjectDetailEvent>(Channel.BUFFERED)
    val events: Flow<ProjectDetailEvent> = _events.receiveAsFlow()

    private val page: Flow<Pair<LogQuery, Resource<LogPage>>> =
        filters.map { it.toQuery() }.flatMapLatest { q -> logStore.events(q).map { q to it } }

    private val local = combine(sheet, refreshing) { sh, r -> sh to r }

    val state: StateFlow<ProjectDetailUiState> =
        combine(page, projectStore.projects(), projectStore.summary(projectId), local) { (q, logs), projects, summary, l ->
            val project = projects.data?.firstOrNull { it.id == projectId }
            val notFound = projects.data != null && project == null
            ProjectDetailUiState(
                project = project,
                summary = summary.data,
                query = q,
                items = logs.data?.items.orEmpty(),
                hasMore = logs.data?.hasMore == true,
                initialLoad = projects.isInitialLoad || (project != null && logs.isInitialLoad),
                notFound = notFound,
                loadFailed = (projects.data == null && projects.error != null) ||
                    (project != null && logs.data == null && logs.error != null),
                refreshing = l.second,
                sheet = l.first,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProjectDetailUiState(query = filters.value.toQuery()),
        )

    /** The Errors tile: Error and above, or back to all levels when that is already the filter. */
    fun toggleErrors() = toggleLevel(LfLevel.Error)

    /** The Warnings tile: Warning and above, or back to all levels. */
    fun toggleWarnings() = toggleLevel(LfLevel.Warning)

    private fun toggleLevel(level: LfLevel) = update { it.copy(minLevel = if (it.minLevel == level) null else level) }

    fun setMinLevel(level: LfLevel?) = update { it.copy(minLevel = level) }

    fun setLogFile(id: Int?) = update { it.copy(logFileId = id) }

    fun setSort(order: LogSortOrder) = update { it.copy(sort = order) }

    /** Drops the file and level filters; the sort order stays. */
    fun clearFilters() = update { it.copy(minLevel = null, logFileId = null) }

    fun openSheet(target: ProjectDetailSheet) {
        sheet.value = target
    }

    fun closeSheet() {
        sheet.value = null
    }

    /** Pull-to-refresh, awaited: the logs of the current query, this project's counts and the project list. */
    fun refresh() {
        if (refreshing.value) return
        val q = state.value.query
        refreshing.value = true
        viewModelScope.launch {
            val results = listOf(
                async { logStore.refresh(q) },
                async { projectStore.refresh(projectId) },
                async { projectStore.refresh() },
            ).awaitAll()
            refreshing.value = false
            if (results.any { it.isFailure }) _events.send(ProjectDetailEvent.RefreshFailed)
        }
    }

    /** The list scrolled near its end: loads the next page unless one is loading or none is left. */
    fun onEndReached() {
        val s = state.value
        if (loadingMore || !s.hasMore || s.initialLoad) return
        loadingMore = true
        viewModelScope.launch {
            val result = logStore.loadMore(s.query)
            loadingMore = false
            if (result.isFailure) _events.send(ProjectDetailEvent.LoadMoreFailed)
        }
    }

    private fun update(change: (Filters) -> Filters) {
        val next = change(filters.value)
        filters.value = next
        savedState[KEY_LEVEL] = next.minLevel?.name
        savedState[KEY_FILE] = next.logFileId
        savedState[KEY_SORT] = next.sort.name
    }

    private fun readFilters() = Filters(
        minLevel = savedState.get<String>(KEY_LEVEL)?.let(LfLevel::parse),
        logFileId = savedState[KEY_FILE],
        sort = savedState.get<String>(KEY_SORT)?.let { runCatching { LogSortOrder.valueOf(it) }.getOrNull() }
            ?: LogSortOrder.Newest,
    )

    private fun Filters.toQuery() = LogQuery(projectId, minLevel, logFileId, sort = sort)

    private data class Filters(
        val minLevel: LfLevel? = null,
        val logFileId: Int? = null,
        val sort: LogSortOrder = LogSortOrder.Newest,
    )

    private companion object {
        const val KEY_LEVEL = "projectDetail.minLevel"
        const val KEY_FILE = "projectDetail.logFileId"
        const val KEY_SORT = "projectDetail.sort"
    }
}
