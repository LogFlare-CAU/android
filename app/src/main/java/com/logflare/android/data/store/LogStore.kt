package com.logflare.android.data.store

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventDetailDTO
import com.example.logflare.core.model.LogEventPageDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogEventDetail
import com.logflare.android.data.store.model.LogPage
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.toLogEventDetail
import com.logflare.android.data.store.model.toLogPage
import com.logflare.android.di.ApplicationScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Grouped log events. Each [LogQuery] has its own cache entry holding every page loaded so far
 * plus the cursor of the next one; [loadMore] appends to it.
 */
@Singleton
class LogStore internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) {
    @Inject
    constructor(
        api: LogflareApi,
        sessionRepository: SessionRepository,
        store: CacheStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(api, sessionRepository.state, store, scope, System::currentTimeMillis)

    private val pages = ConcurrentHashMap<String, CachedResource<LogEventPageDTO>>()
    private val details = ConcurrentHashMap<Int, CachedResource<LogEventDetailDTO>>()
    private val loadMoreLock = Mutex()

    /** The newest error groups (ERROR and up), first page only. */
    fun recent(limit: Int = 5): Flow<Resource<List<LogEvent>>> =
        events(RECENT_QUERY).map { r -> r.mapData { it.items.take(limit) } }

    fun events(query: LogQuery): Flow<Resource<LogPage>> =
        sessions.whileSignedIn { session ->
            pageResource(query).stream(session).map { r -> r.mapData { it.toLogPage() } }
        }

    /**
     * Fetches the page after the cached ones and appends it, updating open [events] streams.
     * Does nothing when there is no cached first page or no next page.
     */
    suspend fun loadMore(query: LogQuery): Result<Unit> = attempt {
        val session = sessions.signedIn().key
        val resource = pageResource(query)
        loadMoreLock.withLock {
            // Taken before the request: a sign-out while it runs drops the merged page.
            val generation = store.generation(session)
            val cached = store.read(session, eventsKey(query), LogEventPageDTO.serializer()) ?: return@withLock
            val cursor = cached.value.nextCursor ?: return@withLock
            val next = fetchPage(session, query, cursor)
            val seen = cached.value.items.mapTo(HashSet()) { it.id }
            val merged = LogEventPageDTO(cached.value.items + next.items.filter { it.id !in seen }, next.nextCursor)
            // Keep the first page's fetchedAt: staleness is about when the list was (re)loaded.
            resource.put(session, merged, fetchedAt = cached.fetchedAt, generation = generation)
        }
    }

    /** One event row with its group (`GET /log/events/{id}`). */
    fun event(id: Int): Flow<Resource<LogEventDetail>> =
        sessions.whileSignedIn { session ->
            detailResource(id).stream(session).map { r -> r.mapData { it.toLogEventDetail() } }
        }

    private fun pageResource(query: LogQuery): CachedResource<LogEventPageDTO> {
        val key = eventsKey(query)
        return pages.getOrPut(key) {
            CachedResource(key, LogEventPageDTO.serializer(), store, clock, scope) { session ->
                fetchPage(session, query, cursor = null)
            }
        }
    }

    private fun detailResource(id: Int): CachedResource<LogEventDetailDTO> =
        details.getOrPut(id) {
            CachedResource("event:$id", LogEventDetailDTO.serializer(), store, clock, scope) { session ->
                sessions.tokenFor(session)
                api.getLogEvent(id)
                    .let { requireData(it.success, it.message, it.errorCode, it.data) }
                    .also { it.toLogEventDetail() } // Fails the fetch, not the stream, on bad data.
            }
        }

    private suspend fun fetchPage(session: SessionKey, query: LogQuery, cursor: String?): LogEventPageDTO {
        sessions.tokenFor(session) // The interceptor adds the header; make sure it is this session's.
        return api.getLogEvents(
            projectId = query.projectId,
            minLevel = query.apiMinLevel,
            logfileId = query.logFileId,
            query = query.apiText,
            sort = query.sort.api,
            cursor = cursor,
        )
            .let { requireData(it.success, it.message, it.errorCode, it.data) }
            .also { it.toLogPage() } // Fails the fetch, not the stream, on bad data.
    }

    private fun eventsKey(query: LogQuery) = "events:" + query.hashKey()

    private companion object {
        val RECENT_QUERY = LogQuery(minLevel = LfLevel.Error)
    }
}
