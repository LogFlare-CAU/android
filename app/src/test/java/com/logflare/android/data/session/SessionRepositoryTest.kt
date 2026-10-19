package com.logflare.android.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.example.logflare.core.model.UserDTO
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.DataStoreBaseUrlProvider
import com.logflare.android.data.ServerConfigRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class Env(
        val auth: AuthRepository,
        val serverConfig: ServerConfigRepository,
        val baseUrl: DataStoreBaseUrlProvider,
        val scope: TestScope,
    ) {
        fun session() = SessionRepository(auth, serverConfig, baseUrl, scope.backgroundScope)
    }

    /** A store whose file can't be read (e.g. corrupt): every read and write throws. */
    private object BrokenStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("corrupt store")
    }

    /** Delivers every value of [delegate] [delayMs] late, like a slow state collector. */
    private class DelayedStore(private val delegate: DataStore<Preferences>, delayMs: Long) : DataStore<Preferences> {
        override val data: Flow<Preferences> = delegate.data.onEach { delay(delayMs) }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            delegate.updateData(transform)
    }

    private fun TestScope.env(
        brokenAuth: Boolean = false,
        brokenConfig: Boolean = false,
        authDelayMs: Long = 0,
    ): Env {
        fun store(name: String) = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmp.root, "$name.preferences_pb") },
        )
        val serverConfig = ServerConfigRepository(if (brokenConfig) BrokenStore else store("config"))
        return Env(
            auth = AuthRepository(
                when {
                    brokenAuth -> BrokenStore
                    authDelayMs > 0 -> DelayedStore(store("auth"), authDelayMs)
                    else -> store("auth")
                },
                serverConfig.serverUrl,
            ),
            serverConfig = serverConfig,
            baseUrl = DataStoreBaseUrlProvider(serverConfig, backgroundScope),
            scope = this,
        )
    }

    @Test
    fun gate_emitsLoadingThenSignedIn_whenTokenPresent() = runTest {
        val env = env()
        env.serverConfig.setServerUrl("https://logs.example.com")
        env.auth.setToken("Bearer abc")
        env.auth.setUsername("alice")
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            val signedIn = awaitItem() as SessionState.SignedIn
            assertEquals("https://logs.example.com/", signedIn.serverUrl)
            assertEquals("Bearer abc", signedIn.token)
            assertEquals("alice", signedIn.username)
            assertNull(signedIn.userIdx)
            assertEquals(SessionKey.of("https://logs.example.com/", null, "alice"), signedIn.key)
            expectNoEvents()
        }
        // The base URL cache is warm by the time the gate opens.
        assertEquals("https://logs.example.com/", env.baseUrl.getBaseUrl())
    }

    @Test
    fun gate_emitsLoadingThenSignedOut_whenNoToken() = runTest {
        val env = env()
        env.serverConfig.setServerUrl("https://logs.example.com")
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(SessionState.SignedOut, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun gate_signedIn_whenTokenPresent_evenIfMeFails() = runTest {
        // Server unreachable / profile never fetched: only a token and a URL are on disk.
        // SessionRepository has no network dependency, so gating can't wait on /me.
        val env = env()
        env.serverConfig.setServerUrl("http://10.255.255.1")
        env.auth.setToken("Bearer offline")
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            val signedIn = awaitItem() as SessionState.SignedIn
            assertEquals("Bearer offline", signedIn.token)
            assertNull(signedIn.userIdx)
            assertNull(signedIn.permission)
            expectNoEvents()
        }
        assertEquals("Bearer offline", repo.currentToken())
    }

    @Test
    fun signIn_storesProfileAndUpdatesKey() = runTest {
        val env = env()
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(SessionState.SignedOut, awaitItem())

            repo.signIn("logs.example.com", "Bearer t", UserDTO(idx = 7, username = "bob", permission = 2))
            var state = awaitItem()
            while (state !is SessionState.SignedIn || state.userIdx != 7) state = awaitItem()
            assertEquals("http://logs.example.com/", state.serverUrl)
            assertEquals("bob", state.username)
            assertEquals(2, state.permission)
            assertEquals(SessionKey.of("http://logs.example.com/", 7), state.key)

            repo.updateProfile(UserDTO(idx = 7, username = "bobby", permission = 3))
            state = awaitItem()
            while (state !is SessionState.SignedIn || state.permission != 3) state = awaitItem()
            assertEquals("bobby", state.username)
            assertEquals("a profile update never changes the key", SessionKey.of("http://logs.example.com/", 7), state.key)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("http://logs.example.com/", env.baseUrl.getBaseUrl())
        assertEquals("Bearer t", repo.currentToken())
    }

    @Test
    fun signOut_clearsTokenKeepsServerUrl() = runTest {
        val env = env()
        env.serverConfig.setServerUrl("https://logs.example.com")
        env.auth.setToken("Bearer abc")
        env.auth.setUserProfile(UserDTO(idx = 3, username = "alice", permission = 1))
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertTrue(awaitItem() is SessionState.SignedIn)
            repo.signOut()
            assertEquals(SessionState.SignedOut, awaitItem())
            expectNoEvents()
        }
        assertNull(env.auth.token.first())
        assertNull(repo.currentToken())
        assertEquals("https://logs.example.com/", env.serverConfig.serverUrl.first())
        assertEquals("https://logs.example.com/", env.baseUrl.getBaseUrl())
    }

    @Test
    fun signOut_thenOtherUserLogsIn_neverCarriesPreviousIdentity() = runTest {
        val env = env()
        val url = "https://logs.example.com/"
        env.serverConfig.setServerUrl(url)
        env.auth.setSession("Bearer alice", UserDTO(idx = 1, username = "alice", permission = 1))
        val repo = env.session()
        val aliceKey = SessionKey.of(url, 1)

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(aliceKey, (awaitItem() as SessionState.SignedIn).key)

            repo.signOut()
            assertEquals(SessionState.SignedOut, awaitItem())
            val stored = env.auth.stored.first()
            assertNull(stored.username)
            assertNull(stored.userIdx)

            // Old login path: token + username in one transaction.
            env.auth.setTokenAndUsername("Bearer bob", "bob")
            val bob = awaitItem() as SessionState.SignedIn
            assertEquals("Bearer bob", bob.token)
            assertEquals("bob", bob.username)
            assertNull(bob.userIdx)
            assertEquals(SessionKey.of(url, null, "bob"), bob.key)
            assertNotEquals(aliceKey, bob.key)
            expectNoEvents()
        }
    }

    @Test
    fun signOut_returnsOnlyAfterStateLeftSignedIn() = runTest {
        val env = env(authDelayMs = 500)
        env.serverConfig.setServerUrl("https://logs.example.com")
        env.auth.setSession("Bearer alice", UserDTO(idx = 1, username = "alice", permission = 1))
        val repo = env.session()
        repo.state.first { it is SessionState.SignedIn }

        repo.signOut()

        assertNull("no request after signOut() may carry the old token", repo.currentToken())
        assertEquals(SessionState.SignedOut, repo.state.value)
    }

    @Test
    fun signIn_returnsOnlyAfterStateShowsNewSession() = runTest {
        val env = env(authDelayMs = 500)
        env.serverConfig.setServerUrl("https://a.example.com")
        env.auth.setSession("Bearer alice", UserDTO(idx = 1, username = "alice", permission = 1))
        val repo = env.session()
        repo.state.first { it is SessionState.SignedIn }

        repo.signIn("https://b.example.com", "Bearer bob", UserDTO(idx = 2, username = "bob", permission = 1))
        var state = repo.state.value as SessionState.SignedIn
        assertEquals("Bearer bob", state.token)
        assertEquals(SessionKey.of("https://b.example.com/", 2), state.key)

        repo.signIn("https://b.example.com", "Bearer carol", "carol")
        state = repo.state.value as SessionState.SignedIn
        assertEquals("Bearer carol", state.token)
        assertEquals(SessionKey.of("https://b.example.com/", null, "carol"), state.key)
    }

    @Test
    fun sessionKey_stableForWholeSignIn_whenProfileArrivesOrChanges() = runTest {
        val env = env()
        val url = "https://logs.example.com/"
        val repo = env.session()
        repo.state.first { it != SessionState.Loading }

        // Profile unknown at sign-in: the key is derived from the username once.
        repo.signIn(url, "Bearer t", "alice")
        val key = (repo.state.value as SessionState.SignedIn).key
        assertEquals(SessionKey.of(url, null, "alice"), key)

        // The idx arrives later, with a different server username.
        repo.updateProfile(UserDTO(idx = 7, username = "Alice2", permission = 2))
        var state = repo.state.first { it is SessionState.SignedIn && it.userIdx == 7 } as SessionState.SignedIn
        assertEquals(key, state.key)
        assertEquals(2, state.permission)

        // A username written straight to the stored auth (pre-rework path).
        env.auth.setUsername("someone-else")
        state = repo.state.first { it is SessionState.SignedIn && it.username == "someone-else" } as SessionState.SignedIn
        assertEquals(key, state.key)

        // A restart reads the persisted key instead of deriving one from the newer profile.
        val restarted = env.session()
        assertEquals(key, (restarted.state.first { it != SessionState.Loading } as SessionState.SignedIn).key)
    }

    @Test
    fun sessionKey_newToken_newKey() = runTest {
        val env = env()
        val url = "https://logs.example.com/"
        val repo = env.session()
        repo.state.first { it != SessionState.Loading }

        repo.signIn(url, "Bearer one", UserDTO(idx = 1, username = "alice", permission = 1))
        assertEquals(SessionKey.of(url, 1), (repo.state.value as SessionState.SignedIn).key)

        repo.signIn(url, "Bearer two", UserDTO(idx = 2, username = "bob", permission = 1))
        assertEquals(SessionKey.of(url, 2), (repo.state.value as SessionState.SignedIn).key)

        // Old login path, without signing out first: the new token still gets its own key.
        env.auth.setTokenAndUsername("Bearer three", "carol")
        val carol = repo.state.first { it is SessionState.SignedIn && it.token == "Bearer three" } as SessionState.SignedIn
        assertEquals(SessionKey.of(url, null, "carol"), carol.key)
        assertEquals(carol.key.value, env.auth.stored.first().sessionKey)
    }

    @Test
    fun sessionKey_legacyTokenWithoutKey_derivedOnceAndPersisted() = runTest {
        val env = env()
        val url = "https://logs.example.com/"
        env.serverConfig.setServerUrl(url)
        env.auth.setToken("Bearer legacy")
        env.auth.setUserProfile(UserDTO(idx = 3, username = "alice", permission = 1))
        assertNull(env.auth.stored.first().sessionKey)
        val repo = env.session()

        val legacy = repo.state.first { it != SessionState.Loading } as SessionState.SignedIn
        assertEquals(SessionKey.of(url, 3), legacy.key)
        assertEquals(legacy.key.value, env.auth.stored.first { it.sessionKey != null }.sessionKey)

        repo.updateProfile(UserDTO(idx = 9, username = "alice", permission = 1))
        val updated = repo.state.first { it is SessionState.SignedIn && it.userIdx == 9 } as SessionState.SignedIn
        assertEquals(legacy.key, updated.key)
    }

    @Test
    fun updateProfile_forAnotherSession_isDropped() = runTest {
        val env = env()
        val url = "https://logs.example.com/"
        val repo = env.session()
        repo.state.first { it != SessionState.Loading }
        repo.signIn(url, "Bearer bob", UserDTO(idx = 2, username = "bob", permission = 1))

        // A /me answer for a session that has since been replaced must not land on this one.
        repo.updateProfile(UserDTO(idx = 1, username = "alice", permission = 100), SessionKey.of(url, 1))
        assertEquals(1, env.auth.stored.first().permission)

        repo.updateProfile(UserDTO(idx = 2, username = "bob", permission = 50), SessionKey.of(url, 2))
        assertEquals(50, (repo.state.first { it is SessionState.SignedIn && it.permission == 50 } as SessionState.SignedIn).permission)
    }

    @Test
    fun signOut_onlyIfToken_ignoresOtherToken() = runTest {
        val env = env()
        val repo = env.session()
        repo.state.first { it != SessionState.Loading }
        repo.signIn("https://logs.example.com/", "Bearer new", UserDTO(idx = 2, username = "bob", permission = 1))

        assertFalse(repo.signOut(onlyIfToken = "Bearer old"))
        assertEquals("Bearer new", repo.currentToken())

        assertTrue(repo.signOut(onlyIfToken = "Bearer new"))
        assertNull(repo.currentToken())
    }

    @Test
    fun gate_notStuck_whenConfigStoreUnreadable_andTokenReadable() = runTest {
        val env = env(brokenConfig = true)
        env.auth.setToken("Bearer abc")
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            val signedIn = awaitItem() as SessionState.SignedIn
            assertEquals("Bearer abc", signedIn.token)
            assertEquals("", signedIn.serverUrl)
            expectNoEvents()
        }
        withTimeout(1_000) { env.baseUrl.awaitLoaded() }
        assertNull(env.baseUrl.getBaseUrl())
    }

    @Test
    fun gate_notStuck_whenAuthStoreUnreadable() = runTest {
        val env = env(brokenAuth = true)
        env.serverConfig.setServerUrl("https://logs.example.com")
        val repo = env.session()

        repo.state.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(SessionState.SignedOut, awaitItem())
            expectNoEvents()
        }
        withTimeout(1_000) { env.baseUrl.awaitLoaded() }
        assertEquals("https://logs.example.com/", env.baseUrl.getBaseUrl())
    }

    @Test
    fun sessionKey_differsByServerAndUser() {
        val base = SessionKey.of("https://a.example.com/", 1)
        assertNotEquals(base, SessionKey.of("https://a.example.com/", 2))
        assertNotEquals(base, SessionKey.of("https://b.example.com/", 1))
        assertNotEquals(
            SessionKey.of("https://a.example.com/", null, "alice"),
            SessionKey.of("https://a.example.com/", null, "bob"),
        )
        assertNotEquals(base, SessionKey.of("https://a.example.com/", null, "1"))
    }

    @Test
    fun sessionKey_stableForSameInputs() {
        val key = SessionKey.of("https://a.example.com/", 1)
        assertEquals(key, SessionKey.of("https://a.example.com/", 1))
        assertEquals(key, SessionKey.of("https://a.example.com", 1)) // normalized before hashing
        assertTrue(key.value.matches(Regex("[0-9a-f]{16}")))
        // sha256("https://a.example.com/|1"), first 16 hex chars
        assertEquals(sha256Prefix("https://a.example.com/|1"), key.value)
        assertEquals(sha256Prefix("https://a.example.com/|u:alice"), SessionKey.of("https://a.example.com/", null, "alice").value)
    }

    private fun sha256Prefix(input: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
}
