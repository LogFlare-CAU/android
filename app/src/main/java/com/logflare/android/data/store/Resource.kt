package com.logflare.android.data.store

/**
 * What a screen renders for one cached resource.
 *
 * [data] is the last known value (cached or fresh) and is kept when a refresh fails; [error] then
 * holds the failure. [fetchedAt] is when [data] was fetched from the server (epoch millis).
 */
data class Resource<T>(
    val data: T?,
    val refreshing: Boolean,
    val error: Throwable?,
    val fetchedAt: Long?,
) {
    /** Nothing to show yet and a fetch is running: show a full loading state. */
    val isInitialLoad: Boolean get() = data == null && refreshing
}
