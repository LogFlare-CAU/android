package com.example.logflare.core.network.host

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Request tag (Retrofit `@Tag`) that sends one call to [url] instead of the saved server, for
 * reading public data from a server the user has only typed so far. It is never sent on the wire.
 *
 * Only https is honored. The saved server's session token and Access service token are not
 * attached to a tagged request, so neither can reach the probed host.
 */
class ProbeBaseUrl(val url: String) {
    /** The https base to use, or null when [url] is not a valid https URL. */
    fun httpsBase() = url.trim().let { if (it.endsWith("/")) it else "$it/" }
        .toHttpUrlOrNull()
        ?.takeIf { it.scheme == "https" }
}
