package com.logflare.android.feature.log

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventPageDTO
import com.example.logflare.core.model.LogEventPageResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.page
import com.logflare.android.data.store.FakeLogflareApi.Companion.project
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.ui.nav.Destinations
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(SIGNED_IN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    /** Real stores over the fake API; the VM's state is collected for the whole test. */
    private fun TestScope.newVm(handle: SavedStateHandle = SavedStateHandle()): LogsViewModel {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val vm = LogsViewModel(
            handle,
            LogStore(api, sessions, cache, backgroundScope) { NOW },
            ProjectStore(api, sessions, cache, backgroundScope) { NOW },
        )
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    /** See HomeViewModelTest: the stores' fetches run in backgroundScope. */
    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun loadsFirstPage_groupsAndCounts() = runTest {
        api.projects = { ok(listOf(project(1, "alpha"))) }
        api.events = { page(1, 2, cursor = "c1") }
        val vm = newVm()
        assertTrue(vm.state.value.initialLoad)
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertEquals(2, s.count)
        assertTrue(s.hasMore)
        assertEquals(listOf(1, 2), s.groups.flatMap { g -> g.items.map { it.id } })
        assertEquals(listOf("alpha"), s.projects.map { it.name })
        assertEquals(LogQuery(), s.query)
        assertNull(api.eventsCalls.single().minLevel)
    }

    @Test
    fun changingFilter_requestsNewQuery() = runTest {
        val vm = newVm()
        settle()

        vm.setMinLevel(LfLevel.Error)
        settle()
        assertEquals("ERROR", api.eventsCalls.last().minLevel)

        vm.setProject(3)
        settle()
        assertEquals(3, api.eventsCalls.last().projectId)
        assertEquals("ERROR", api.eventsCalls.last().minLevel)

        vm.setSort(LogSortOrder.Oldest)
        settle()
        assertEquals("oldest", api.eventsCalls.last().sort)
        assertEquals(LogQuery(projectId = 3, minLevel = LfLevel.Error, sort = LogSortOrder.Oldest), vm.state.value.query)

        // Search text is debounced before it becomes part of the query.
        vm.toggleSearch()
        vm.onSearch("timeout")
        runCurrent()
        assertEquals("timeout", vm.state.value.search)
        val before = api.count("getLogEvents")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(before, api.count("getLogEvents"))
        settle()
        assertEquals("timeout", api.eventsCalls.last().query)
        assertEquals("timeout", vm.state.value.query.text)
    }

    @Test
    fun clearFilters_resetsToDefault() = runTest {
        val vm = newVm()
        vm.setMinLevel(LfLevel.Warning)
        vm.setProject(2)
        vm.setSort(LogSortOrder.Highest)
        vm.toggleSearch()
        vm.onSearch("db")
        settle()
        assertTrue(vm.state.value.filtered)

        vm.clearFilters()
        runCurrent()

        val s = vm.state.value
        assertEquals(LogQuery(), s.query)
        assertEquals("", s.search)
        assertFalse(s.filtered)
        settle()
        assertEquals(EventsDefaults, api.eventsCalls.last().let { Triple(it.projectId, it.minLevel, it.query) })
    }

    @Test
    fun closingSearch_clearsText_atOnce() = runTest {
        val vm = newVm()
        vm.toggleSearch()
        vm.onSearch("db")
        settle()
        assertEquals("db", vm.state.value.query.text)

        vm.toggleSearch()
        runCurrent()

        assertFalse(vm.state.value.searchOpen)
        assertEquals("", vm.state.value.search)
        // A blank query applies without the debounce.
        assertNull(vm.state.value.query.apiText)
    }

    @Test
    fun filters_surviveRecreation() = runTest {
        val handle = SavedStateHandle()
        val vm = newVm(handle)
        vm.setProject(2)
        vm.setMinLevel(LfLevel.Error)
        vm.setSort(LogSortOrder.Lowest)
        vm.toggleSearch()
        vm.onSearch("disk")
        vm.openSheet(LogsSheet.Level)
        settle()

        // Process death: only what the handle saved comes back.
        val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val again = newVm(restored)
        runCurrent()

        val s = again.state.value
        assertEquals(LogQuery(projectId = 2, minLevel = LfLevel.Error, text = "disk", sort = LogSortOrder.Lowest), s.query)
        assertTrue(s.searchOpen)
        assertEquals("disk", s.search)
    }

    @Test
    fun minLevelPreset_fromEntryHandle_appliesAndClears() = runTest {
        val entry = SavedStateHandle()
        val vm = newVm()
        vm.setProject(2)
        backgroundScope.launch { vm.collectPresets(entry) }
        settle()

        // Home's "Errors · 24h" tap while the Logs entry is alive (tab state restored).
        entry[Destinations.LogsMinLevelArg] = "ERROR"
        settle()

        assertEquals(LogQuery(minLevel = LfLevel.Error), vm.state.value.query)
        assertEquals("ERROR", api.eventsCalls.last().minLevel)
        assertNull(api.eventsCalls.last().projectId)
        // Consumed: a recreated screen doesn't apply it again.
        assertNull(entry.get<String>(Destinations.LogsMinLevelArg))

        vm.setMinLevel(null)
        settle()
        entry[Destinations.LogsMinLevelArg] = "WARNING"
        settle()
        assertEquals(LfLevel.Warning, vm.state.value.query.minLevel)
    }

    @Test
    fun endReached_callsLoadMoreOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.events = { call ->
            if (call.cursor == null) {
                page(1, 2, cursor = "c1")
            } else {
                gate.await()
                page(3)
            }
        }
        val vm = newVm()
        settle()

        vm.onEndReached()
        vm.onEndReached()
        settle()
        gate.complete(Unit)
        settle()
        vm.onEndReached() // No next page any more.
        settle()

        assertEquals(listOf(null, "c1"), api.eventsCalls.map { it.cursor })
        assertEquals(3, vm.state.value.count)
        assertFalse(vm.state.value.hasMore)
    }

    @Test
    fun loadMoreFailure_keepsContent_emitsEvent() = runTest {
        api.events = { call -> if (call.cursor == null) page(1, cursor = "c1") else throw IOException("offline") }
        val vm = newVm()
        settle()
        val received = mutableListOf<LogsEvent>()
        backgroundScope.launch { vm.events.collect { received += it } }

        vm.onEndReached()
        settle()

        assertEquals(1, vm.state.value.count)
        assertEquals(listOf<LogsEvent>(LogsEvent.LoadMoreFailed), received)
    }

    @Test
    fun refresh_awaitsStore() = runTest {
        api.events = { page(1) }
        val vm = newVm()
        settle()

        val gate = CompletableDeferred<Unit>()
        api.events = {
            gate.await()
            page(5)
        }
        vm.refresh()
        settle()
        assertTrue(vm.state.value.refreshing)
        gate.complete(Unit)
        settle()

        assertFalse(vm.state.value.refreshing)
        assertEquals(listOf(5), vm.state.value.groups.flatMap { g -> g.items.map { it.id } })
    }

    @Test
    fun emptyData_notInitialLoad() = runTest {
        api.projects = { ok(emptyList()) }
        api.events = { LogEventPageResponse(true, "ok", data = LogEventPageDTO()) }
        val vm = newVm()
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertFalse(s.loadFailed)
        assertEquals(0, s.count)
        assertTrue(s.groups.isEmpty())
        assertFalse(s.hasMore)
    }

    @Test
    fun loadFailure_withoutCache_isNotInitialLoad() = runTest {
        api.events = { throw IOException("offline") }
        val vm = newVm()
        settle()

        assertFalse(vm.state.value.initialLoad)
        assertTrue(vm.state.value.loadFailed)
    }

    @Test
    fun sheets_openAndClose() = runTest {
        val vm = newVm()
        vm.openSheet(LogsSheet.Sort)
        runCurrent()
        assertEquals(LogsSheet.Sort, vm.state.value.sheet)
        vm.closeSheet()
        runCurrent()
        assertNull(vm.state.value.sheet)
    }

    private companion object {
        const val NOW = 1_000_000L
        val EventsDefaults = Triple<Int?, String?, String?>(null, null, null)
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
