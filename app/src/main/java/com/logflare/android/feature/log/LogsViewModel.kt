package com.logflare.android.feature.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.Resource
import com.logflare.android.data.store.model.LogPage
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

enum class LogsSheet { Project, Level, Sort }

data class LogsUiState(
    /** The query the list shows: filters plus the debounced search text. */
    val query: LogQuery = LogQuery(),
    val searchOpen: Boolean = false,
    /** Search text as typed; it joins [query] after [LOGS_SEARCH_DEBOUNCE_MS]. */
    val search: String = "",
    val groups: List<DayGroup> = emptyList(),
    /** Rows loaded so far (all pages). */
    val count: Int = 0,
    val hasMore: Boolean = false,
    val initialLoad: Boolean = true,
    /** Nothing cached and the first load failed: show the retry state, not "no logs". */
    val loadFailed: Boolean = false,
    val refreshing: Boolean = false,
    val projects: List<ProjectItem> = emptyList(),
    val sheet: LogsSheet? = null,
) {
    /** The list shown is filtered or searched (offers "Clear filters"); typed text counts once applied. */
    val filtered: Boolean get() = query != LogQuery()
}

sealed interface LogsEvent {
    data object RefreshFailed : LogsEvent
    data object LoadMoreFailed : LogsEvent
}

internal const val LOGS_SEARCH_DEBOUNCE_MS = 300L

/**
 * The Logs tab: server-side filters (project, minimum level, sort, search) over [LogStore], with
 * infinite scroll. Filters and search text live in [savedState], so they survive process death;
 * an open sheet does not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val logStore: LogStore,
    projectStore: ProjectStore,
) : ViewModel() {

    /** Every filter in one value, so a reset is one change (no transient mixed query). */
    private val filters = MutableStateFlow(readFilters())
    private val sheet = MutableStateFlow<LogsSheet?>(null)
    private val refreshing = MutableStateFlow(false)
    private var loadingMore = false

    private val _events = Channel<LogsEvent>(Channel.BUFFERED)
    val events: Flow<LogsEvent> = _events.receiveAsFlow()

    /**
     * The query to show. A change of the search text alone waits [LOGS_SEARCH_DEBOUNCE_MS] (and
     * restarts while typing); anything else applies at once, together with the text typed so far.
     * A blank text is never held back, so clearing the search never shows stale results, and the
     * first value (e.g. restored after process death) is not held back either.
     */
    private val query: Flow<LogQuery> = flow {
        var shown: Filters? = null
        emitAll(
            filters.transformLatest { f ->
                val prev = shown
                if (prev != null && f.search.isNotBlank() && f.toQuery().copy(text = null) == prev.toQuery().copy(text = null)) {
                    delay(LOGS_SEARCH_DEBOUNCE_MS)
                }
                shown = f
                emit(f.toQuery())
            },
        )
    }.distinctUntilChanged()

    private val page: Flow<Pair<LogQuery, Resource<LogPage>>> =
        query.flatMapLatest { q -> logStore.events(q).map { q to it } }

    private val local = combine(sheet, refreshing, filters) { sh, r, f -> Local(sh, r, f.search, f.searchOpen) }

    val state: StateFlow<LogsUiState> = combine(page, projectStore.projects(), local) { (q, res), projects, l ->
        val items = res.data?.items.orEmpty()
        val zone = ZoneId.systemDefault()
        LogsUiState(
            query = q,
            searchOpen = l.searchOpen,
            search = l.search,
            groups = groupByDay(items, LocalDate.now(zone), zone),
            count = items.size,
            hasMore = res.data?.hasMore == true,
            initialLoad = res.isInitialLoad,
            loadFailed = res.data == null && res.error != null,
            refreshing = l.refreshing,
            projects = projects.data.orEmpty(),
            sheet = l.sheet,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        filters.value.let { LogsUiState(query = it.toQuery(), searchOpen = it.searchOpen, search = it.search) },
    )

    /**
     * Applies level presets written to the Logs back-stack entry's [entryState] under
     * [Destinations.LogsMinLevelArg] (Home's stat taps, see `openLogs`), for as long as the caller
     * collects. That handle is the nav entry's, not this ViewModel's: Navigation keeps them apart.
     * A preset replaces every filter (the tap means "show me these levels") and is then cleared,
     * so a recreated screen doesn't apply it again.
     */
    suspend fun collectPresets(entryState: SavedStateHandle) {
        entryState.getStateFlow<String?>(Destinations.LogsMinLevelArg, null).collect { raw ->
            if (raw == null) return@collect
            update { Filters(minLevel = LfLevel.parse(raw)) }
            entryState[Destinations.LogsMinLevelArg] = null
        }
    }

    fun setProject(id: Int?) = update { it.copy(projectId = id) }

    fun setMinLevel(level: LfLevel?) = update { it.copy(minLevel = level) }

    fun setSort(order: LogSortOrder) = update { it.copy(sort = order) }

    /** Opens the search field, or closes it and drops its text. */
    fun toggleSearch() = update { if (it.searchOpen) it.copy(searchOpen = false, search = "") else it.copy(searchOpen = true) }

    fun onSearch(text: String) = update { it.copy(search = text) }

    /** Back to the unfiltered list; an open search field stays open, emptied. */
    fun clearFilters() = update { Filters(searchOpen = it.searchOpen) }

    fun openSheet(target: LogsSheet) {
        sheet.value = target
    }

    fun closeSheet() {
        sheet.value = null
    }

    /** Pull-to-refresh of the current query, awaited. */
    fun refresh() {
        if (refreshing.value) return
        val q = state.value.query
        refreshing.value = true
        viewModelScope.launch {
            val result = logStore.refresh(q)
            refreshing.value = false
            if (result.isFailure) _events.send(LogsEvent.RefreshFailed)
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
            if (result.isFailure) _events.send(LogsEvent.LoadMoreFailed)
        }
    }

    private fun update(change: (Filters) -> Filters) {
        val next = change(filters.value)
        filters.value = next
        savedState[KEY_PROJECT] = next.projectId
        savedState[KEY_LEVEL] = next.minLevel?.name
        savedState[KEY_SORT] = next.sort.name
        savedState[KEY_SEARCH] = next.search
        savedState[KEY_SEARCH_OPEN] = next.searchOpen
    }

    private fun readFilters() = Filters(
        projectId = savedState[KEY_PROJECT],
        minLevel = savedState.get<String>(KEY_LEVEL)?.let(LfLevel::parse),
        sort = savedState.get<String>(KEY_SORT)?.let(LogSortOrder::valueOf) ?: LogSortOrder.Newest,
        search = savedState[KEY_SEARCH] ?: "",
        searchOpen = savedState[KEY_SEARCH_OPEN] ?: false,
    )

    /** What the user picked; [search] is the text as typed. */
    private data class Filters(
        val projectId: Int? = null,
        val minLevel: LfLevel? = null,
        val sort: LogSortOrder = LogSortOrder.Newest,
        val search: String = "",
        val searchOpen: Boolean = false,
    ) {
        fun toQuery() = LogQuery(projectId, minLevel, text = search.takeIf { it.isNotBlank() }, sort = sort)
    }

    private class Local(val sheet: LogsSheet?, val refreshing: Boolean, val search: String, val searchOpen: Boolean)

    private companion object {
        const val KEY_PROJECT = "logs.projectId"
        const val KEY_LEVEL = "logs.minLevel"
        const val KEY_SORT = "logs.sort"
        const val KEY_SEARCH = "logs.search"
        const val KEY_SEARCH_OPEN = "logs.searchOpen"
    }
}
