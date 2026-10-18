package com.logflare.android.feature.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.ProjectItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

enum class ProjectSort { RecentErrors, NameAsc, MostErrors }

data class ProjectsUiState(
    /** Projects to show: matching the (debounced) search, in [sort] order. */
    val items: List<ProjectItem> = emptyList(),
    /** All projects, whatever the search. */
    val total: Int = 0,
    /** Search text as typed; [items] follow it after [PROJECTS_SEARCH_DEBOUNCE_MS]. */
    val search: String = "",
    val searchOpen: Boolean = false,
    val sort: ProjectSort = ProjectSort.RecentErrors,
    val sortSheetOpen: Boolean = false,
    /** Admins only: the New button and the empty-state action. */
    val canCreate: Boolean = false,
    val initialLoad: Boolean = true,
    /** Nothing cached and the first load failed: show the retry state, not "no projects". */
    val loadFailed: Boolean = false,
    val refreshing: Boolean = false,
)

sealed interface ProjectsEvent {
    data object RefreshFailed : ProjectsEvent
}

internal const val PROJECTS_SEARCH_DEBOUNCE_MS = 300L

private const val ADMIN_PERMISSION = 80

/** Name order is case-insensitive; it also breaks ties in the other orders. */
fun sortProjects(list: List<ProjectItem>, sort: ProjectSort): List<ProjectItem> {
    val byName = compareBy<ProjectItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
    return when (sort) {
        ProjectSort.RecentErrors -> list.sortedWith(
            compareBy<ProjectItem, Instant?>(nullsLast(reverseOrder())) { it.lastErrorAt }.then(byName),
        )
        ProjectSort.MostErrors -> list.sortedWith(compareByDescending<ProjectItem> { it.errorCount24h }.then(byName))
        ProjectSort.NameAsc -> list.sortedWith(byName)
    }
}

/**
 * The Projects tab: [ProjectStore]'s projects, searched by name and sorted locally. Sort and search
 * live in [savedState] so they survive process death; the sort sheet does not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectsViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val projectStore: ProjectStore,
    sessions: StateFlow<SessionState>,
) : ViewModel() {

    @Inject
    constructor(
        savedState: SavedStateHandle,
        projectStore: ProjectStore,
        sessionRepository: SessionRepository,
    ) : this(savedState, projectStore, sessionRepository.state)

    private val filters = MutableStateFlow(readFilters())
    private val sheetOpen = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)

    private val _events = Channel<ProjectsEvent>(Channel.BUFFERED)
    val events: Flow<ProjectsEvent> = _events.receiveAsFlow()

    /**
     * The search text the list follows. Typing waits [PROJECTS_SEARCH_DEBOUNCE_MS] (restarting
     * while typing); the first value (blank, or restored after process death) and a blank text are
     * never held back, so the first state isn't delayed and clearing never shows stale results.
     */
    private val appliedSearch: Flow<String> = flow {
        var first = true
        emitAll(
            filters.map { it.search }.distinctUntilChanged().transformLatest { text ->
                if (!first && text.isNotBlank()) delay(PROJECTS_SEARCH_DEBOUNCE_MS)
                first = false
                emit(text)
            },
        )
    }

    private val canCreate = sessions.map { it is SessionState.SignedIn && (it.permission ?: 0) >= ADMIN_PERMISSION }

    private val local = combine(filters, sheetOpen, refreshing, canCreate) { f, sheet, r, admin ->
        Local(f, sheet, r, admin)
    }

    val state: StateFlow<ProjectsUiState> = combine(projectStore.projects(), appliedSearch, local) { res, applied, l ->
        val all = res.data.orEmpty()
        val q = applied.trim()
        ProjectsUiState(
            items = sortProjects(all.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }, l.filters.sort),
            total = all.size,
            search = l.filters.search,
            searchOpen = l.filters.searchOpen,
            sort = l.filters.sort,
            sortSheetOpen = l.sheetOpen,
            canCreate = l.canCreate,
            initialLoad = res.isInitialLoad,
            loadFailed = res.data == null && res.error != null,
            refreshing = l.refreshing,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        filters.value.let { ProjectsUiState(search = it.search, searchOpen = it.searchOpen, sort = it.sort) },
    )

    /** Opens the search field, or closes it and drops its text. */
    fun toggleSearch() =
        update { if (it.searchOpen) it.copy(searchOpen = false, search = "") else it.copy(searchOpen = true) }

    fun onSearch(text: String) = update { it.copy(search = text) }

    fun openSortSheet() {
        sheetOpen.value = true
    }

    fun closeSortSheet() {
        sheetOpen.value = false
    }

    /** Picks a sort order and closes the sheet. */
    fun setSort(sort: ProjectSort) {
        update { it.copy(sort = sort) }
        sheetOpen.value = false
    }

    /** Pull-to-refresh, awaited; a failure keeps what is shown and emits [ProjectsEvent.RefreshFailed]. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            val result = projectStore.refresh()
            refreshing.value = false
            if (result.isFailure) _events.send(ProjectsEvent.RefreshFailed)
        }
    }

    private fun update(change: (Filters) -> Filters) {
        val next = change(filters.value)
        filters.value = next
        savedState[KEY_SORT] = next.sort.name
        savedState[KEY_SEARCH] = next.search
        savedState[KEY_SEARCH_OPEN] = next.searchOpen
    }

    private fun readFilters() = Filters(
        sort = savedState.get<String>(KEY_SORT)?.let { runCatching { ProjectSort.valueOf(it) }.getOrNull() }
            ?: ProjectSort.RecentErrors,
        search = savedState[KEY_SEARCH] ?: "",
        searchOpen = savedState[KEY_SEARCH_OPEN] ?: false,
    )

    private data class Filters(
        val sort: ProjectSort = ProjectSort.RecentErrors,
        val search: String = "",
        val searchOpen: Boolean = false,
    )

    private class Local(val filters: Filters, val sheetOpen: Boolean, val refreshing: Boolean, val canCreate: Boolean)

    private companion object {
        const val KEY_SORT = "projects.sort"
        const val KEY_SEARCH = "projects.search"
        const val KEY_SEARCH_OPEN = "projects.searchOpen"
    }
}
