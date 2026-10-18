package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.CacheStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** A push that was shown or held by quiet hours. */
@Serializable
data class HistoryEntry(
    val errorId: Int,
    val projectId: Int?,
    val type: String,
    val level: LfLevel,
    val message: String,
    val receivedAt: Long,
    val read: Boolean,
)

/**
 * Recent pushes of the signed-in session, newest first, at most [CAPACITY], persisted per
 * [SessionKey] in [CacheStore]. Empty while signed out; [add] is then ignored.
 */
@Singleton
class NotificationHistory internal constructor(
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(sessionRepository: SessionRepository, store: CacheStore) : this(sessionRepository.state, store)

    /** Bumped after every write so [entries] re-reads the store. */
    private val version = MutableStateFlow(0L)
    private val lock = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<List<HistoryEntry>> =
        sessions.map { (it as? SessionState.SignedIn)?.key }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                if (key == null) flowOf(emptyList()) else version.map { read(key) }
            }
            .distinctUntilChanged()

    val unreadCount: Flow<Int> = entries.map { list -> list.count { !it.read } }.distinctUntilChanged()

    suspend fun add(e: HistoryEntry) = edit { (listOf(e) + it).take(CAPACITY) }

    suspend fun markRead(errorId: Int) = edit { list -> list.map { if (it.errorId == errorId) it.copy(read = true) else it } }

    suspend fun markAllRead() = edit { list -> list.map { it.copy(read = true) } }

    suspend fun clear(session: SessionKey) {
        lock.withLock { write(session, emptyList()) }
    }

    /** Read-modify-write of the signed-in session's list. No-op when signed out. */
    private suspend fun edit(transform: (List<HistoryEntry>) -> List<HistoryEntry>) {
        val key = (sessions.value as? SessionState.SignedIn)?.key ?: return
        lock.withLock { write(key, transform(read(key))) }
    }

    private suspend fun read(key: SessionKey): List<HistoryEntry> =
        store.read(key, KEY, serializer)?.value.orEmpty()

    private suspend fun write(key: SessionKey, list: List<HistoryEntry>) {
        store.write(key, KEY, list, serializer, clock())
        version.value++
    }

    private companion object {
        const val KEY = "notification-history"
        const val CAPACITY = 50
        val serializer = ListSerializer(HistoryEntry.serializer())
    }
}
