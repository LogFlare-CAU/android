package com.logflare.android.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserResponse
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.DataStoreBaseUrlProvider
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.ThemeRepository
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.push.HistoryEntry
import com.logflare.android.push.NotificationHistory
import com.logflare.android.push.PushRegistrar
import com.logflare.android.push.PushTokenSource
import java.io.File
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response

/** Robolectric only for [ThemeRepository], which reads its DataStore from a Context. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInOutUseCaseTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()

    private class FakeTokens : PushTokenSource {
        var ready: suspend () -> Boolean = { true }
        override suspend fun ensureFirebase(): Boolean = ready()
        override suspend fun getFcmTokenOrNull(): String? = "fcm-1"
    }

    private class Env(
        val auth: AuthRepository,
        val serverConfig: ServerConfigRepository,
        val baseUrl: DataStoreBaseUrlProvider,
        val session: SessionRepository,
        val recent: RecentServersRepository,
        val cache: FileCacheStore,
        val history: NotificationHistory,
        val tokens: FakeTokens,
        val registrar: PushRegistrar,
        val signOut: SignOutUseCase,
        val signIn: SignInUseCase,
    )

    private fun TestScope.env(): Env {
        fun store(name: String) = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmp.root, "$name.preferences_pb") },
        )
        val auth = AuthRepository(store("auth"))
        val serverConfig = ServerConfigRepository(store("config"))
        val baseUrl = DataStoreBaseUrlProvider(serverConfig, backgroundScope)
        val session = SessionRepository(auth, serverConfig, baseUrl, backgroundScope)
        val recent = RecentServersRepository(store("recent"))
        val cache = FileCacheStore(File(tmp.root, "cache"), Json, StandardTestDispatcher(testScheduler))
        val history = NotificationHistory(session.state, cache)
        val tokens = FakeTokens()
        val registrar = PushRegistrar(session.state, api, tokens, backgroundScope)
        val signOut = SignOutUseCase(session, registrar, history, cache)
        val signIn = SignInUseCase(api, session, baseUrl, recent, signOut)
        return Env(auth, serverConfig, baseUrl, session, recent, cache, history, tokens, registrar, signOut, signIn)
    }

    private suspend fun Env.signedIn(): SessionState.SignedIn =
        session.state.first { it is SessionState.SignedIn } as SessionState.SignedIn

    @Test
    fun signIn_doesNotAwaitFcm() = runTest {
        val env = env()
        env.tokens.ready = { awaitCancellation() } // Firebase never becomes ready
        env.registrar.start()

        val result = env.signIn("logs.example.com", "alice", "pw")

        assertEquals(SignInResult.Success, result)
        assertEquals(listOf(true), api.authCalls.map { it.keep_logged_in })
        val state = env.signedIn()
        assertEquals("Bearer jwt-alice", state.token)
        assertEquals("http://logs.example.com/", state.serverUrl)
        assertEquals(7, state.userIdx)
        assertEquals(SessionKey.of("http://logs.example.com/", 7), state.key)
        assertEquals("/me is called with the new token", listOf("Bearer jwt-alice"), api.tokens)
        runCurrent()
        assertEquals(0, api.count("registerFcmToken"))
    }

    @Test
    fun signIn_registersPushInBackground() = runTest {
        val env = env()
        env.registrar.start()

        assertEquals(SignInResult.Success, env.signIn("logs.example.com", "alice", "pw"))
        runCurrent()

        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(env.signedIn().key, env.registrar.registeredFor.value)
    }

    @Test
    fun signIn_meFailureStillSucceeds() = runTest {
        val env = env()
        api.me = { throw IOException("timeout") }

        assertEquals(SignInResult.Success, env.signIn("https://logs.example.com", "alice", "pw"))
        val state = env.signedIn()
        assertEquals("Bearer jwt-alice", state.token)
        assertEquals("alice", state.username)
        assertNull(state.userIdx)
        assertEquals(SessionKey.of("https://logs.example.com/", null, "alice"), state.key)
    }

    @Test
    fun signIn_meRejectedStillSucceeds() = runTest {
        val env = env()
        api.me = { UserResponse(false, "nope") }

        assertEquals(SignInResult.Success, env.signIn("https://logs.example.com", "bob", "pw"))
        val state = env.signedIn()
        assertEquals("bob", state.username)
        assertNull(state.userIdx)
    }

    @Test
    fun signIn_rememberServer() = runTest {
        val env = env()
        env.recent.remember("https://old.example.com")

        env.signIn("logs.example.com", "alice", "pw")

        assertEquals(listOf("http://logs.example.com/", "https://old.example.com/"), env.recent.servers.first())
        assertEquals("http://logs.example.com/", env.serverConfig.serverUrl.first())
        assertEquals("http://logs.example.com/", env.baseUrl.getBaseUrl())
    }

    @Test
    fun signIn_401_invalidCredentials() = runTest {
        val env = env()
        api.authenticateResult = { throw httpError(401) }
        assertEquals(SignInResult.InvalidCredentials, env.signIn("logs.example.com", "alice", "bad"))

        api.authenticateResult = { throw httpError(403) }
        assertEquals(SignInResult.InvalidCredentials, env.signIn("logs.example.com", "alice", "bad"))

        api.authenticateResult = { StringResponse(false, "wrong password", 401) }
        assertEquals(SignInResult.InvalidCredentials, env.signIn("logs.example.com", "alice", "bad"))

        api.authenticateResult = { StringResponse(true, "ok", data = "") }
        assertEquals(SignInResult.InvalidCredentials, env.signIn("logs.example.com", "alice", "bad"))

        assertEquals(SessionState.SignedOut, env.session.state.first { it != SessionState.Loading })
        assertEquals("a failed sign-in remembers nothing", emptyList<String>(), env.recent.servers.first())
        assertEquals(0, api.count("getme"))
    }

    @Test
    fun signIn_rateLimitedAndUnreachable() = runTest {
        val env = env()
        api.authenticateResult = { throw httpError(429) }
        assertEquals(SignInResult.RateLimited, env.signIn("logs.example.com", "alice", "pw"))

        val offline = IOException("no route")
        api.authenticateResult = { throw offline }
        assertEquals(SignInResult.ServerUnreachable(offline), env.signIn("logs.example.com", "alice", "pw"))

        val result = env.signIn("   ", "alice", "pw")
        assertTrue(result is SignInResult.ServerUnreachable)
        assertEquals(SessionState.SignedOut, env.session.state.first { it != SessionState.Loading })
    }

    @Test
    fun signOut_clearsSessionDataKeepsTheme() = runTest {
        val theme = ThemeRepository(ApplicationProvider.getApplicationContext())
        theme.setPreference(ThemePreference.DARK)
        val env = env()
        env.registrar.start()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        runCurrent()
        env.history.add(entry(1), alice.key)
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        var tokenAtUnregister: String? = "not called"
        api.unregisterFcm = {
            tokenAtUnregister = env.session.currentToken()
            com.example.logflare.core.model.RemovedResponse(true, "ok")
        }

        env.signOut()

        assertEquals("unregistered while the bearer was still there", "Bearer jwt-alice", tokenAtUnregister)
        assertEquals(listOf("fcm-1", "fcm-1"), api.fcmTokens)
        assertEquals(SessionState.SignedOut, env.session.state.first { it !is SessionState.SignedIn })
        assertNull(env.auth.token.first())
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
        assertEquals(emptyList<HistoryEntry>(), env.history.entries.first())
        assertNull(env.cache.read(alice.key, "notification-history", HISTORY))
        // Device settings survive.
        assertEquals(ThemePreference.DARK, theme.preference.first())
        assertEquals(listOf("http://logs.example.com/"), env.recent.servers.first())
        assertEquals("http://logs.example.com/", env.serverConfig.serverUrl.first())

        // Signing in again as the same user starts from an empty history.
        env.signIn("logs.example.com", "alice", "pw")
        assertEquals(alice.key, env.signedIn().key)
        assertEquals(emptyList<HistoryEntry>(), env.history.entries.first())
        theme.setPreference(ThemePreference.SYSTEM)
    }

    @Test
    fun signOut_unregisterTimeout_stillSignsOut() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        api.unregisterFcm = { awaitCancellation() }

        val start = currentTime
        env.signOut()

        assertEquals("gave up after 3 s", 3_000L, currentTime - start)
        assertEquals(SessionState.SignedOut, env.session.state.first { it !is SessionState.SignedIn })
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
    }

    @Test
    fun signIn_whileSignedIn_signsOutPreviousSessionFirst() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        api.me = { UserResponse(true, "ok", data = UserDTO(8, "bob", 1)) }

        assertEquals(SignInResult.Success, env.signIn("other.example.com", "bob", "pw"))

        val bob = env.session.state.first { it is SessionState.SignedIn && it.userIdx == 8 } as SessionState.SignedIn
        assertEquals("http://other.example.com/", bob.serverUrl)
        assertEquals(1, api.count("unregisterFcmToken"))
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
    }

    private companion object {
        val HISTORY = kotlinx.serialization.builtins.ListSerializer(HistoryEntry.serializer())

        fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody(null)))

        fun entry(errorId: Int) = HistoryEntry(
            errorId = errorId,
            projectId = 1,
            type = "ValueError",
            level = LfLevel.Error,
            message = "boom",
            receivedAt = 1_000L,
            read = false,
        )
    }
}
