package com.logflare.android.data.store

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.model.Member
import com.logflare.android.data.store.model.toMember
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * The signed-in user's own profile (`GET /user/me`). Push registration is not its business.
 *
 * Every successful fetch also refreshes the session's username, idx and role
 * ([SessionRepository.updateProfile]), so [SessionState.SignedIn] stays the one source of truth
 * for the role.
 */
@Singleton
class AccountStore internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    store: CacheStore,
    scope: CoroutineScope,
    /** Receives each fetched profile with the session it was fetched for. */
    private val onProfile: suspend (SessionKey, UserDTO) -> Unit,
    clock: () -> Long,
) {
    @Inject
    constructor(
        api: LogflareApi,
        sessionRepository: SessionRepository,
        store: CacheStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(
        api,
        sessionRepository.state,
        store,
        scope,
        { key, user -> sessionRepository.updateProfile(user, key) },
        System::currentTimeMillis,
    )

    private val resource = CachedResource("me", UserDTO.serializer(), store, clock, scope) { session ->
        val user = sessions.fetchAs(session) { api.getme(it) }.let { requireData(it.success, it.message, it.error_code, it.data) }
        // Best effort: a profile that can't be stored still is the answer to show.
        attempt { onProfile(session, user) }
        user
    }

    fun me(force: Boolean = false): Flow<Resource<Member>> =
        sessions.whileSignedIn { session -> resource.stream(session, force).map { r -> r.mapData { it.toMember() } } }

    /** Pull-to-refresh: fetches now, even when fresh, and returns once done with the outcome. */
    suspend fun refresh(): Result<Unit> = sessions.refreshSignedIn { resource.refresh(it) }
}
