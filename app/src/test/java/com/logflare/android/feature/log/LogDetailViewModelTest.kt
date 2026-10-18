package com.logflare.android.feature.log

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.model.LogEventDetailResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FakeLogflareApi.Companion.detail
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.project
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.ui.nav.Destinations
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
class LogDetailViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(SIGNED_IN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newVm(id: Int = 5): LogDetailViewModel {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val vm = LogDetailViewModel(
            SavedStateHandle(mapOf(Destinations.ErrorIdArg to id)),
            LogStore(api, sessions, cache, backgroundScope) { NOW },
            ProjectStore(api, sessions, cache, backgroundScope) { NOW },
        )
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun startsAsInitialLoad() = runTest {
        val vm = newVm()
        assertTrue(vm.state.value.initialLoad)
        assertNull(vm.state.value.detail)
    }

    @Test
    fun loadsDetailAndProjectName() = runTest {
        api.projects = { ok(listOf(project(1, "api-server"))) }
        api.eventDetail = { LogEventDetailResponse(true, "ok", data = detail(it).copy(raw = "stack", recentIds = listOf(9, 8))) }
        val vm = newVm(5)
        settle()
        val s = vm.state.value
        assertFalse(s.initialLoad)
        assertNull(s.error)
        assertEquals(5, s.detail?.event?.id)
        assertEquals("stack", s.detail?.raw)
        assertEquals(listOf(9, 8), s.detail?.recentIds)
        assertEquals("api-server", s.projectName)
    }

    @Test
    fun notFound_showsError() = runTest {
        api.eventDetail = { LogEventDetailResponse(false, "not found", 404) }
        val vm = newVm()
        settle()
        val s = vm.state.value
        assertNull(s.detail)
        assertFalse(s.initialLoad)
        assertEquals(LogDetailError.NotFound, s.error)
    }

    @Test
    fun otherFailure_isLoadFailed() = runTest {
        api.eventDetail = { throw java.io.IOException("offline") }
        val vm = newVm()
        settle()
        assertEquals(LogDetailError.LoadFailed, vm.state.value.error)
    }

    @Test
    fun toggleRaw() = runTest {
        val vm = newVm()
        settle()
        assertFalse(vm.state.value.rawExpanded)
        vm.toggleRaw()
        settle()
        assertTrue(vm.state.value.rawExpanded)
        vm.toggleRaw()
        settle()
        assertFalse(vm.state.value.rawExpanded)
    }

    @Test
    fun shareSheetOpensAndCloses() = runTest {
        val vm = newVm()
        settle()
        vm.openShare()
        settle()
        assertTrue(vm.state.value.shareSheetOpen)
        vm.closeShare()
        settle()
        assertFalse(vm.state.value.shareSheetOpen)
        assertNotNull(vm.state.value.detail)
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
