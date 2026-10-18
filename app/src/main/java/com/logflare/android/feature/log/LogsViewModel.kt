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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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
    /** Any filter or search differs from the default list (offers "Clear filters"). */
    val filtered: Boolean get() = query != LogQuery() || search.isNotBlank()
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
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val logStore: LogStore,
    projectStore: ProjectStore,
) : ViewModel() {

    private val projectId = savedState.getStateFlow<Int?>(KEY_PROJECT, null)
    private val minLevel = savedState.getStateFlow<String?>(KEY_LEVEL, null)
    private val sort = savedState.getStateFlow(KEY_SORT, LogSortOrder.Newest.name)
    private val search = savedState.getStateFlow(KEY_SEARCH, "")
    private val searchOpen = savedState.getStateFlow(KEY_SEARCH_OPEN, false)

    private val sheet = MutableStateFlow<LogsSheet?>(null)
    private val refreshing = MutableStateFlow(false)
    private var loadingMore = false

    private val _events = Channel<LogsEvent>(Channel.BUFFERED)
    val events: Flow<LogsEvent> = _events.receiveAsFlow()

    /**
     * Search text as it joins the query. Typing is debounced; the current text on start (e.g.
     * restored after process death) and a blank text apply at once, so the first state is never
     * held back and clearing the search never shows stale results.
     */
    private val appliedSearch: Flow<String> = flow {
        emit(search.value)
        emitAll(search.drop(1).debounce { if (it.isBlank()) 0L else LOGS_SEARCH_DEBOUNCE_MS })
    }

    private val filters = combine(projectId, minLevel, sort) { p, l, s ->
        LogQuery(projectId = p, minLevel = l?.let(LfLevel::parse), sort = LogSortOrder.valueOf(s))
    }

    private val query: Flow<LogQuery> =
        combine(filters, appliedSearch) { f, text -> f.copy(text = text.takeIf { it.isNotBlank() }) }
            .distinctUntilChanged()

    private val page: Flow<Pair<LogQuery, Resource<LogPage>>> =
        query.flatMapLatest { q -> logStore.events(q).map { q to it } }

    private val local = combine(sheet, refreshing, search, searchOpen) { sh, r, s, o -> Local(sh, r, s, o) }

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
        LogsUiState(query = currentFilters(), searchOpen = searchOpen.value, search = search.value),
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
            resetFilters(minLevel = LfLevel.parse(raw))
            entryState[Destinations.LogsMinLevelArg] = null
        }
    }

    fun setProject(id: Int?) {
        savedState[KEY_PROJECT] = id
    }

    fun setMinLevel(level: LfLevel?) {
        savedState[KEY_LEVEL] = level?.name
    }

    fun setSort(order: LogSortOrder) {
        savedState[KEY_SORT] = order.name
    }

    /** Opens the search field, or closes it and drops its text. */
    fun toggleSearch() {
        val open = !searchOpen.value
        if (!open) savedState[KEY_SEARCH] = ""
        savedState[KEY_SEARCH_OPEN] = open
    }

    fun onSearch(text: String) {
        savedState[KEY_SEARCH] = text
    }

    fun clearFilters() = resetFilters(minLevel = null)

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

    private fun resetFilters(minLevel: LfLevel?) {
        savedState[KEY_PROJECT] = null
        savedState[KEY_LEVEL] = minLevel?.name
        savedState[KEY_SORT] = LogSortOrder.Newest.name
        savedState[KEY_SEARCH] = ""
    }

    private fun currentFilters() = LogQuery(
        projectId = projectId.value,
        minLevel = minLevel.value?.let(LfLevel::parse),
        text = search.value.takeIf { it.isNotBlank() },
        sort = LogSortOrder.valueOf(sort.value),
    )

    private class Local(val sheet: LogsSheet?, val refreshing: Boolean, val search: String, val searchOpen: Boolean)

    private companion object {
        const val KEY_PROJECT = "logs.projectId"
        const val KEY_LEVEL = "logs.minLevel"
        const val KEY_SORT = "logs.sort"
        const val KEY_SEARCH = "logs.search"
        const val KEY_SEARCH_OPEN = "logs.searchOpen"
    }
}
