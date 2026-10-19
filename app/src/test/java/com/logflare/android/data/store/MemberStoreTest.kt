package com.logflare.android.data.store

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.model.Member
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MemberStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi().apply { authHeader = { (sessions.value as? SessionState.SignedIn)?.token } }
    private val sessions = MutableStateFlow<SessionState>(ProjectStoreTest.signedIn())
    private var now = 1_000_000L

    private fun TestScope.cache() = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))

    /** Profiles [AccountStore] handed to the session, with the session they were fetched for. */
    private val profiles = mutableListOf<Pair<SessionKey, UserDTO>>()

    private fun TestScope.account() =
        AccountStore(api, sessions, cache(), backgroundScope, onProfile = { key, user -> profiles += key to user }) { now }

    private fun TestScope.members(account: AccountStore = account()) =
        MemberStore(api, sessions, cache(), backgroundScope, account) { now }

    @Test
    fun isNameTaken_caseInsensitive() = runTest {
        api.users = { UserSequenceResponse(true, "ok", data = listOf(UserDTO(1, "Alice", 0), UserDTO(2, "bob", 80))) }
        val store = members()
        assertEquals(
            listOf(Member(1, "Alice", 0), Member(2, "bob", 80)),
            store.members().first { it.data != null }.data,
        )

        assertTrue(store.isNameTaken("alice"))
        assertTrue(store.isNameTaken(" BOB "))
        assertFalse(store.isNameTaken("carol"))
        assertEquals(1, api.count("getAllUsers")) // answered from the cached list
    }

    @Test
    fun add_success_returnsMember_andRefreshesList() = runTest {
        assertEquals(Result.success(Member(50, "carol", 10)), members().add("carol", "pw", 10))
        assertEquals(1, api.count("createUser"))
        assertEquals(1, api.count("getAllUsers"))
    }

    @Test
    fun add_failure_isResult() = runTest {
        api.mutationRejected = true
        val result = members().add("carol", "pw", 10)
        assertEquals(FakeLogflareApi.REJECTED, (result.exceptionOrNull() as ApiException).message)
        assertEquals(0, api.count("getAllUsers"))
    }

    @Test
    fun update_success_returnsMember_andRefreshesList() = runTest {
        assertEquals(Result.success(Member(2, "bobby", 80)), members().update(2, "bobby", null, 80))
        assertEquals(1, api.count("getAllUsers"))
    }

    @Test
    fun update_failure_isResult() = runTest {
        val offline = IOException("offline")
        api.mutationError = offline
        assertSame(offline, members().update(2, "bobby", null, 80).exceptionOrNull())
    }

    @Test
    fun delete_success_refreshesList() = runTest {
        assertTrue(members().delete(2).isSuccess)
        assertEquals(1, api.count("deleteUser"))
        assertEquals(1, api.count("getAllUsers"))
    }

    @Test
    fun delete_failure_isResult() = runTest {
        api.mutationRejected = true
        assertTrue(members().delete(2).exceptionOrNull() is ApiException)
    }

    @Test
    fun me_fetch_updatesSessionProfile() = runTest {
        api.me = { UserResponse(true, "ok", data = UserDTO(7, "me", 80)) }
        account().me().first { it.data != null }

        assertEquals(listOf(ProjectStoreTest.signedIn().key to UserDTO(7, "me", 80)), profiles)
    }

    @Test
    fun me_failedFetch_leavesSessionProfile() = runTest {
        api.me = { UserResponse(false, "nope") }
        assertTrue(account().refresh().isFailure)
        assertEquals(emptyList<Pair<SessionKey, UserDTO>>(), profiles)
    }

    @Test
    fun update_ownAccount_refreshesMe() = runTest {
        val store = members()
        api.me = { UserResponse(true, "ok", data = UserDTO(7, "me2", 80)) }

        assertTrue(store.update(7, "me2", null, 80).isSuccess)

        assertEquals(1, api.count("getme"))
        assertEquals(UserDTO(7, "me2", 80), profiles.single().second)
    }

    @Test
    fun update_otherAccount_doesNotRefreshMe() = runTest {
        assertTrue(members().update(2, "bobby", null, 80).isSuccess)
        assertEquals(0, api.count("getme"))
    }

    @Test
    fun refresh_awaitsAndReportsOutcome() = runTest {
        val gate = CompletableDeferred<UserSequenceResponse>()
        api.users = { gate.await() }
        val store = members()

        val result = async { store.refresh() }
        runCurrent()
        assertFalse(result.isCompleted)
        gate.complete(UserSequenceResponse(false, "nope"))
        assertTrue(result.await().exceptionOrNull() is ApiException)

        api.users = { UserSequenceResponse(true, "ok", data = emptyList()) }
        assertTrue(store.refresh().isSuccess)
        assertTrue(account().refresh().isSuccess)
        assertEquals(1, api.count("getme"))

        sessions.value = SessionState.SignedOut
        assertTrue(store.refresh().exceptionOrNull() is NotSignedInException)
        assertTrue(account().refresh().exceptionOrNull() is NotSignedInException)
    }

    @Test
    fun me_doesNotCallFcmEndpoints() = runTest {
        val me = account().me().first { it.data != null }.data

        assertEquals(Member(7, "me", 100), me)
        assertEquals(1, api.count("getme"))
        assertEquals(0, api.count("getFcmConfig"))
        assertEquals(0, api.count("registerFcmToken"))
        assertEquals(0, api.count("getFirebaseConfig"))
        assertEquals(0, api.count("unregisterFcmToken"))
        assertEquals(listOf(ProjectStoreTest.TOKEN), api.tokens)
    }
}
