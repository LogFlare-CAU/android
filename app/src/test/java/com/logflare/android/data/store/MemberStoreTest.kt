package com.logflare.android.data.store

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserSequenceResponse
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.model.Member
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MemberStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(ProjectStoreTest.signedIn())
    private var now = 1_000_000L

    private fun TestScope.cache() = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))

    private fun TestScope.members() = MemberStore(api, sessions, cache(), backgroundScope) { now }

    private fun TestScope.account() = AccountStore(api, sessions, cache(), backgroundScope) { now }

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
