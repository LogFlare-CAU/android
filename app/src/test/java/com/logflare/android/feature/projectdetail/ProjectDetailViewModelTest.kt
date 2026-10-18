package com.logflare.android.feature.projectdetail

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogFileDTO
import com.example.logflare.core.model.ProjectDTO
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.page
import com.logflare.android.data.store.FakeLogflareApi.Companion.summaryOf
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.ui.nav.Destinations
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(SIGNED_IN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newVm(projectId: Int = 1, handle: SavedStateHandle = handleFor(projectId)): ProjectDetailViewModel {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val vm = ProjectDetailViewModel(
            handle,
            LogStore(api, sessions, cache, backgroundScope) { NOW },
            ProjectStore(api, sessions, cache, backgroundScope) { NOW },
        )
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    private fun handleFor(id: Int) = SavedStateHandle(mapOf(Destinations.ProjectIdArg to id))

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    private fun seed() {
        api.projects = {
            ok(
                listOf(
                    ProjectDTO(
                        1,
                        "api-server",
                        logfiles = listOf(LogFileDTO(10, 1, "/a/app.log", "app.log"), LogFileDTO(11, 1, "/a/db.log", "db.log")),
                    ),
                    ProjectDTO(2, "worker"),
                ),
            )
        }
        api.summary = { id ->
            if (id == 1) summaryOf(counts = mapOf("ERROR" to 3, "CRITICAL" to 1, "WARNING" to 5)) else summaryOf()
        }
        api.events = { page(1, 2, cursor = "c1") }
    }

    @Test
    fun loadsProject_summaryAndItsLogs() = runTest {
        seed()
        val vm = newVm()
        assertTrue(vm.state.value.initialLoad)
        settle()

        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertFalse(s.notFound)
        assertEquals("api-server", s.project?.name)
        assertEquals(4, s.summary?.errors24h)
        assertEquals(5, s.summary?.warnings24h)
        assertEquals(listOf(1, 2), s.items.map { it.id })
        assertTrue(s.hasMore)
        assertEquals(LogQuery(projectId = 1), s.query)
        assertEquals(1, api.eventsCalls.single().projectId)
    }

    @Test
    fun statTap_togglesLevel() = runTest {
        seed()
        val vm = newVm()
        settle()

        vm.toggleErrors()
        settle()
        assertEquals(LfLevel.Error, vm.state.value.query.minLevel)
        assertEquals("ERROR", api.eventsCalls.last().minLevel)

        vm.toggleErrors()
        settle()
        assertNull(vm.state.value.query.minLevel)

        vm.toggleWarnings()
        settle()
        assertEquals(LfLevel.Warning, vm.state.value.query.minLevel)

        // The other tile switches the level rather than clearing it.
        vm.toggleErrors()
        settle()
        assertEquals(LfLevel.Error, vm.state.value.query.minLevel)
    }

    @Test
    fun fileFilter_setsLogFileId_andSheetOpensAndCloses() = runTest {
        seed()
        val vm = newVm()
        settle()

        vm.openSheet(ProjectDetailSheet.File)
        runCurrent()
        assertEquals(ProjectDetailSheet.File, vm.state.value.sheet)

        vm.setLogFile(11)
        vm.closeSheet()
        settle()
        assertEquals(11, vm.state.value.query.logFileId)
        assertEquals(11, api.eventsCalls.last().logfileId)
        assertEquals(1, api.eventsCalls.last().projectId)
        assertNull(vm.state.value.sheet)

        vm.setLogFile(null)
        settle()
        assertNull(vm.state.value.query.logFileId)
    }

    @Test
    fun levelAndSort_changeQuery() = runTest {
        seed()
        val vm = newVm()
        settle()

        vm.setMinLevel(LfLevel.Critical)
        vm.setSort(LogSortOrder.Oldest)
        settle()
        assertEquals(LogQuery(projectId = 1, minLevel = LfLevel.Critical, sort = LogSortOrder.Oldest), vm.state.value.query)
        assertEquals("oldest", api.eventsCalls.last().sort)
    }

    @Test
    fun clearFilters_resetsFileAndLevel() = runTest {
        seed()
        val vm = newVm()
        settle()
        vm.setMinLevel(LfLevel.Warning)
        vm.setLogFile(10)
        settle()
        assertTrue(vm.state.value.filtered)

        vm.clearFilters()
        settle()
        assertEquals(LogQuery(projectId = 1), vm.state.value.query)
        assertFalse(vm.state.value.filtered)
    }

    @Test
    fun notFound_whenProjectMissing() = runTest {
        seed()
        val vm = newVm(projectId = 99)
        settle()

        val s = vm.state.value
        assertTrue(s.notFound)
        assertNull(s.project)
        assertFalse(s.initialLoad)
        assertFalse(s.loadFailed)
    }

    @Test
    fun projectsLoadFailing_isRetryState_notNotFound() = runTest {
        api.projects = { throw IOException("offline") }
        val vm = newVm()
        settle()

        val s = vm.state.value
        assertFalse(s.notFound)
        assertTrue(s.loadFailed)
    }

    @Test
    fun filters_surviveRecreation() = runTest {
        seed()
        val handle = handleFor(1)
        val first = newVm(handle = handle)
        settle()
        first.setMinLevel(LfLevel.Error)
        first.setLogFile(10)
        first.setSort(LogSortOrder.Highest)
        settle()

        val second = newVm(handle = handle)
        runCurrent()
        assertEquals(LogQuery(1, LfLevel.Error, 10, null, LogSortOrder.Highest), second.state.value.query)
    }

    @Test
    fun onEndReached_loadsNextPage_once() = runTest {
        seed()
        api.events = { call -> if (call.cursor == null) page(1, 2, cursor = "c1") else page(3, 4) }
        val vm = newVm()
        settle()

        vm.onEndReached()
        vm.onEndReached()
        settle()
        assertEquals(listOf(1, 2, 3, 4), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.hasMore)
        assertEquals(1, api.eventsCalls.count { it.cursor == "c1" })
    }

    @Test
    fun refresh_failure_emitsEvent_andKeepsContent() = runTest {
        seed()
        val vm = newVm()
        settle()
        val events = mutableListOf<ProjectDetailEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        api.events = { throw IOException("offline") }

        vm.refresh()
        settle()
        assertEquals(listOf<ProjectDetailEvent>(ProjectDetailEvent.RefreshFailed), events)
        assertEquals(listOf(1, 2), vm.state.value.items.map { it.id })
        assertNotNull(vm.state.value.project)
        assertFalse(vm.state.value.refreshing)
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
