package com.logflare.android.data.store

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
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

/** The signed-in user's own profile (`GET /user/me`). Push registration is not its business. */
@Singleton
class AccountStore internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    store: CacheStore,
    scope: CoroutineScope,
    clock: () -> Long,
) {
    @Inject
    constructor(
        api: LogflareApi,
        sessionRepository: SessionRepository,
        store: CacheStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(api, sessionRepository.state, store, scope, System::currentTimeMillis)

    private val resource = CachedResource("me", UserDTO.serializer(), store, clock, scope) { session ->
        val token = sessions.tokenFor(session)
        api.getme(token).let { requireData(it.success, it.message, it.error_code, it.data) }
    }

    fun me(force: Boolean = false): Flow<Resource<Member>> =
        sessions.whileSignedIn { session -> resource.stream(session, force).map { r -> r.mapData { it.toMember() } } }
}
