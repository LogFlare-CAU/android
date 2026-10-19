package com.logflare.android.data.store

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventDTO
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

    /** The newest error groups (ERROR and up), first page only. [force] refetches it. */
    fun recent(limit: Int = 5, force: Boolean = false): Flow<Resource<List<LogEvent>>> =
        events(RECENT_QUERY, force).map { r -> r.mapData { it.items.take(limit) } }

    /** The first page (plus any pages [loadMore] appended). [force] refetches the first page. */
    fun events(query: LogQuery, force: Boolean = false): Flow<Resource<LogPage>> =
        sessions.whileSignedIn { session ->
            pageResource(query).stream(session, force).map { r -> r.mapData { it.toLogPage() } }
        }

    /**
     * Fetches the page after the cached ones and appends it, updating open [events] streams.
     * Does nothing when there is no cached first page or no next page.
     *
     * A first-page refresh wins over a load-more: a running one is awaited first, so the next
     * page continues the fresh list; one that lands while the next page is loading makes the
     * append a no-op (compare-and-set in [CachedResource.put]), so stale rows never come back.
     */
    suspend fun loadMore(query: LogQuery): Result<Unit> = attempt {
        val session = sessions.signedIn().key
        val resource = pageResource(query)
        loadMoreLock.withLock {
            resource.awaitInFlight(session)
            // Taken before the read: a sign-out from here on drops the merged page.
            val generation = store.generation(session)
            val cached = store.read(session, eventsKey(query), LogEventPageDTO.serializer()) ?: return@withLock
            val cursor = cached.value.nextCursor ?: return@withLock
            val next = fetchPage(session, query, cursor)
            // Keeps the first page's fetchedAt: staleness is about when the list was (re)loaded.
            resource.put(session, cached.value.append(next), expected = cached, generation = generation)
        }
    }

    /** One event row with its group (`GET /log/events/{id}`). [force] refetches it. */
    fun event(id: Int, force: Boolean = false): Flow<Resource<LogEventDetail>> =
        sessions.whileSignedIn { session ->
            detailResource(id).stream(session, force).map { r -> r.mapData { it.toLogEventDetail() } }
        }

    /**
     * Pull-to-refresh of [events] for [query]: refetches the first page (dropping pages
     * [loadMore] appended), even when fresh, and returns once done with the outcome.
     */
    suspend fun refresh(query: LogQuery): Result<Unit> =
        sessions.refreshSignedIn { session -> pageResource(query).refresh(session) }

    /** Pull-to-refresh of [recent]; see [refresh]. */
    suspend fun refreshRecent(): Result<Unit> = refresh(RECENT_QUERY)

    /** Pull-to-refresh of [event]; see [refresh]. */
    suspend fun refreshEvent(id: Int): Result<Unit> =
        sessions.refreshSignedIn { session -> detailResource(id).refresh(session) }

    /**
     * Drops [projectId]'s groups from the cached first pages (including [recent]'s), so lists and
     * Home stop showing a deleted project's errors. Open [events] streams update; pages not
     * loaded in this process are replaced by their next refetch.
     */
    suspend fun forgetProject(projectId: Int) {
        val session = sessions.value as? SessionState.SignedIn ?: return
        pageResource(RECENT_QUERY) // Make sure the Home entry is among those scanned.
        for ((key, resource) in pages.entries.toList()) {
            val generation = store.generation(session.key)
            val cached = store.read(session.key, key, LogEventPageDTO.serializer()) ?: continue
            val kept = cached.value.items.filter { it.projectId != projectId }
            if (kept.size == cached.value.items.size) continue
            resource.put(session.key, cached.value.copy(items = kept), expected = cached, generation = generation)
        }
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
                sessions.fetchAs(session) { api.getLogEvent(id) }
                    .let { requireData(it.success, it.message, it.errorCode, it.data) }
                    .also { it.toLogEventDetail() } // Fails the fetch, not the stream, on bad data.
            }
        }

    private suspend fun fetchPage(session: SessionKey, query: LogQuery, cursor: String?): LogEventPageDTO =
        sessions.fetchAs(session) {
            api.getLogEvents(
                projectId = query.projectId,
                minLevel = query.apiMinLevel,
                logfileId = query.logFileId,
                query = query.apiText,
                sort = query.sort.api,
                cursor = cursor,
            )
        }
            .let { requireData(it.success, it.message, it.errorCode, it.data) }
            .also { it.toLogPage() } // Fails the fetch, not the stream, on bad data.

    /**
     * Appends [next], skipping groups already listed. A group is (project, type, message), not
     * the row id: its id changes when a new row arrives between pages.
     */
    private fun LogEventPageDTO.append(next: LogEventPageDTO): LogEventPageDTO {
        val seen = items.mapTo(HashSet()) { it.groupKey() }
        return LogEventPageDTO(items + next.items.filter { it.groupKey() !in seen }, next.nextCursor)
    }

    private fun LogEventDTO.groupKey() = Triple(projectId, errortype, message)

    private fun eventsKey(query: LogQuery) = "events:" + query.hashKey()

    private companion object {
        val RECENT_QUERY = LogQuery(minLevel = LfLevel.Error)
    }
}
