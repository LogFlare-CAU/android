package com.logflare.android.feature.project

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.ProjectPermsDTO
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserSequenceResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.MemberStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.Member
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.GlobalAlert
import com.logflare.android.push.LegacyAlerts
import com.logflare.android.push.ProjectAlert
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSettingsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi().apply {
        projects = { FakeLogflareApi.ok(listOf(FakeLogflareApi.project(1, "alpha"), FakeLogflareApi.project(2, "beta"))) }
        users = {
            UserSequenceResponse(
                true,
                "ok",
                data = listOf(UserDTO(1, "admin", 100), UserDTO(7, "me", 80), UserDTO(8, "minsu", 0), UserDTO(9, "jihyun", 0)),
            )
        }
        projectPerms = { id ->
            ProjectPermsSequenceResponse(true, "ok", data = listOf(ProjectPermsDTO(1, id, 8, view = true)))
        }
    }
    private val sessions = MutableStateFlow<SessionState>(ADMIN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: ProjectSettingsViewModel,
        val alerts: AlertSettingsRepository,
        val events: MutableList<ProjectSettingsEvent>,
    )

    private fun TestScope.newEnv(): Env {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val alerts = AlertSettingsRepository(sessions, cache, { LegacyAlerts(null, emptyList()) }, backgroundScope)
        val account = AccountStore(api, sessions, cache, backgroundScope, onProfile = { _, _ -> }) { NOW }
        val vm = ProjectSettingsViewModel(
            SavedStateHandle(mapOf(Destinations.ProjectIdArg to 1)),
            ProjectStore(api, sessions, cache, backgroundScope) { NOW },
            MemberStore(api, sessions, cache, backgroundScope, account) { NOW },
            alerts,
            sessions,
        )
        val events = mutableListOf<ProjectSettingsEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        return Env(vm, alerts, events)
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun member_hidesAdminActions() = runTest {
        sessions.value = ADMIN.copy(permission = 0)
        val vm = newEnv().vm
        settle()

        val s = vm.state.value
        assertEquals("alpha", s.project?.name)
        assertFalse(s.isAdmin)
        assertTrue(s.members.isEmpty())
        assertFalse("getProjectPermissions" in api.calls)
        assertFalse("getAllUsers" in api.calls)

        // Admin-only sheets and token calls do nothing for a member.
        for (sheet in listOf(SettingsSheet.Name, SettingsSheet.Members, SettingsSheet.Rotate, SettingsSheet.Delete)) {
            vm.openSheet(sheet)
            settle()
            assertNull(vm.state.value.sheet)
        }
        vm.toggleReveal()
        vm.copyToken()
        settle()
        assertFalse("getProjectToken" in api.calls)

        // Per-device alert settings stay available.
        vm.openSheet(SettingsSheet.AlertLevel)
        settle()
        assertEquals(SettingsSheet.AlertLevel, vm.state.value.sheet)
    }

    @Test
    fun rename_validatesAndSaves_toast() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.openSheet(SettingsSheet.Name)
        settle()
        assertEquals(SettingsSheet.Name, vm.state.value.sheet)
        assertEquals("alpha", vm.state.value.nameDraft)
        assertEquals(NameCheck.Valid, vm.state.value.nameCheck)
        // Unchanged: nothing to save.
        vm.saveName()
        settle()
        assertFalse("changeProjectName" in api.calls)

        vm.setNameDraft("Beta")
        settle()
        assertEquals(NameCheck.Invalid(NameProblem.Duplicate), vm.state.value.nameCheck)
        vm.saveName()
        settle()
        assertFalse("changeProjectName" in api.calls)

        vm.setNameDraft("alpha-2")
        settle()
        assertEquals(NameCheck.Valid, vm.state.value.nameCheck)
        vm.saveName()
        settle()
        assertEquals(1, api.count("changeProjectName"))
        assertNull(vm.state.value.sheet)
        assertEquals(listOf<ProjectSettingsEvent>(ProjectSettingsEvent.Saved), env.events)
    }

    @Test
    fun alertLevel_savedPerDevice() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        // No own level: shows the Push alerts default (Error and above).
        assertEquals(LfLevel.Error, vm.state.value.alertLevel)
        val callsBefore = api.calls.size

        vm.saveAlertLevel(LfLevel.Warning)
        settle()
        assertEquals(LfLevel.Warning, vm.state.value.alertLevel)
        assertEquals(LfLevel.Warning, env.alerts.settings.value.projects[1]?.level)

        // Picking the global level again makes the project follow Push alerts.
        vm.saveAlertLevel(LfLevel.Error)
        settle()
        assertNull(env.alerts.settings.value.projects[1])
        assertEquals(LfLevel.Error, vm.state.value.alertLevel)

        vm.saveKeywords(listOf("healthcheck", "favicon.ico"))
        settle()
        assertEquals(ProjectAlert(mutedKeywords = setOf("healthcheck", "favicon.ico")), env.alerts.settings.value.projects[1])
        assertEquals(listOf("healthcheck", "favicon.ico"), vm.state.value.mutedKeywords)

        assertEquals(callsBefore, api.calls.size)
        assertEquals(List(3) { ProjectSettingsEvent.Saved }, env.events)

        env.alerts.setGlobal(GlobalAlert.Off)
        settle()
        assertTrue(vm.state.value.pushAlertsOff)
    }

    @Test
    fun members_lockedSuperAdmin() = runTest {
        val vm = newEnv().vm
        settle()

        val s = vm.state.value
        assertEquals(listOf(1, 7, 8, 9), s.members.map { it.member.idx })
        assertEquals(
            MemberAccess(Member(1, "admin", 100), hasAccess = true, locked = true),
            s.members.first(),
        )
        assertEquals(listOf(true, false, true, false), s.members.map { it.hasAccess })
        assertEquals(listOf(true, false, false, false), s.members.map { it.locked })
        assertEquals(2, s.memberCount)

        vm.saveMembers(setOf(1, 8, 9))
        settle()
        assertEquals(1, api.count("resetProjectPerms"))
        assertEquals(3, vm.state.value.memberCount)
        assertEquals(listOf(true, false, true, true), vm.state.value.members.map { it.hasAccess })
    }

    @Test
    fun delete_requiresExactName() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.openSheet(SettingsSheet.Delete)
        for (text in listOf("", "Alpha", "alpha ", "alph")) {
            vm.setDeleteConfirmText(text)
            vm.confirmDelete()
            settle()
        }
        assertFalse("deleteProject" in api.calls)
        assertTrue(env.events.isEmpty())

        vm.setDeleteConfirmText("alpha")
        vm.confirmDelete()
        settle()
        assertEquals(1, api.count("deleteProject"))
        assertEquals(listOf<ProjectSettingsEvent>(ProjectSettingsEvent.Deleted), env.events)
    }

    @Test
    fun rotate_confirmsThenUpdatesToken() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.confirmRotate()
        settle()
        assertFalse("rotateProjectToken" in api.calls)

        vm.openSheet(SettingsSheet.Rotate)
        settle()
        assertEquals(SettingsSheet.Rotate, vm.state.value.sheet)
        vm.confirmRotate()
        settle()
        assertEquals(1, api.count("rotateProjectToken"))
        assertNull(vm.state.value.sheet)
        assertEquals("rotated-1", vm.state.value.token)
        assertEquals(listOf<ProjectSettingsEvent>(ProjectSettingsEvent.TokenRotated), env.events)
    }

    @Test
    fun tokenFetchedOnlyOnReveal() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        assertNull(vm.state.value.token)
        assertFalse("getProjectToken" in api.calls)

        vm.toggleReveal()
        settle()
        assertEquals("tok-1", vm.state.value.token)
        assertTrue(vm.state.value.tokenRevealed)

        vm.toggleReveal()
        vm.copyToken()
        settle()
        assertFalse(vm.state.value.tokenRevealed)
        assertEquals(1, api.count("getProjectToken"))
        assertEquals(listOf<ProjectSettingsEvent>(ProjectSettingsEvent.CopyToken("tok-1")), env.events)
    }

    @Test
    fun tokenForbidden_reportsInsteadOfCrashing() = runTest {
        api.projectToken = { ProjectResponseWithToken(false, "You do not have access to this project", 403) }
        val env = newEnv()
        settle()
        env.vm.copyToken()
        settle()
        assertNull(env.vm.state.value.token)
        assertEquals(listOf<ProjectSettingsEvent>(ProjectSettingsEvent.TokenUnavailable), env.events)
    }

    private companion object {
        const val NOW = 1_000_000L
        val ADMIN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 80,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        )
    }
}
