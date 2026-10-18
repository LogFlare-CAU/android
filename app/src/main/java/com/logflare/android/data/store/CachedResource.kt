package com.logflare.android.data.store

import com.logflare.android.data.session.SessionKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val events = MutableSharedFlow<FetchEvent<T>>(extraBufferCapacity = EVENT_BUFFER)

    fun stream(session: SessionKey, force: Boolean = false): Flow<Resource<T>> = flow {
        val cached = store.read(session, key, serializer)
        val needsFetch = force || cached == null || isStale(cached)
        var current = Resource(cached?.value, refreshing = needsFetch, error = null, fetchedAt = cached?.fetchedAt)
        emit(current)
        events
            // Start (or join) the fetch only once subscribed, so its result can't be missed.
            .onSubscription { if (needsFetch) fetchShared(session) }
            .filter { it.session == session }
            .collect { event ->
                val next = current.apply(event)
                if (next != current) {
                    current = next
                    emit(next)
                }
            }
    }

    /**
     * Fetches now (joining an in-flight fetch if any) and updates the cache and every active
     * [stream] of [session]. Failures do not throw; they surface as `error` on those streams.
     */
    suspend fun refresh(session: SessionKey) {
        fetchShared(session).await()
    }

    private fun isStale(cached: Cached<T>): Boolean {
        val age = clock() - cached.fetchedAt
        return age < 0 || age >= maxAgeMs
    }

    private suspend fun fetchShared(session: SessionKey): Deferred<Result<Cached<T>>> = mutex.withLock {
        val id = "${session.value}|$key"
        inFlight[id]?.takeIf { it.isActive }?.let { return@withLock it }
        scope.async {
            events.emit(FetchEvent.Started(session))
            val result = try {
                val value = fetch(session)
                val fetchedAt = clock()
                store.write(session, key, value, serializer, fetchedAt)
                Result.success(Cached(value, fetchedAt))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }
            // Still the map's entry: a new one is only created once this one is no longer active.
            mutex.withLock { inFlight.remove(id) }
            events.emit(
                result.fold(
                    onSuccess = { FetchEvent.Succeeded(session, it) },
                    onFailure = { FetchEvent.Failed(session, it) },
                ),
            )
            result
        }.also { inFlight[id] = it }
    }

    private fun Resource<T>.apply(event: FetchEvent<T>): Resource<T> = when (event) {
        is FetchEvent.Started -> copy(refreshing = true, error = null)
        is FetchEvent.Succeeded -> Resource(event.cached.value, refreshing = false, error = null, fetchedAt = event.cached.fetchedAt)
        is FetchEvent.Failed -> copy(refreshing = false, error = event.error)
    }

    companion object {
        const val DEFAULT_MAX_AGE_MS = 30_000L
        private const val EVENT_BUFFER = 64
    }
}

private sealed interface FetchEvent<out T> {
    val session: SessionKey

    data class Started(override val session: SessionKey) : FetchEvent<Nothing>
    data class Succeeded<T>(override val session: SessionKey, val cached: Cached<T>) : FetchEvent<T>
    data class Failed(override val session: SessionKey, val error: Throwable) : FetchEvent<Nothing>
}
