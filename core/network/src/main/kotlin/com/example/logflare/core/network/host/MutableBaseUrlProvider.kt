package com.example.logflare.core.network.host

/**
 * Mutable extension of [BaseUrlProvider] for selecting a server base URL.
 *
 * Intentionally has no clear/reset API. Clearing would need a modeled persist-and-ack
 * path to keep cache and DataStore consistent; the last selected URL remains until
 * replaced by another [setBaseUrl].
 */
interface MutableBaseUrlProvider : BaseUrlProvider {
    suspend fun setBaseUrl(url: String)
}
