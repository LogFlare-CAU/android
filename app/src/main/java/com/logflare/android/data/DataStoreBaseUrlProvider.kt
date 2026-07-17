package com.logflare.android.data

import com.example.logflare.core.network.host.MutableBaseUrlProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
class DataStoreBaseUrlProvider @Inject constructor(
    private val serverConfigRepository: ServerConfigRepository
) : MutableBaseUrlProvider {

    private val coordinator = BaseUrlCacheCoordinator()
    private val writeMutex = Mutex()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            serverConfigRepository.serverUrl.collectLatest { url ->
                coordinator.onPersistedEmission(url)
            }
        }
    }

    override fun getBaseUrl(): String? = coordinator.current()

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
