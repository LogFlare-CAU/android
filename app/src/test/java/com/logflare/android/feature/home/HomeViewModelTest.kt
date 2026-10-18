package com.logflare.android.feature.home

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventPageDTO
import com.example.logflare.core.model.LogEventPageResponse
import com.example.logflare.core.model.ProjectSummaryDTO
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FakeLogflareApi.Companion.event
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.project
import com.logflare.android.data.store.FakeLogflareApi.Companion.summaryOf
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.push.HistoryEntry
import com.logflare.android.push.NotificationHistory
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
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
class HomeViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(SIGNED_IN)
    private val main = StandardTestDispatcher()
    private lateinit var history: NotificationHistory

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    /** Real stores over the fake API; the VM's state is collected for the whole test. */
    private fun TestScope.newVm(): HomeViewModel {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        history = NotificationHistory(sessions, cache)
        val vm = HomeViewModel(
            ProjectStore(api, sessions, cache, backgroundScope) { NOW },
            LogStore(api, sessions, cache, backgroundScope) { NOW },
            history,
        )
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    @Test
    fun warmCache_showsContentWithoutWaitingForSearchDebounce() = runTest {
        events(10 to "cached")
        newVm()
        settle()

        // Same cache files, fresh stores and VM: like reopening Home within the cache's max age.
        val vm = newVm()
        runCurrent()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertEquals(listOf(10), s.recent.map { it.id })
        assertEquals(1, api.count("getLogEvents"))
    }

    /** Events with the given ids and messages; event `id` belongs to project `projectOf(id)`. */
    private fun events(vararg items: Pair<Int, String>, projectOf: (Int) -> Int = { 1 }) {
        val page = LogEventPageDTO(items.map { (id, message) -> event(id, message = message).copy(projectId = projectOf(id)) })
        api.events = { LogEventPageResponse(true, "ok", data = page) }
    }

    /**
     * Runs everything due. advanceUntilIdle alone may stop while the stores' fetches (in
     * backgroundScope) are still queued, so alternate it with runCurrent.
     */
    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    private fun alert(errorId: Int, read: Boolean = false) =
        HistoryEntry(errorId, projectId = 1, type = "ValueError", level = LfLevel.Error, message = "boom", receivedAt = 1L, read = read)

    @Test
    fun combinesStoresIntoState() = runTest {
        api.projects = { ok(listOf(project(1, "alpha"), project(2, "beta"))) }
        api.summary = {
            summaryOf(
                counts = mapOf("ERROR" to 3, "CRITICAL" to 1, "WARNING" to 5, "INFO" to 9),
                projects = listOf(
                    ProjectSummaryDTO(1, errorCount = 4, warningCount = 5, lastErrorAt = "2026-10-18T09:00:00"),
                    ProjectSummaryDTO(2, errorCount = 0, warningCount = 0),
                ),
            )
        }
        events(10 to "first", 11 to "second")
        val vm = newVm()
        assertTrue(vm.state.value.initialLoad)

        history.add(alert(10), SIGNED_IN.key)
        history.add(alert(11, read = true), SIGNED_IN.key)
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertEquals(HomeSummary(errors24h = 4, warnings24h = 5, projectCount = 2), s.summary)
        assertEquals(listOf(10, 11), s.recent.map { it.id })
        assertEquals(listOf("alpha", "beta"), s.projects.map { it.name })
        assertEquals(mapOf(1 to "alpha", 2 to "beta"), s.projectNames)
        assertEquals(1, s.unreadAlerts)
        assertEquals(listOf(11, 10), s.alerts.map { it.errorId })
        assertFalse(s.loadFailed)
        // One request per kind of data: the summary shares the project list's fetch.
        assertEquals(1, api.count("listProjects"))
        assertEquals(1, api.count("getLogSummary"))
        assertEquals(1, api.count("getLogEvents"))
        assertEquals("ERROR", api.eventsCalls.single().minLevel)
    }

    @Test
    fun emptyData_notInitialLoad() = runTest {
        api.projects = { ok(emptyList()) }
        val vm = newVm()
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertFalse(s.loadFailed)
        assertEquals(HomeSummary(0, 0, 0), s.summary)
        assertTrue(s.recent.isEmpty())
        assertTrue(s.projects.isEmpty())
        assertEquals(0, s.unreadAlerts)
    }

    @Test
    fun loadFailure_withoutCache_isNotInitialLoad() = runTest {
        api.projects = { throw IOException("offline") }
        val vm = newVm()
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertTrue(s.loadFailed)
    }

    @Test
    fun projectsSortedByLastError_top3() = runTest {
        api.projects = { ok((1..5).map { project(it, "p$it") }) }
        api.summary = {
            summaryOf(
                projects = listOf(
                    ProjectSummaryDTO(1, 0, 0, lastErrorAt = "2026-10-10T00:00:00"),
                    ProjectSummaryDTO(2, 0, 0, lastErrorAt = null),
                    ProjectSummaryDTO(3, 2, 0, lastErrorAt = "2026-10-18T08:00:00"),
                    ProjectSummaryDTO(4, 1, 0, lastErrorAt = "2026-10-17T00:00:00"),
                    ProjectSummaryDTO(5, 0, 0, lastErrorAt = null),
                ),
            )
        }
        val vm = newVm()
        settle()

        assertEquals(listOf(3, 4, 1), vm.state.value.projects.map { it.id })
        // Every project stays nameable for the recent rows and search.
        assertEquals(5, vm.state.value.projectNames.size)
    }

    @Test
    fun search_matchesMessageAndProjectName() = runTest {
        api.projects = { ok(listOf(project(1, "payments-api"), project(2, "worker"))) }
        events(10 to "Payment declined", 11 to "Disk full", 12 to "Timeout") { if (it == 12) 1 else 2 }
        val vm = newVm()
        settle()
        vm.openSheet(HomeSheet.Search)
        settle()
        // Blank query lists everything loaded.
        assertEquals(listOf(10, 11, 12), vm.state.value.searchResults.logs.map { it.id })
        assertEquals(listOf(1, 2), vm.state.value.searchResults.projects.map { it.id })

        vm.onSearch("PAY")
        runCurrent()
        assertEquals("PAY", vm.state.value.search)
        advanceTimeBy(299)
        // Debounced: not applied yet.
        assertEquals(3, vm.state.value.searchResults.logs.size)

        settle()
        val results = vm.state.value.searchResults
        // By message (10) and by the log's project name (12, in payments-api).
        assertEquals(listOf(10, 12), results.logs.map { it.id })
        assertEquals(listOf(1), results.projects.map { it.id })

        // Type matches too ("RuntimeError" for every fake event).
        vm.onSearch("runtime")
        settle()
        assertEquals(listOf(10, 11, 12), vm.state.value.searchResults.logs.map { it.id })
        assertTrue(vm.state.value.searchResults.projects.isEmpty())
    }

    @Test
    fun openSearch_clearsPreviousQuery_closeSheet() = runTest {
        events(10 to "boom")
        val vm = newVm()
        settle()
        vm.openSheet(HomeSheet.Search)
        vm.onSearch("x")
        settle()
        vm.closeSheet()
        settle()
        assertEquals(null, vm.state.value.sheet)

        vm.openSheet(HomeSheet.Search)
        runCurrent()
        assertEquals(HomeSheet.Search, vm.state.value.sheet)
        assertEquals("", vm.state.value.search)
        // The cleared query applies at once: no stale "x" results while the debounce runs.
        assertEquals(listOf(10), vm.state.value.searchResults.logs.map { it.id })
    }

    @Test
    fun markAllRead_zeroUnread() = runTest {
        val vm = newVm()
        history.add(alert(1), SIGNED_IN.key)
        history.add(alert(2), SIGNED_IN.key)
        history.add(alert(3), SIGNED_IN.key)
        settle()
        assertEquals(3, vm.state.value.unreadAlerts)

        vm.markRead(2)
        settle()
        assertEquals(2, vm.state.value.unreadAlerts)
        assertTrue(vm.state.value.alerts.single { it.errorId == 2 }.read)

        vm.markAllRead()
        settle()
        assertEquals(0, vm.state.value.unreadAlerts)
    }

    @Test
    fun refresh_forcesAllStores() = runTest {
        val vm = newVm()
        settle()
        assertEquals(1, api.count("listProjects"))

        // Still fresh, but pull-to-refresh refetches anyway, and awaits the stores.
        val gate = CompletableDeferred<Unit>()
        api.events = {
            gate.await()
            LogEventPageResponse(true, "ok", data = LogEventPageDTO(listOf(event(20))))
        }
        vm.refresh()
        settle()
        assertTrue(vm.state.value.refreshing)
        gate.complete(Unit)
        settle()

        val s = vm.state.value
        assertFalse(s.refreshing)
        assertEquals(2, api.count("listProjects"))
        assertEquals(2, api.count("getLogSummary"))
        assertEquals(2, api.count("getLogEvents"))
        assertEquals(listOf(20), s.recent.map { it.id })
    }

    @Test
    fun refreshFailure_keepsContent_emitsEvent() = runTest {
        events(10 to "kept")
        val vm = newVm()
        settle()
        val received = mutableListOf<HomeEvent>()
        backgroundScope.launch { vm.events.collect { received += it } }

        api.events = { throw IOException("offline") }
        vm.refresh()
        settle()

        assertEquals(listOf(10), vm.state.value.recent.map { it.id })
        assertFalse(vm.state.value.loadFailed)
        assertEquals(listOf<HomeEvent>(HomeEvent.RefreshFailed), received)
    }

    private companion object {
        const val NOW = 1_000_000L
        val SIGNED_IN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 100,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        )
    }
}
