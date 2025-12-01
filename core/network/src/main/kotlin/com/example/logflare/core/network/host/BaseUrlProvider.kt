package com.example.logflare.core.network.host

/**
 * Provides a dynamic base URL selected by the user (e.g. self-hosted server).
 * Returns null when no override has been configured; in that case the retrofit
 * fallback base URL is used.
 */
interface BaseUrlProvider {
    fun getBaseUrl(): String?
}
