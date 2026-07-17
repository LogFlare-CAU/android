package com.logflare.android.data

/**
 * Coordinates the in-memory base URL cache with pending local writes.
 *
 * While a local write is pending, DataStore emissions that are null, stale, or
 * otherwise non-matching are ignored so they cannot overwrite the selected URL.
 * A matching emission acknowledges the write. After acknowledgement, normal
 * persisted emissions may update the cache again.
 *
 * Thread-safe for concurrent [current] / [onPersistedEmission] / write APIs.
 */
internal class BaseUrlCacheCoordinator {
    private val lock = Any()
    private var cached: String? = null
    private var pending: String? = null

    fun current(): String? = synchronized(lock) { cached }

    /**
     * Marks [normalized] as the pending desired URL and publishes it immediately.
     * Returns the previous cached value for rollback on persistence failure.
     */
    fun beginLocalWrite(normalized: String): String? = synchronized(lock) {
        val previous = cached
        pending = normalized
        cached = normalized
        previous
    }

    /**
     * Applies a DataStore emission. Ignores null/stale/non-matching values while
     * a local write is pending; matching value clears pending without changing
     * away from the desired URL.
     */
    fun onPersistedEmission(url: String?): Unit = synchronized(lock) {
        val pendingUrl = pending
        if (pendingUrl != null) {
            if (url == pendingUrl) {
                pending = null
            }
            return
        }
        cached = url
    }

    /** Restores [previous] and clears pending after a failed persistence. */
    fun rollback(previous: String?): Unit = synchronized(lock) {
        cached = previous
        pending = null
    }
}
