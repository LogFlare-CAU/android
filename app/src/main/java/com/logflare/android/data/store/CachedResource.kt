package com.logflare.android.data.store

import com.logflare.android.data.session.SessionKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * One cache-first value (e.g. the project list) for any session.
 *
 * [stream] emits the cached value immediately, then refreshes in the background when the value
 * is missing, older than [maxAgeMs] or `force` is set. Fetches are de-duplicated per session:
 * concurrent collectors (and [refresh] calls) share one in-flight request. The fetch runs in
 * [scope], so a collector going away never cancels a request others are waiting on, and the
 * result is still written to the cache.
 *
 * A stream keeps following later fetches for its session (e.g. a [refresh] after a mutation),
 * so it never completes on its own. A failed fetch keeps the last data and exposes the error.
 */
class CachedResource<T>(
    private val key: String,
    private val serializer: KSerializer<T>,
    private val store: CacheStore,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    private val maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
    private val fetch: suspend (SessionKey) -> T,
) {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Result<Cached<T>>>>()
    private var nextFetchId = 0L // guarded by mutex
    private val events = MutableSharedFlow<FetchEvent<T>>(extraBufferCapacity = EVENT_BUFFER)

    fun stream(session: SessionKey, force: Boolean = false): Flow<Resource<T>> = flow {
        val cached = store.read(session, key, serializer)
        val needsFetch = force || cached == null || isStale(cached)
        var current = Resource(cached?.value, refreshing = needsFetch, error = null, fetchedAt = cached?.fetchedAt)
        var latestStarted = -1L
        emit(current)
        events
            // Start (or join) the fetch only once subscribed, so its result can't be missed.
            .onSubscription { if (needsFetch) fetchShared(session) }
            .filter { it.session == session }
            .collect { event ->
                when (event) {
                    is FetchEvent.Started -> latestStarted = maxOf(latestStarted, event.fetchId)
                    // A put() is not a fetch: it never ends one, so it is never "older" either.
                    is FetchEvent.Replaced -> Unit
                    // The end of an older fetch while a newer one runs: keep showing "refreshing".
                    else -> if (event.fetchId < latestStarted) return@collect
                }
                val next = current.apply(event)
                if (next != current) {
                    current = next
                    emit(next)
                }
            }
    }

    /**
     * Fetches now (joining an in-flight fetch if any) and updates the cache and every active
     * [stream] of [session]. Returns once the fetch is done, with its outcome; failures do not
     * throw and also surface as `error` on those streams.
     */
    suspend fun refresh(session: SessionKey): Result<Unit> = fetchShared(session).await().map { }

    /** Waits for the fetch of [session] that is running now, if any, without starting one. */
    suspend fun awaitInFlight(session: SessionKey) {
        mutex.withLock { inFlight[inFlightId(session)]?.takeIf { it.isActive } }?.await()
    }

    /**
     * Compare-and-set: replaces the cached value of [session] with [value] (e.g. a list with a
     * "load more" page appended) only if the cache still holds exactly [expected], i.e. no
     * fetch or other put landed since [expected] was read; then pushes it to every active
     * [stream] of [session] without ending an in-flight fetch's "refreshing". Like a fetch
     * result, it is dropped when [session] was cleared since [generation] was taken, so take
     * the generation before reading [expected].
     *
     * @return whether [value] was stored.
     */
    suspend fun put(
        session: SessionKey,
        value: T,
        expected: Cached<T>?,
        generation: Long,
        fetchedAt: Long = expected?.fetchedAt ?: clock(),
    ): Boolean {
        return mutex.withLock {
            if (store.generation(session) != generation) return@withLock false
            if (store.read(session, key, serializer) != expected) return@withLock false
            store.write(session, key, value, serializer, fetchedAt, generation)
            // Still under the mutex: a fetch that writes after us also announces after us.
            events.emit(FetchEvent.Replaced(session, Cached(value, fetchedAt)))
            true
        }
    }

    private fun isStale(cached: Cached<T>): Boolean {
        val age = clock() - cached.fetchedAt
        return age < 0 || age >= maxAgeMs
    }

    private suspend fun fetchShared(session: SessionKey): Deferred<Result<Cached<T>>> = mutex.withLock {
        val id = inFlightId(session)
        inFlight[id]?.takeIf { it.isActive }?.let { return@withLock it }
        val fetchId = nextFetchId++
        // Taken before the request so a clear() (sign-out) during it makes the write a no-op.
        val generation = store.generation(session)
        scope.async {
            val self = coroutineContext.job
            var result: Result<Cached<T>> = Result.failure(CancellationException("Fetch cancelled"))
            try {
                events.emit(FetchEvent.Started(session, fetchId))
                result = try {
                    val value = fetch(session)
                    val fetchedAt = clock()
                    // Under the mutex, so a put() compare-and-set can't interleave with it.
                    mutex.withLock { store.write(session, key, value, serializer, fetchedAt, generation) }
                    Result.success(Cached(value, fetchedAt))
                } catch (e: Throwable) {
                    // Only our own cancellation propagates; e.g. a withTimeout inside fetch is a failure.
                    if (e is CancellationException && !currentCoroutineContext().isActive) throw e
                    Result.failure(e)
                }
                result
            } finally {
                // Always release the map entry and the streams, even when cancelled.
                withContext(NonCancellable) {
                    mutex.withLock { if (inFlight[id] === self) inFlight.remove(id) }
                    events.emit(
                        result.fold(
                            onSuccess = { FetchEvent.Succeeded(session, fetchId, it) },
                            onFailure = { FetchEvent.Failed(session, fetchId, it) },
                        ),
                    )
                }
            }
        }.also { inFlight[id] = it }
    }

    private fun inFlightId(session: SessionKey) = "${session.value}|$key"

    private fun Resource<T>.apply(event: FetchEvent<T>): Resource<T> = when (event) {
        is FetchEvent.Started -> copy(refreshing = true, error = null)
        is FetchEvent.Succeeded -> Resource(event.cached.value, refreshing = false, error = null, fetchedAt = event.cached.fetchedAt)
        is FetchEvent.Failed -> copy(refreshing = false, error = event.error)
        is FetchEvent.Replaced -> copy(data = event.cached.value, fetchedAt = event.cached.fetchedAt)
    }

    companion object {
        const val DEFAULT_MAX_AGE_MS = 30_000L
        private const val EVENT_BUFFER = 64
    }
}

private sealed interface FetchEvent<out T> {
    val session: SessionKey

    /** Increases with every fetch a resource starts. */
    val fetchId: Long

    data class Started(override val session: SessionKey, override val fetchId: Long) : FetchEvent<Nothing>
    data class Succeeded<T>(override val session: SessionKey, override val fetchId: Long, val cached: Cached<T>) : FetchEvent<T>
    data class Failed(override val session: SessionKey, override val fetchId: Long, val error: Throwable) : FetchEvent<Nothing>

    /** A put(); not tied to any fetch. */
    data class Replaced<T>(override val session: SessionKey, val cached: Cached<T>) : FetchEvent<T> {
        override val fetchId: Long get() = -1
    }
}
