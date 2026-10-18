package com.logflare.android.feature.project

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.model.ProjectSummaryDTO
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.project
import com.logflare.android.data.store.FakeLogflareApi.Companion.summaryOf
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.ProjectItem
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(signedIn(permission = 100))
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newVm(savedState: SavedStateHandle = SavedStateHandle()): ProjectsViewModel {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val vm = ProjectsViewModel(savedState, ProjectStore(api, sessions, cache, backgroundScope) { NOW }, sessions)
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    private fun seed() {
        api.projects = { ok(listOf(project(1, "alpha"), project(2, "Beta"), project(3, "gamma"), project(4, "delta"))) }
        api.summary = {
            summaryOf(
                projects = listOf(
                    ProjectSummaryDTO(1, errorCount = 2, warningCount = 0, lastErrorAt = "2026-10-10T00:00:00"),
                    ProjectSummaryDTO(2, errorCount = 9, warningCount = 0, lastErrorAt = "2026-10-12T00:00:00"),
                    ProjectSummaryDTO(3, errorCount = 0, warningCount = 0),
                    ProjectSummaryDTO(4, errorCount = 2, warningCount = 0, lastErrorAt = null),
                ),
            )
        }
    }

    private fun item(id: Int, name: String, errors: Int = 0, last: String? = null) =
        ProjectItem(id, name, emptyList(), errors, 0, last?.let(Instant::parse))

    @Test
    fun sortProjects_recentErrors_nullsLast() {
        val list = listOf(
            item(1, "b", last = "2026-10-10T00:00:00Z"),
            item(2, "z"),
            item(3, "a"),
            item(4, "c", last = "2026-10-12T00:00:00Z"),
        )
        assertEquals(listOf(4, 1, 3, 2), sortProjects(list, ProjectSort.RecentErrors).map { it.id })
    }

    @Test
    fun sortProjects_mostErrors_thenName() {
        val list = listOf(item(1, "b", errors = 2), item(2, "a", errors = 2), item(3, "c", errors = 9), item(4, "d"))
        assertEquals(listOf(3, 2, 1, 4), sortProjects(list, ProjectSort.MostErrors).map { it.id })
    }

    @Test
    fun sortProjects_nameAsc_ignoresCase() {
        val list = listOf(item(1, "beta"), item(2, "Alpha"), item(3, "Charlie"))
        assertEquals(listOf(2, 1, 3), sortProjects(list, ProjectSort.NameAsc).map { it.id })
    }

    @Test
    fun defaultState_isRecentErrorsOrder() = runTest {
        seed()
        val vm = newVm()
        settle()
        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertEquals(4, s.total)
        assertEquals(listOf(2, 1, 4, 3), s.items.map { it.id })
        assertEquals(ProjectSort.RecentErrors, s.sort)
    }

    @Test
    fun search_filtersByName_afterDebounce() = runTest {
        seed()
        val vm = newVm()
        settle()

        vm.onSearch("ta")
        runCurrent()
        assertEquals("ta", vm.state.value.search)
        assertEquals(4, vm.state.value.items.size)

        advanceTimeBy(PROJECTS_SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        assertEquals(listOf("Beta", "delta"), vm.state.value.items.map { it.name }.sorted())
        assertEquals(4, vm.state.value.total)

        vm.onSearch("")
        runCurrent()
        assertEquals(4, vm.state.value.items.size)
    }

    @Test
    fun toggleSearch_closing_dropsText() = runTest {
        seed()
        val vm = newVm()
        settle()
        vm.toggleSearch()
        vm.onSearch("alp")
        advanceTimeBy(PROJECTS_SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        assertTrue(vm.state.value.searchOpen)
        assertEquals(1, vm.state.value.items.size)

        vm.toggleSearch()
        runCurrent()
        assertFalse(vm.state.value.searchOpen)
        assertEquals("", vm.state.value.search)
        assertEquals(4, vm.state.value.items.size)
    }

    @Test
    fun sort_andSheet() = runTest {
        seed()
        val vm = newVm()
        settle()
        vm.openSortSheet()
        runCurrent()
        assertTrue(vm.state.value.sortSheetOpen)

        vm.setSort(ProjectSort.NameAsc)
        runCurrent()
        assertFalse(vm.state.value.sortSheetOpen)
        assertEquals(listOf("alpha", "Beta", "delta", "gamma"), vm.state.value.items.map { it.name })
    }

    @Test
    fun sortAndSearch_surviveRecreation() = runTest {
        seed()
        val saved = SavedStateHandle()
        val vm = newVm(saved)
        settle()
        vm.setSort(ProjectSort.MostErrors)
        vm.toggleSearch()
        vm.onSearch("a")

        val restored = newVm(saved)
        runCurrent()
        val s = restored.state.value
        assertEquals(ProjectSort.MostErrors, s.sort)
        assertTrue(s.searchOpen)
        assertEquals("a", s.search)
    }

    @Test
    fun warmStart_showsContentWithoutAdvancingTime() = runTest {
        seed()
        newVm()
        settle()

        val vm = newVm()
        runCurrent()
        assertFalse(vm.state.value.initialLoad)
        assertEquals(4, vm.state.value.items.size)
    }

    @Test
    fun member_hidesAdminActions() = runTest {
        seed()
        sessions.value = signedIn(permission = 0)
        val vm = newVm()
        settle()
        assertFalse(vm.state.value.canCreate)
    }

    @Test
    fun admin_canCreate() = runTest {
        seed()
        sessions.value = signedIn(permission = 80)
        val vm = newVm()
        settle()
        assertTrue(vm.state.value.canCreate)
    }

    @Test
    fun emptyData_notInitialLoad() = runTest {
        api.projects = { ok(emptyList()) }
        val vm = newVm()
        assertTrue(vm.state.value.initialLoad)
        settle()
        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertFalse(s.loadFailed)
        assertEquals(0, s.total)
        assertTrue(s.items.isEmpty())
    }

    @Test
    fun loadFailure_withoutCache_isRetryState() = runTest {
        api.projects = { throw IOException("offline") }
        val vm = newVm()
        settle()
        assertFalse(vm.state.value.initialLoad)
        assertTrue(vm.state.value.loadFailed)
    }

    @Test
    fun refreshFailure_keepsContent_emitsEvent() = runTest {
        seed()
        val vm = newVm()
        settle()
        val received = mutableListOf<ProjectsEvent>()
        backgroundScope.launch { vm.events.collect { received += it } }

        api.projects = { throw IOException("offline") }
        vm.refresh()
        settle()

        assertEquals(4, vm.state.value.items.size)
        assertFalse(vm.state.value.loadFailed)
        assertFalse(vm.state.value.refreshing)
        assertEquals(listOf(ProjectsEvent.RefreshFailed), received)
    }

    private companion object {
        const val NOW = 1_000_000L

        fun signedIn(permission: Int) = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = permission,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        )
    }
}
