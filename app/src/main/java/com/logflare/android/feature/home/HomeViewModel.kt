package com.logflare.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.Resource
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.push.HistoryEntry
import com.logflare.android.push.NotificationHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeSheet { Search, Notifications }

/** Search hits among what Home already loaded (recent errors and projects). */
data class SearchResults(val logs: List<LogEvent>, val projects: List<ProjectItem>) {
    companion object {
        val None = SearchResults(emptyList(), emptyList())
    }
}

data class HomeUiState(
    val summary: HomeSummary? = null,
    val recent: List<LogEvent> = emptyList(),
    /** Top [HOME_PROJECT_COUNT] by latest error. */
    val projects: List<ProjectItem> = emptyList(),
    /** Every loaded project's name by id, for the recent rows' subtitles. */
    val projectNames: Map<Int, String> = emptyMap(),
    val initialLoad: Boolean = true,
    /** Nothing cached and the first load failed: show the retry state, not an empty server. */
    val loadFailed: Boolean = false,
    val refreshing: Boolean = false,
    val unreadAlerts: Int = 0,
    val alerts: List<HistoryEntry> = emptyList(),
    /** Search text as typed; [searchResults] follow it after [SEARCH_DEBOUNCE_MS]. */
    val search: String = "",
    val searchResults: SearchResults = SearchResults.None,
    val sheet: HomeSheet? = null,
)

sealed interface HomeEvent {
    /** Pull-to-refresh failed; the cached content stays. */
    data object RefreshFailed : HomeEvent
}

internal const val HOME_RECENT_COUNT = 5
internal const val HOME_PROJECT_COUNT = 3
internal const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectStore: ProjectStore,
    private val logStore: LogStore,
    private val history: NotificationHistory,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sheet = MutableStateFlow<HomeSheet?>(null)
    private val search = MutableStateFlow("")

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    private val data = combine(
        projectStore.projects(),
        projectStore.summary(),
        logStore.recent(HOME_RECENT_COUNT),
    ) { projects, summary, recent -> Loaded(projects, summary, recent) }

    private val ui = combine(refreshing, sheet, search) { r, s, q -> Local(r, s, q) }

    val state: StateFlow<HomeUiState> = combine(
        data,
        history.entries,
        ui,
        // Typing is debounced; a blank query (the start, or a reopened sheet) applies at once, so
        // the first state isn't held back and a cleared query never shows the old results.
        search.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
    ) { loaded, alerts, local, query ->
        val projects = loaded.projects.data.orEmpty()
        val recent = loaded.recent.data.orEmpty()
        val initialLoad = loaded.projects.isInitialLoad || loaded.recent.isInitialLoad
        HomeUiState(
            summary = loaded.summary.data,
            recent = recent,
            projects = projects.sortedWith(compareBy(nullsLast(reverseOrder())) { it.lastErrorAt }).take(HOME_PROJECT_COUNT),
            projectNames = projects.associate { it.id to it.name },
            initialLoad = initialLoad,
            loadFailed = !initialLoad && (loaded.projects.failedEmpty() || loaded.recent.failedEmpty()),
            refreshing = local.refreshing,
            unreadAlerts = alerts.count { !it.read },
            alerts = alerts,
            search = local.search,
            searchResults = search(query, recent, projects),
            sheet = local.sheet,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Pull-to-refresh: refetches the projects (with the summary) and recent errors, awaiting both. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            val projects = async { projectStore.refresh() }
            val recent = async { logStore.refreshRecent() }
            val failed = projects.await().isFailure or recent.await().isFailure
            refreshing.value = false
            if (failed) _events.send(HomeEvent.RefreshFailed)
        }
    }

    fun openSheet(target: HomeSheet) {
        if (target == HomeSheet.Search) search.value = ""
        sheet.value = target
    }

    fun closeSheet() {
        sheet.value = null
    }

    fun onSearch(text: String) {
        search.value = text
    }

    fun markRead(errorId: Int) {
        viewModelScope.launch { history.markRead(errorId) }
    }

    fun markAllRead() {
        viewModelScope.launch { history.markAllRead() }
    }

    private class Loaded(
        val projects: Resource<List<ProjectItem>>,
        val summary: Resource<HomeSummary>,
        val recent: Resource<List<LogEvent>>,
    )

    private class Local(val refreshing: Boolean, val sheet: HomeSheet?, val search: String)
}

/** Nothing to show because the fetch failed (as opposed to an empty answer). */
private fun Resource<*>.failedEmpty(): Boolean = data == null && error != null

/** Case-insensitive match on a log's message, type and project name, and on project names. */
internal fun search(query: String, logs: List<LogEvent>, projects: List<ProjectItem>): SearchResults {
    val q = query.trim()
    if (q.isEmpty()) return SearchResults(logs, projects)
    val names = projects.associate { it.id to it.name }
    return SearchResults(
        logs = logs.filter { log ->
            log.message.contains(q, ignoreCase = true) ||
                log.type.orEmpty().contains(q, ignoreCase = true) ||
                names[log.projectId].orEmpty().contains(q, ignoreCase = true)
        },
        projects = projects.filter { it.name.contains(q, ignoreCase = true) },
    )
}
