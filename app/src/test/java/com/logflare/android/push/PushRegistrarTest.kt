package com.logflare.android.push

import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.RemovedResponse
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrarTest {

    private val api = FakeLogflareApi().apply { authHeader = { (sessions.value as? SessionState.SignedIn)?.token } }
    private val sessions = MutableStateFlow<SessionState>(SessionState.SignedOut)

    /** [PushTokenSource] whose behavior each test can swap. */
    private class FakeTokens : PushTokenSource {
        var ready: suspend () -> Boolean = { true }
        var token: suspend () -> String? = { "fcm-1" }
        var ensureCalls = 0
        var cachedReady: suspend () -> Boolean = { true }

        override suspend fun ensureFirebase(): Boolean {
            ensureCalls++
            return ready()
        }

        override suspend fun ensureFirebaseFromCache(): Boolean = cachedReady()

        override suspend fun getFcmTokenOrNull(): String? = token()
    }

    private val tokens = FakeTokens()

    /** Survives "process restarts": shared by every registrar a test creates. */
    private val saved = InMemoryPushRegistrationStore()

    private fun TestScope.registrar() = PushRegistrar(sessions, api, tokens, saved, backgroundScope)

    @Test
    fun registrar_registersOncePerSession() = runTest {
        val registrar = registrar()
        registrar.start()
        runCurrent()
        assertEquals("signed out never registers", 0, api.count("registerFcmToken"))

        sessions.value = signedIn(A, token = "Bearer a")
        runCurrent()
        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(listOf("Bearer a"), api.tokens)
        assertEquals(listOf("fcm-1"), api.fcmTokens)
        assertEquals(A, registrar.registeredFor.value)

        // Same session, other fields change (profile refresh): no new registration.
        sessions.value = signedIn(A, token = "Bearer a", permission = 9)
        registrar.start() // idempotent
        runCurrent()
        assertEquals(1, api.count("registerFcmToken"))

        sessions.value = signedIn(B, token = "Bearer b")
        runCurrent()
        assertEquals(2, api.count("registerFcmToken"))
        assertEquals(B, registrar.registeredFor.value)

        sessions.value = SessionState.SignedOut
        runCurrent()
        assertNull(registrar.registeredFor.value)
        sessions.value = signedIn(A, token = "Bearer a2")
        runCurrent()
        assertEquals("signing in again registers again", 3, api.count("registerFcmToken"))
        assertEquals("Bearer a2", api.tokens.last())
    }

    @Test
    fun registrar_409IsSuccess() = runTest {
        api.registerFcm = { throw httpError(409) }
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()

        assertEquals(A, registrar.registeredFor.value)
        assertEquals("no retry after 409", 1, api.count("registerFcmToken"))
    }

    @Test
    fun registrar_serverErrorIsNotRegistered() = runTest {
        api.registerFcm = { throw httpError(500) }
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()
        assertNull(registrar.registeredFor.value)

        api.registerFcm = { FcmTokenResponse(false, "nope") }
        sessions.value = signedIn(B)
        runCurrent()
        assertNull(registrar.registeredFor.value)
    }

    @Test
    fun registrar_firebaseNotReady_skipsRegistration() = runTest {
        tokens.ready = { false }
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()

        assertEquals(1, tokens.ensureCalls)
        assertEquals(0, api.count("registerFcmToken"))
        assertNull(registrar.registeredFor.value)
    }

    @Test
    fun onNewToken_registersForSignedInSessionOnly() = runTest {
        val registrar = registrar()
        registrar.onNewToken("fcm-new")
        assertEquals(0, api.count("registerFcmToken"))

        sessions.value = signedIn(A, token = "Bearer a")
        registrar.onNewToken("fcm-new")
        assertEquals(listOf("fcm-new"), api.fcmTokens)
        assertEquals(listOf("Bearer a"), api.tokens)
        assertEquals(A, registrar.registeredFor.value)
    }

    @Test
    fun unregister_sendsRegisteredToken_andForgetsIt() = runTest {
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()
        tokens.token = { error("Firebase must not be needed for a token we registered") }

        assertTrue(registrar.unregister())
        assertEquals(listOf("fcm-1", "fcm-1"), api.fcmTokens)
        assertEquals(1, api.count("unregisterFcmToken"))
        assertNull(registrar.registeredFor.value)
    }

    @Test
    fun onNewToken_duringSignOut_cannotRegisterAfterUnregister() = runTest {
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A, token = "Bearer a")
        runCurrent()
        assertTrue(registrar.unregister())

        // Firebase refreshes the token between the DELETE and the token being cleared.
        registrar.onNewToken("fcm-2")
        assertEquals(1, api.count("registerFcmToken"))
        assertNull(registrar.registeredFor.value)

        // The same user signing in again gets a new bearer and registers normally, even if the
        // SignedOut in between is never observed.
        sessions.value = signedIn(A, token = "Bearer a2")
        runCurrent()
        assertEquals(2, api.count("registerFcmToken"))
        assertEquals(A, registrar.registeredFor.value)
    }

    @Test
    fun unregister_cancelsPendingRegistration() = runTest {
        tokens.ready = { awaitCancellation() }
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()

        assertTrue(registrar.unregister())
        tokens.ready = { true }
        runCurrent()
        assertEquals("cancelled registration never lands after unregister", 0, api.count("registerFcmToken"))
    }

    @Test
    fun unregister_timesOut() = runTest {
        api.unregisterFcm = { awaitCancellation() }
        val registrar = registrar()
        sessions.value = signedIn(A)

        assertFalse(registrar.unregister(timeoutMs = 3_000))
        assertEquals(3_000L, currentTime)
    }

    @Test
    fun unregister_failureReturnsFalse() = runTest {
        api.unregisterFcm = { RemovedResponse(false, "nope") }
        val registrar = registrar()
        sessions.value = signedIn(A)
        assertFalse(registrar.unregister())

        tokens.token = { null }
        api.unregisterFcm = { RemovedResponse(true, "ok") }
        assertFalse("no token known: nothing to unregister", registrar.unregister())
    }

    @Test
    fun restart_withSameSessionAndToken_makesNoCalls() = runTest {
        registrar().start()
        sessions.value = signedIn(A)
        runCurrent()
        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(PushRegistration(A, "fcm-1"), saved.get())

        // Process restart: a new registrar with the same persisted pair.
        tokens.ensureCalls = 0
        val restarted = registrar()
        restarted.start()
        runCurrent()

        assertEquals("no GET /fcm/data", 0, tokens.ensureCalls)
        assertEquals("no POST /fcm/token", 1, api.count("registerFcmToken"))
        assertEquals(A, restarted.registeredFor.value)

        // Firebase rotates the token: registered as usual.
        restarted.onNewToken("fcm-2")
        assertEquals(2, api.count("registerFcmToken"))
        assertEquals(PushRegistration(A, "fcm-2"), saved.get())
    }

    @Test
    fun restart_withChangedFcmToken_registers() = runTest {
        saved.set(PushRegistration(A, "fcm-old"))
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()

        assertEquals(1, tokens.ensureCalls)
        assertEquals(listOf("fcm-1"), api.fcmTokens)
        assertEquals(PushRegistration(A, "fcm-1"), saved.get())
    }

    @Test
    fun restart_forAnotherSession_registers() = runTest {
        saved.set(PushRegistration(B, "fcm-1"))
        registrar().start()
        sessions.value = signedIn(A)
        runCurrent()

        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(PushRegistration(A, "fcm-1"), saved.get())
    }

    @Test
    fun signOutAndUnregister_forgetTheRegistration() = runTest {
        val registrar = registrar()
        registrar.start()
        sessions.value = signedIn(A)
        runCurrent()
        assertTrue(registrar.unregister())
        assertNull(saved.get())

        sessions.value = signedIn(A, token = "Bearer a2")
        runCurrent()
        assertEquals(PushRegistration(A, "fcm-1"), saved.get())
        // A local sign-out (401) never unregisters, but signing in again must still register.
        sessions.value = SessionState.SignedOut
        runCurrent()
        assertNull(saved.get())
    }

    private companion object {
        val A = SessionKey("aaaaaaaaaaaaaaaa")
        val B = SessionKey("bbbbbbbbbbbbbbbb")

        fun signedIn(key: SessionKey, token: String = "Bearer ${key.value}", permission: Int = 1) =
            SessionState.SignedIn(
                serverUrl = "https://logs.example.com/",
                token = token,
                username = "user",
                userIdx = 1,
                permission = permission,
                key = key,
            )

        fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody(null)))
    }
}
