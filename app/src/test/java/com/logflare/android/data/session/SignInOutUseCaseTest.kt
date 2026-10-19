package com.logflare.android.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.push.HistoryEntry
import com.example.logflare.core.model.LogoutParams
import com.logflare.android.push.InMemoryPushRegistrationStore
import com.logflare.android.push.NotificationHistory
import com.logflare.android.push.PushRegistrar
import com.logflare.android.push.PushTokenSource
import java.io.File
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        override suspend fun ensureFirebaseFromCache(): Boolean = ready()
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
        val access: MemoryAccessCredentialStore,
        val signOut: SignOutUseCase,
        val signIn: SignInUseCase,
        val unauthorized: UnauthorizedSignOut,
    )

    /** Delivers every value of [delegate] [delayMs] late, like a slow session-state collector. */
    private class DelayedStore(private val delegate: DataStore<Preferences>, delayMs: Long) : DataStore<Preferences> {
        override val data: Flow<Preferences> = delegate.data.onEach { delay(delayMs) }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            delegate.updateData(transform)
    }

    private fun TestScope.env(authDelayMs: Long = 0): Env {
        fun store(name: String) = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmp.root, "$name.preferences_pb") },
        )
        val serverConfig = ServerConfigRepository(store("config"))
        val auth = AuthRepository(
            if (authDelayMs > 0) DelayedStore(store("auth"), authDelayMs) else store("auth"),
            serverConfig.serverUrl,
        )
        val baseUrl = DataStoreBaseUrlProvider(serverConfig, backgroundScope)
        val session = SessionRepository(auth, serverConfig, baseUrl, backgroundScope)
        val recent = RecentServersRepository(store("recent"))
        val cache = FileCacheStore(File(tmp.root, "cache"), Json, StandardTestDispatcher(testScheduler))
        val history = NotificationHistory(session.state, cache)
        val tokens = FakeTokens()
        val registrar = PushRegistrar(session.state, api, tokens, InMemoryPushRegistrationStore(), backgroundScope)
        val access = MemoryAccessCredentialStore()
        val localSignOut = LocalSignOut(session, history, cache, ApplicationProvider.getApplicationContext<android.content.Context>(), access)
        val signOut = SignOutUseCase(session, registrar, localSignOut)
        val signIn = SignInUseCase(api, session, baseUrl, recent, signOut, access)
        val unauthorized = UnauthorizedSignOut(session, { localSignOut }, backgroundScope)
        return Env(auth, serverConfig, baseUrl, session, recent, cache, history, tokens, registrar, access, signOut, signIn, unauthorized)
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
    fun signInWithAccess_storesCredential_andSameSessionToken() = runTest {
        val env = env()

        val result = env.signIn.signInWithAccess("https://logs.example.com", "client-id-value", "client-secret")

        assertEquals(SignInResult.Success, result)
        assertEquals(listOf(true), api.accessCalls.map { it.keep_logged_in })
        assertEquals("https://logs.example.com/", env.access.savedServer)
        assertEquals("client-id-value", env.access.savedId)
        assertEquals("client-secret", env.access.savedSecret)
        assertEquals("Bearer jwt-access", env.signedIn().token)
    }

    @Test
    fun signInWithAccess_keepLoggedInFalse_reachesTheServer() = runTest {
        val env = env()

        env.signIn.signInWithAccess("https://logs.example.com", "client-id-value", "client-secret", keepLoggedIn = false)

        assertEquals(listOf(false), api.accessCalls.map { it.keep_logged_in })
    }

    @Test
    fun signInWithAccess_credentialStoreFails_isAFailureNotACrash() = runTest {
        val env = env()
        env.access.failSave = true

        val result = env.signIn.signInWithAccess("https://logs.example.com", "id", "secret")

        assertTrue(result is SignInResult.ServerUnreachable)
        assertEquals(0, api.accessCalls.size)
    }

    @Test
    fun signInWithAccess_rejected_clearsCredential() = runTest {
        val env = env()
        api.authenticateAccessResult = { throw httpError(401) }

        assertEquals(SignInResult.InvalidCredentials, env.signIn.signInWithAccess("https://logs.example.com", "id", "secret"))
        assertNull(env.access.savedSecret)
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

        api.authenticateResult = { StringResponse(false, "slow down", 429) }
        assertEquals(SignInResult.RateLimited, env.signIn("logs.example.com", "alice", "bad"))

        assertEquals(SessionState.SignedOut, env.session.state.first { it != SessionState.Loading })
        assertEquals("a failed sign-in remembers nothing", emptyList<String>(), env.recent.servers.first())
        assertEquals(0, api.count("getMeAs"))
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

    @After
    fun resetTheme() = runBlocking {
        ThemeRepository(ApplicationProvider.getApplicationContext()).setPreference(ThemePreference.SYSTEM)
    }

    @Test
    fun signIn_failure_keepsSavedServerUrl() = runTest {
        val env = env()
        env.signIn("https://saved.example.com", "alice", "pw")
        env.signOut()
        api.authenticateResult = { throw IOException("no route") }

        assertTrue(env.signIn("https://typo.example.com", "alice", "pw") is SignInResult.ServerUnreachable)

        assertEquals("https://saved.example.com/", env.serverConfig.serverUrl.first())
        assertEquals("https://saved.example.com/", env.baseUrl.getBaseUrl())
    }

    @Test
    fun signIn_switchingAccount_neverSendsPreviousBearer() = runTest {
        val env = env(authDelayMs = 500)
        env.signIn("a.example.com", "alice", "pw")
        assertEquals("Bearer jwt-alice", env.session.currentToken())
        var bearerAtAuth: String? = "not called"
        api.authenticateResult = {
            bearerAtAuth = env.session.currentToken()
            StringResponse(true, "ok", data = "jwt-${it.username}")
        }
        api.me = { UserResponse(true, "ok", data = UserDTO(8, "bob", 1)) }

        assertEquals(SignInResult.Success, env.signIn("b.example.com", "bob", "pw"))

        assertNull("the interceptor must have no token for POST /user/auth", bearerAtAuth)
        val state = env.session.state.value as SessionState.SignedIn
        assertEquals("Bearer jwt-bob", state.token)
        assertEquals(SessionKey.of("http://b.example.com/", 8), state.key)
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
        api.logoutResult = {
            tokenAtUnregister = env.session.currentToken()
            com.example.logflare.core.model.LogoutResponse(true, "ok")
        }

        env.signOut()

        assertEquals("logged out while the bearer was still there", "Bearer jwt-alice", tokenAtUnregister)
        assertEquals(listOf(LogoutParams("fcm-1")), api.logoutBodies)
        assertEquals(0, api.count("unregisterFcmToken"))
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
    }

    @Test
    fun signOut_oldServer405_fallsBackToDeleteFcmToken_andSignsOut() = runTest {
        val env = env()
        env.registrar.start()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        runCurrent()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        api.logoutResult = { throw retrofit2.HttpException(retrofit2.Response.error<Any>(405, "{}".toResponseBody(null))) }

        env.signOut()

        assertEquals(1, api.count("logout"))
        assertEquals(1, api.count("unregisterFcmToken"))
        assertEquals(SessionState.SignedOut, env.session.state.first { it !is SessionState.SignedIn })
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
    }

    @Test
    fun signOut_logoutFails_stillSignsOut() = runTest {
        val env = env()
        env.registrar.start()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        runCurrent()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        api.logoutResult = { throw java.io.IOException("down") }

        env.signOut()

        assertEquals(SessionState.SignedOut, env.session.state.first { it !is SessionState.SignedIn })
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
    }

    @Test
    fun signOut_unregisterTimeout_stillSignsOut() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)
        api.logoutResult = { awaitCancellation() }

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
        assertEquals(1, api.count("logout"))
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
    }

    @Test
    fun unauthorized_forAnotherToken_isIgnored() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)

        // A late 401 for a previous session's token, and one for a request without a token.
        env.unauthorized.onUnauthorized("Bearer jwt-previous")
        env.unauthorized.onUnauthorized(null)
        advanceUntilIdle()

        assertEquals("Bearer jwt-alice", env.session.currentToken())
        assertEquals("cached", env.cache.read(alice.key, "projects", String.serializer())?.value)
    }

    @Test
    fun unauthorized_forCurrentToken_signsOutLocally() = runTest {
        val env = env()
        env.registrar.start()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        runCurrent()
        env.history.add(entry(1), alice.key)
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)

        env.unauthorized.onUnauthorized("Bearer jwt-alice")
        // The auth store writes off the test scheduler: wait for the sign-out, then the clears.
        env.session.state.first { it == SessionState.SignedOut }
        advanceUntilIdle()

        assertNull(env.auth.token.first())
        assertNull(env.cache.read(alice.key, "projects", String.serializer()))
        assertNull(env.cache.read(alice.key, "notification-history", HISTORY))
        assertEquals("the server already rejected the token: no logout call", 0, api.count("logout"))
    }

    @Test
    fun meFetch_withNewRole_updatesSessionState_keepsKey() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val before = env.signedIn()
        assertEquals(100, before.permission)
        // Same wiring as AccountStore's @Inject constructor.
        val account = AccountStore(api, env.session.state, env.cache, backgroundScope, { key, user -> env.session.updateProfile(user, key) }) { 0L }
        api.me = { UserResponse(true, "ok", data = UserDTO(7, "alice", 1)) }

        assertTrue(account.refresh().isSuccess)

        val after = env.session.state.value as SessionState.SignedIn
        assertEquals(1, after.permission)
        assertEquals("alice", after.username)
        assertEquals(before.key, after.key)
    }

    @Test
    fun localSignOut_cancelsPostedNotifications() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel("c", "c", android.app.NotificationManager.IMPORTANCE_HIGH))
        manager.notify(41, android.app.Notification.Builder(context, "c").setSmallIcon(android.R.drawable.ic_dialog_info).build())
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        env.signedIn()
        assertEquals(1, manager.activeNotifications.size)

        env.signOut()

        assertEquals(0, manager.activeNotifications.size)
    }

    @Test
    fun localSignOut_onlyIfToken_keepsNewerSession() = runTest {
        val env = env()
        env.signIn("logs.example.com", "alice", "pw")
        val alice = env.signedIn()
        env.cache.write(alice.key, "projects", "cached", String.serializer(), fetchedAt = 1L)

        assertFalse(LocalSignOut(env.session, env.history, env.cache, {})(onlyIfToken = "Bearer jwt-previous"))

        assertEquals("Bearer jwt-alice", env.session.currentToken())
        assertEquals("cached", env.cache.read(alice.key, "projects", String.serializer())?.value)
    }

    private class MemoryAccessCredentialStore : AccessCredentialStore {
        var savedServer: String? = null
        var savedId: String? = null
        var savedSecret: String? = null
        var failSave = false

        override fun save(serverUrl: String, clientId: String, secret: String) {
            if (failSave) throw IllegalStateException("keystore")
            savedServer = serverUrl
            savedId = clientId
            savedSecret = secret
        }

        override fun clear() {
            savedServer = null
            savedId = null
            savedSecret = null
        }

        override fun current() = savedId?.let { id ->
            savedSecret?.let { secret -> com.example.logflare.core.network.auth.AccessServiceToken(id, secret) }
        }
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
