package com.logflare.android.feature.project

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.LegacyAlerts
import com.logflare.android.push.ProjectAlert
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCreateViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(SIGNED_IN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: ProjectCreateViewModel,
        val alerts: AlertSettingsRepository,
        val events: MutableList<ProjectCreateEvent>,
    )

    private fun TestScope.newEnv(handle: SavedStateHandle = SavedStateHandle()): Env {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val alerts = AlertSettingsRepository(sessions, cache, { LegacyAlerts(null, emptyList()) }, backgroundScope)
        val vm = ProjectCreateViewModel(handle, ProjectStore(api, sessions, cache, backgroundScope) { NOW }, alerts, sessions)
        val events = mutableListOf<ProjectCreateEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        return Env(vm, alerts, events)
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun create_disabledUntilValid() = runTest {
        val vm = newEnv().vm
        settle()
        assertEquals(NameCheck.Empty, vm.state.value.nameCheck)
        vm.create()
        settle()
        assertFalse("createProject" in api.calls)

        // "alpha" exists on the server.
        vm.setName("Alpha")
        settle()
        assertTrue(vm.state.value.nameCheck is NameCheck.Invalid)
        vm.create()
        settle()
        assertFalse("createProject" in api.calls)

        vm.setName("beta")
        settle()
        assertEquals(NameCheck.Valid, vm.state.value.nameCheck)
        vm.create()
        settle()
        assertTrue("createProject" in api.calls)
    }

    @Test
    fun create_savesAlertSettingsForNewProject() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        assertEquals(LfLevel.Warning, vm.state.value.level)
        vm.setName("beta")
        vm.setLevel(LfLevel.Error)
        vm.addKeyword("timeout")
        vm.addKeyword("Retry")
        vm.addKeyword("timeout")
        settle()
        assertEquals(listOf("timeout", "Retry"), vm.state.value.keywords)
        vm.create()
        settle()

        val s = vm.state.value
        assertEquals(CreatedProject(99, "beta", "tok-99"), s.created)
        assertFalse(s.submitting)
        assertFalse(s.dirty)
        assertEquals(
            ProjectAlert(level = LfLevel.Error, mutedKeywords = setOf("timeout", "Retry")),
            env.alerts.settings.value.projects[99],
        )
    }

    @Test
    fun create_failure_keepsFormAndReports() = runTest {
        api.createProjectResult = { throw IOException("offline") }
        val env = newEnv()
        env.vm.setName("beta")
        env.vm.create()
        settle()

        assertNull(env.vm.state.value.created)
        assertFalse(env.vm.state.value.submitting)
        assertEquals("beta", env.vm.state.value.name)
        assertEquals(listOf<ProjectCreateEvent>(ProjectCreateEvent.CreateFailed), env.events)
        assertTrue(env.alerts.settings.value.projects.isEmpty())
    }

    @Test
    fun close_whenDirty_opensDiscard() = runTest {
        val env = newEnv()
        env.vm.setName("b")
        env.vm.close()
        settle()
        assertEquals(CreateSheet.Discard, env.vm.state.value.sheet)
        assertTrue(env.events.isEmpty())

        env.vm.dismissSheet()
        settle()
        assertNull(env.vm.state.value.sheet)

        env.vm.close()
        env.vm.confirmDiscard()
        settle()
        assertEquals(listOf<ProjectCreateEvent>(ProjectCreateEvent.NavigateBack), env.events)
    }

    @Test
    fun close_whenClean_navigatesBack() = runTest {
        val env = newEnv()
        settle()
        assertFalse(env.vm.state.value.dirty)
        env.vm.close()
        settle()
        assertEquals(listOf<ProjectCreateEvent>(ProjectCreateEvent.NavigateBack), env.events)
        assertNull(env.vm.state.value.sheet)
    }

    @Test
    fun dirty_countsNameLevelAndKeywords() = runTest {
        val vm = newEnv().vm
        settle()
        vm.setLevel(LfLevel.Error)
        settle()
        assertTrue(vm.state.value.dirty)
        vm.setLevel(LfLevel.Warning)
        settle()
        assertFalse(vm.state.value.dirty)
        vm.addKeyword("x")
        settle()
        assertTrue(vm.state.value.dirty)
    }

    @Test
    fun rotate_replacesToken() = runTest {
        val env = newEnv()
        env.vm.setName("beta")
        env.vm.create()
        settle()
        env.vm.openRotate()
        settle()
        assertEquals(CreateSheet.Rotate, env.vm.state.value.sheet)

        env.vm.confirmRotate()
        settle()
        val s = env.vm.state.value
        assertEquals("rotated-99", s.created?.token)
        assertNull(s.sheet)
        assertEquals(listOf<ProjectCreateEvent>(ProjectCreateEvent.TokenRotated), env.events)
    }

    @Test
    fun rotate_failure_keepsOldToken() = runTest {
        val env = newEnv()
        env.vm.setName("beta")
        env.vm.create()
        settle()
        api.mutationError = IOException("offline")
        env.vm.openRotate()
        env.vm.confirmRotate()
        settle()
        assertEquals("tok-99", env.vm.state.value.created?.token)
        assertEquals(listOf<ProjectCreateEvent>(ProjectCreateEvent.RotateFailed), env.events)
    }

    @Test
    fun form_survivesRecreation() = runTest {
        val handle = SavedStateHandle()
        val first = newEnv(handle).vm
        first.setName("beta")
        first.setLevel(LfLevel.Critical)
        first.addKeyword("noise")
        first.close() // the discard sheet is not restored
        settle()
        assertEquals(CreateSheet.Discard, first.state.value.sheet)

        val second = newEnv(handle).vm
        settle()
        val s = second.state.value
        assertEquals("beta", s.name)
        assertEquals(LfLevel.Critical, s.level)
        assertEquals(listOf("noise"), s.keywords)
        assertTrue(s.dirty)
        assertNull(s.sheet)
    }

    @Test
    fun createdProject_survivesRecreation() = runTest {
        val handle = SavedStateHandle()
        val first = newEnv(handle).vm
        first.setName("beta")
        first.create()
        settle()

        val second = newEnv(handle).vm
        settle()
        assertEquals(CreatedProject(99, "beta", "tok-99"), second.state.value.created)
        assertEquals("https://logs.example.com", second.state.value.serverUrl)
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
