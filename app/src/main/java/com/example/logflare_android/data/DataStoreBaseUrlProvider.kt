package com.example.logflare_android.data

import com.example.logflare.core.network.host.BaseUrlProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * App implementation of [BaseUrlProvider] backed by DataStore.
 * It maintains an in-memory cached value updated asynchronously so that
 * OkHttp interceptor calls are non-blocking.
 */
@Singleton
class DataStoreBaseUrlProvider @Inject constructor(
    serverConfigRepository: ServerConfigRepository
) : BaseUrlProvider {

    private val cached = AtomicReference<String?>(null)

    init {
        // Collect server URL changes; fire-and-forget application scope
        CoroutineScope(Dispatchers.IO).launch {
            serverConfigRepository.serverUrl.collectLatest { url ->
                cached.set(url)
            }
        }
    }

    override fun getBaseUrl(): String? = cached.get()
}
