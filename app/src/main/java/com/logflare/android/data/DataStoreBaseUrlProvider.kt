package com.logflare.android.data

import com.example.logflare.core.network.host.MutableBaseUrlProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App implementation of [MutableBaseUrlProvider] backed by DataStore.
 *
 * [setBaseUrl] publishes the normalized URL to an in-memory cache immediately
 * (visible to OkHttp before persistence returns), then persists that exact value.
 * Concurrent [setBaseUrl] calls are serialized with a [Mutex].
 *
 * There is intentionally no cache-clear API: clearing would require a modeled
 * persist-and-ack path; callers leave the last selected/persisted URL in place.
 */
@Singleton
class DataStoreBaseUrlProvider internal constructor(
    private val serverConfigRepository: ServerConfigRepository,
    scope: CoroutineScope,
) : MutableBaseUrlProvider {

    @Inject
    constructor(serverConfigRepository: ServerConfigRepository) :
        this(serverConfigRepository, CoroutineScope(Dispatchers.IO))

    private val coordinator = BaseUrlCacheCoordinator()
    private val writeMutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()

    init {
        scope.launch {
            serverConfigRepository.serverUrl
                // An unreadable store must not block startup forever; the cache simply stays empty.
                .catch { loaded.complete(Unit) }
                .collectLatest { url ->
                    coordinator.onPersistedEmission(url)
                    loaded.complete(Unit)
                }
        }
    }

    override fun getBaseUrl(): String? = coordinator.current()

    /** Suspends until the persisted base URL has been read into the cache at least once. */
    suspend fun awaitLoaded() = loaded.await()

    override suspend fun setBaseUrl(url: String) {
        writeMutex.withLock {
            val normalized = serverConfigRepository.normalize(url)
            val previous = coordinator.beginLocalWrite(normalized)
            try {
                serverConfigRepository.setNormalizedServerUrl(normalized)
            } catch (e: Exception) {
                coordinator.rollback(previous)
                throw e
            }
        }
    }
}
