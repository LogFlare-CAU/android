package com.logflare.android.data

import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * App implementation of [BaseUrlProvider] backed by DataStore.
 * It maintains an in-memory cached value updated synchronously on [setBaseUrl]
 * and asynchronously from persisted DataStore collection so that OkHttp interceptor
 * calls are non-blocking.
 */
@Singleton
class DataStoreBaseUrlProvider @Inject constructor(
    private val serverConfigRepository: ServerConfigRepository
) : MutableBaseUrlProvider {

    private val cached = AtomicReference<String?>(null)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            serverConfigRepository.serverUrl.collectLatest { url ->
                if (url != null) {
                    cached.set(url)
                }
            }
        }
    }

    override fun getBaseUrl(): String? = cached.get()

    override suspend fun setBaseUrl(url: String) {
        val normalized = serverConfigRepository.normalize(url)
        cached.set(normalized)
        serverConfigRepository.setNormalizedServerUrl(normalized)
    }
}
