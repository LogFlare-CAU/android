package com.logflare.android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserSequenceResponse
import com.example.logflare.core.model.UserUpdateParams
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.MemberStore
import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class EditMemberViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** The server's users; tests change it to mimic what the server answers after a mutation. */
    private var serverUsers = listOf(
        UserDTO(1, "admin", 100),
        UserDTO(7, "me", 80),
        UserDTO(8, "minsu", 0),
        UserDTO(9, "jihyun", 80),
    )
    private val api = FakeLogflareApi().apply {
        users = { UserSequenceResponse(true, "ok", data = serverUsers) }
    }
    private val sessions = MutableStateFlow<SessionState>(ADMIN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditMemberViewModel, val events: MutableList<EditMemberEvent>)

    private fun TestScope.newEnv(idx: Int = 8, handle: SavedStateHandle = SavedStateHandle(mapOf(Destinations.MemberIdxArg to idx))): Env {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val account = AccountStore(api, sessions, cache, backgroundScope, onProfile = { _, _ -> }) { NOW }
        val store = MemberStore(api, sessions, cache, backgroundScope, account) { NOW }
        val vm = EditMemberViewModel(handle, store, sessions)
        val events = mutableListOf<EditMemberEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        return Env(vm, events)
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun loads_member() = runTest {
        val vm = newEnv().vm
        assertEquals(EditLoad.Loading, vm.state.value.load)
        settle()
        val s = vm.state.value
        assertEquals(EditLoad.Ready, s.load)
        assertEquals(Member(8, "minsu", 0), s.member)
        assertEquals("minsu", s.name)
        assertEquals(MemberRole.Member, s.role)
        assertFalse(s.locked)
        assertFalse(s.dirty)
    }

    @Test
    fun edit_saveDisabledUntilChanged() = runTest {
        val vm = newEnv().vm
        settle()
        assertFalse(vm.state.value.canSave)
        vm.save()
        settle()
        assertFalse("updateUser" in api.calls)

        // Back to the original name: nothing changed.
        vm.setName("minsu2")
        vm.setName("minsu")
        settle()
        assertFalse(vm.state.value.dirty)
        assertFalse(vm.state.value.canSave)

        // Changed but invalid.
        vm.setName("jihyun")
        settle()
        assertEquals(NameCheck.Invalid(NameProblem.Duplicate), vm.state.value.nameCheck)
        assertTrue(vm.state.value.dirty)
        assertFalse(vm.state.value.canSave)
        vm.setName("")
        settle()
        assertFalse(vm.state.value.canSave)

        // A password that breaks the rules blocks saving too.
        vm.setName("minsu")
        vm.setPassword("short")
        settle()
        assertTrue(vm.state.value.dirty)
        assertFalse(vm.state.value.canSave)
        vm.setPassword("")
        vm.setRole(MemberRole.Admin)
        settle()
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun edit_emptyPassword_notSent() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.setName("minsu.kim")
        vm.setRole(MemberRole.Admin)
        settle()
        serverUsers = serverUsers.map { if (it.idx == 8) UserDTO(8, "minsu.kim", 80) else it }
        vm.save()
        settle()

        assertEquals(listOf(8 to UserUpdateParams(username = "minsu.kim", permission = 80, password = null)), api.userUpdates)
        assertEquals(listOf<EditMemberEvent>(EditMemberEvent.Saved), env.events)
        // The refreshed list is the new baseline: nothing left to save.
        val s = vm.state.value
        assertEquals(Member(8, "minsu.kim", 80), s.member)
        assertFalse(s.dirty)
        assertFalse(s.saving)
    }

    @Test
    fun edit_passwordOnly_sendsOnlyPassword() = runTest {
        val vm = newEnv().vm
        settle()
        vm.setPassword("newpass123")
        settle()
        assertTrue(vm.state.value.canSave)
        vm.save()
        settle()
        assertEquals(listOf(8 to UserUpdateParams(username = null, permission = null, password = "newpass123")), api.userUpdates)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun edit_failure_keepsChangesAndReports() = runTest {
        val env = newEnv()
        settle()
        env.vm.setRole(MemberRole.Admin)
        api.mutationError = IOException("offline")
        env.vm.save()
        settle()
        assertEquals(listOf<EditMemberEvent>(EditMemberEvent.SaveFailed), env.events)
        assertEquals(MemberRole.Admin, env.vm.state.value.role)
        assertFalse(env.vm.state.value.saving)
        assertTrue(env.vm.state.value.canSave)
    }

    @Test
    fun delete_confirmed_callsStoreAndNavigates() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.openDelete()
        runCurrent()
        assertTrue(vm.state.value.deleteOpen)
        serverUsers = serverUsers.filterNot { it.idx == 8 }
        vm.confirmDelete()
        settle()
        assertEquals(1, api.count("deleteUser"))
        assertEquals(listOf<EditMemberEvent>(EditMemberEvent.Deleted), env.events)
        // The member list was reloaded without them, and the screen doesn't flash "missing".
        assertEquals(EditLoad.Ready, vm.state.value.load)
        assertFalse(vm.state.value.deleteOpen)
    }

    @Test
    fun delete_failure_resetsAndReports() = runTest {
        val env = newEnv()
        settle()
        env.vm.openDelete()
        api.mutationError = IOException("offline")
        env.vm.confirmDelete()
        settle()
        assertEquals(listOf<EditMemberEvent>(EditMemberEvent.DeleteFailed), env.events)
        assertFalse(env.vm.state.value.deleting)
        assertFalse(env.vm.state.value.deleteOpen)
    }

    @Test
    fun superAdmin_isLocked() = runTest {
        val vm = newEnv(idx = 1).vm
        settle()
        val s = vm.state.value
        assertTrue(s.locked)
        vm.setRole(MemberRole.Member)
        vm.save()
        vm.openDelete()
        vm.confirmDelete()
        settle()
        assertFalse(vm.state.value.deleteOpen)
        assertFalse("updateUser" in api.calls)
        assertFalse("deleteUser" in api.calls)
    }

    @Test
    fun otherAdmin_isLockedForAnAdmin() = runTest {
        val vm = newEnv(idx = 9).vm
        settle()
        assertTrue(vm.state.value.locked)
    }

    @Test
    fun unknownMember_isMissing() = runTest {
        val vm = newEnv(idx = 42).vm
        settle()
        assertEquals(EditLoad.Missing, vm.state.value.load)
        assertNull(vm.state.value.member)
    }

    @Test
    fun noArtificialDelay() = runTest {
        val env = newEnv()
        settle()
        env.vm.setRole(MemberRole.Admin)
        runCurrent()
        val start = testScheduler.currentTime
        env.vm.save()
        repeat(5) { runCurrent() }
        assertEquals(listOf<EditMemberEvent>(EditMemberEvent.Saved), env.events)
        env.vm.openDelete()
        env.vm.confirmDelete()
        repeat(5) { runCurrent() }
        assertEquals(listOf(EditMemberEvent.Saved, EditMemberEvent.Deleted), env.events)
        assertEquals(start, testScheduler.currentTime)
    }

    private companion object {
        const val NOW = 1_000_000L
        val ADMIN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 80,
            key = SessionKey("dddddddddddddddd"),
        )
    }
}
