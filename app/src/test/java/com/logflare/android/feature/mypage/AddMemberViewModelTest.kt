package com.logflare.android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import com.example.logflare.core.model.UserCreateParams
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserSequenceResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.MemberStore
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem
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
class AddMemberViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi().apply {
        users = {
            UserSequenceResponse(true, "ok", data = listOf(UserDTO(1, "admin", 100), UserDTO(7, "me", 80), UserDTO(9, "jihyun", 80)))
        }
    }
    private val sessions = MutableStateFlow<SessionState>(ADMIN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: AddMemberViewModel, val events: MutableList<AddMemberEvent>)

    private fun TestScope.newEnv(handle: SavedStateHandle = SavedStateHandle()): Env {
        val cache = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))
        val account = AccountStore(api, sessions, cache, backgroundScope, onProfile = { _, _ -> }) { NOW }
        val store = MemberStore(api, sessions, cache, backgroundScope, account) { NOW }
        val vm = AddMemberViewModel(handle, store, sessions)
        val events = mutableListOf<AddMemberEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        return Env(vm, events)
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun add_disabledUntilValid() = runTest {
        val vm = newEnv().vm
        settle()
        val empty = vm.state.value
        assertEquals(NameCheck.Empty, empty.nameCheck)
        assertFalse(empty.canAdd)
        vm.add()
        settle()
        assertFalse("createUser" in api.calls)

        // Taken on the server (case-insensitive).
        vm.setName("JiHyun")
        vm.setPassword("minsupass1")
        settle()
        assertEquals(NameCheck.Invalid(NameProblem.Duplicate), vm.state.value.nameCheck)
        assertFalse(vm.state.value.canAdd)

        // Name fine, password missing a number.
        vm.setName("minsu")
        vm.setPassword("minsupass")
        settle()
        assertEquals(NameCheck.Valid, vm.state.value.nameCheck)
        assertEquals(PasswordRules(length = true, letter = true, number = false), vm.state.value.passwordRules)
        assertFalse(vm.state.value.canAdd)
        vm.add()
        settle()
        assertFalse("createUser" in api.calls)

        vm.setPassword("minsupass1")
        settle()
        assertTrue(vm.state.value.canAdd)
    }

    @Test
    fun add_success_thenAddAnother_resets() = runTest {
        val env = newEnv()
        val vm = env.vm
        settle()
        vm.setName("minsu")
        vm.setPassword("minsupass1")
        vm.setRole(MemberRole.Admin)
        settle()
        vm.add()
        settle()

        assertEquals(listOf(UserCreateParams("minsu", 80, "minsupass1")), api.userCreates)
        val done = vm.state.value
        assertEquals(AddedMember("minsu", MemberRole.Admin), done.added)
        assertEquals("logs.example.com", done.serverHost)
        assertFalse(done.submitting)
        assertFalse(done.dirty)
        // The new member list was fetched again, so My shows them.
        assertEquals(2, api.count("getAllUsers"))

        vm.addAnother()
        settle()
        val again = vm.state.value
        assertNull(again.added)
        assertEquals("", again.name)
        assertEquals("", again.password)
        assertEquals(MemberRole.Member, again.role)
        assertEquals(NameCheck.Empty, again.nameCheck)
        assertTrue(env.events.isEmpty())
    }

    @Test
    fun add_failure_keepsFormAndReports() = runTest {
        val env = newEnv()
        settle()
        env.vm.setName("minsu")
        env.vm.setPassword("minsupass1")
        api.mutationError = IOException("offline")
        env.vm.add()
        settle()
        val s = env.vm.state.value
        assertNull(s.added)
        assertFalse(s.submitting)
        assertEquals("minsu", s.name)
        assertEquals(listOf<AddMemberEvent>(AddMemberEvent.AddFailed), env.events)
    }

    @Test
    fun add_conflict_reportsTaken() = runTest {
        val env = newEnv()
        settle()
        env.vm.setName("minsu")
        env.vm.setPassword("minsupass1")
        api.mutationRejected = true
        env.vm.add()
        settle()
        assertEquals(listOf<AddMemberEvent>(AddMemberEvent.NameTaken), env.events)
        assertFalse(env.vm.state.value.submitting)
    }

    @Test
    fun noArtificialDelay() = runTest {
        val env = newEnv()
        settle()
        env.vm.setName("minsu")
        env.vm.setPassword("minsupass1")
        runCurrent()
        val start = testScheduler.currentTime
        env.vm.add()
        // Only already-due work runs: no virtual time passes.
        repeat(5) { runCurrent() }
        assertEquals(AddedMember("minsu", MemberRole.Member), env.vm.state.value.added)
        assertEquals(start, testScheduler.currentTime)
    }

    @Test
    fun close_asksFirstWhenDirty() = runTest {
        val env = newEnv()
        settle()
        env.vm.close()
        settle()
        assertEquals(listOf<AddMemberEvent>(AddMemberEvent.NavigateBack), env.events)

        env.events.clear()
        env.vm.setName("min")
        settle()
        env.vm.close()
        settle()
        assertTrue(env.vm.state.value.discardOpen)
        assertTrue(env.events.isEmpty())
        env.vm.confirmDiscard()
        settle()
        assertFalse(env.vm.state.value.discardOpen)
        assertEquals(listOf<AddMemberEvent>(AddMemberEvent.NavigateBack), env.events)
    }

    @Test
    fun form_survivesRecreation() = runTest {
        val handle = SavedStateHandle()
        val first = newEnv(handle).vm
        first.setName("minsu")
        first.setPassword("minsupass1")
        first.setRole(MemberRole.Admin)
        settle()

        val second = newEnv(handle).vm
        settle()
        val s = second.state.value
        assertEquals("minsu", s.name)
        assertEquals(MemberRole.Admin, s.role)
        // The password is not written to the saved state.
        assertEquals("", s.password)
    }

    private companion object {
        const val NOW = 1_000_000L
        val ADMIN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 80,
            key = SessionKey("cccccccccccccccc"),
        )
    }
}
