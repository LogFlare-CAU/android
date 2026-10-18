package com.logflare.android.data.store

import com.example.logflare.core.model.UserCreateParams
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserUpdateParams
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
import kotlinx.serialization.builtins.ListSerializer

/** All users of the server (admin screens) and user mutations. */
@Singleton
class MemberStore internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
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

    private val serializer = ListSerializer(UserDTO.serializer())

    private val resource = CachedResource(KEY, serializer, store, clock, scope) { session ->
        val token = sessions.tokenFor(session)
        api.getAllUsers(token).let { requireData(it.success, it.message, it.error_code, it.data) }
    }

    fun members(force: Boolean = false): Flow<Resource<List<Member>>> =
        sessions.whileSignedIn { session ->
            resource.stream(session, force).map { r -> r.mapData { users -> users.map { it.toMember() } } }
        }

    suspend fun add(username: String, password: String, permission: Int): Result<Member> = mutate { token ->
        api.createUser(token, UserCreateParams(username, permission, password))
            .let { requireData(it.success, it.message, it.error_code, it.data) }
            .toMember()
    }

    /** Null arguments are left unchanged. */
    suspend fun update(idx: Int, username: String?, password: String?, permission: Int?): Result<Member> = mutate { token ->
        api.updateUser(token, idx, UserUpdateParams(username = username, permission = permission, password = password))
            .let { requireData(it.success, it.message, it.error_code, it.data) }
            .toMember()
    }

    suspend fun delete(idx: Int): Result<Unit> = mutate { token ->
        api.deleteUser(token, idx).let { requireSuccess(it.success, it.message, it.error_code) }
    }

    /**
     * Whether a user named [name] (trimmed, case-insensitive) exists, checked against the cached
     * list. Without a cached list it loads it once; if that fails, the name counts as free and
     * the server has the final say on [add].
     */
    suspend fun isNameTaken(name: String): Boolean {
        val session = (sessions.value as? SessionState.SignedIn)?.key ?: return false
        val users = store.read(session, KEY, serializer)?.value
            ?: resource.refresh(session).let { store.read(session, KEY, serializer)?.value }
            ?: return false
        val wanted = name.trim()
        return users.any { it.username.equals(wanted, ignoreCase = true) }
    }

    /** Runs [block] with the session token, then refreshes the member list on success. */
    private suspend fun <T> mutate(block: suspend (token: String) -> T): Result<T> {
        val session = sessions.value as? SessionState.SignedIn ?: return Result.failure(NotSignedInException())
        return attempt { block(session.token) }.onSuccess { resource.refresh(session.key) }
    }

    private companion object {
        const val KEY = "members"
    }
}
