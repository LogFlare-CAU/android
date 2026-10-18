package com.logflare.android.data.session

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.auth.AuthTokenProvider
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.DataStoreBaseUrlProvider
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.data.StoredAuth
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SessionState {
    /** Stored session not read from disk yet. The splash screen stays up while in this state. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(
        /**
         * Normalized server URL. Can be empty for legacy installs that stored a token but never
         * a server URL; requests then fail fast in host selection.
         */
        val serverUrl: String,
        /** Stored verbatim, already including the `Bearer ` prefix. */
        val token: String,
        val username: String,
        val userIdx: Int?,
        val permission: Int?,
        val key: SessionKey,
    ) : SessionState
}

/**
 * Single source of truth for "who is signed in, to which server".
 *
 * [state] starts as [SessionState.Loading] and leaves it only after both the base URL cache
 * ([DataStoreBaseUrlProvider.awaitLoaded]) and the auth store have been read. It is derived from
 * disk only — no network call is involved, so an unreachable server can never hold the gate or
 * sign the user out. Old screens keep reading/writing [AuthRepository] directly; their changes
 * flow into [state] as well.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val auth: AuthRepository,
    private val serverConfig: ServerConfigRepository,
    private val baseUrlProvider: DataStoreBaseUrlProvider,
    @ApplicationScope scope: CoroutineScope,
) : AuthTokenProvider {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        scope.launch {
            baseUrlProvider.awaitLoaded()
            // A store that can't be read is treated as empty instead of keeping the splash up forever.
            combine(
                serverConfig.serverUrl.catch { emit(null) },
                auth.stored.catch { emit(StoredAuth(null, null, null, null)) },
                ::toState,
            ).collect { _state.value = it }
        }
    }

    /**
     * [token] is stored verbatim and must already include the `Bearer ` prefix. Returns once
     * [state] is the new session, so the next request already carries the new token.
     */
    suspend fun signIn(serverUrl: String, token: String, user: UserDTO) {
        switchServer(serverUrl)
        auth.setSession(token, user)
        awaitSignedIn(token, SessionKey.of(serverUrl, user.idx))
    }

    /**
     * Sign-in whose profile couldn't be loaded: the session key falls back to [username] until
     * the profile is known. [token] must already include the `Bearer ` prefix.
     */
    suspend fun signIn(serverUrl: String, token: String, username: String) {
        switchServer(serverUrl)
        auth.setTokenAndUsername(token, username)
        awaitSignedIn(token, SessionKey.of(serverUrl, null, username))
    }

    private suspend fun switchServer(serverUrl: String) {
        // Never let a new server URL pair up with a previous user's token, even transiently.
        if (_state.value is SessionState.SignedIn) signOut()
        baseUrlProvider.setBaseUrl(serverUrl)
    }

    suspend fun updateProfile(user: UserDTO) {
        auth.setUserProfile(user)
    }

    /**
     * Clears the token and user identity; the server URL is kept for the next login. Returns once
     * [state] has left [SessionState.SignedIn], so no request after it carries the old token.
     */
    suspend fun signOut() {
        auth.clearToken()
        awaitState { it !is SessionState.SignedIn }
    }

    private suspend fun awaitSignedIn(token: String, key: SessionKey) =
        awaitState { it is SessionState.SignedIn && it.token == token && it.key == key }

    /**
     * [state] follows disk asynchronously; waits until it reflects a write just made. Bounded so a
     * concurrent write by someone else (old screens, the 401 handler) can't hang the caller.
     */
    private suspend fun awaitState(predicate: (SessionState) -> Boolean) {
        withTimeoutOrNull(STATE_WAIT_MS) { _state.first(predicate) }
    }

    private companion object {
        const val STATE_WAIT_MS = 5_000L
    }

    override fun currentToken(): String? = (_state.value as? SessionState.SignedIn)?.token

    private fun toState(serverUrl: String?, stored: StoredAuth): SessionState {
        val token = stored.token?.takeIf { it.isNotBlank() } ?: return SessionState.SignedOut
        val url = serverUrl.orEmpty()
        val username = stored.username.orEmpty()
        return SessionState.SignedIn(
            serverUrl = url,
            token = token,
            username = username,
            userIdx = stored.userIdx,
            permission = stored.permission,
            key = SessionKey.of(url, stored.userIdx, username),
        )
    }
}
