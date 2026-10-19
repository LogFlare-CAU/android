package com.example.logflare.core.network.host

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * OkHttp interceptor that rewrites request URL host/scheme/port using a dynamic base URL
 * supplied by [BaseUrlProvider]. Path and query of the original request are preserved so
 * that API interface definitions remain unchanged.
 *
 * Security considerations: Only http/https schemes are permitted. If the provided base URL
 * is missing, malformed or uses an unsupported scheme the request fails fast with
 * [ServerNotConfiguredException] rather than reaching the placeholder retrofit host.
 */
class HostSelectionInterceptor(
    private val baseUrlProvider: BaseUrlProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        original.tag(ProbeBaseUrl::class.java)?.let { return chain.proceed(rewrite(original, it.httpsBase())) }
        val override = baseUrlProvider.getBaseUrl()?.trim()?.takeIf { it.isNotEmpty() }
        if (override.isNullOrBlank()) throw ServerNotConfiguredException()

        val normalized = if (override.endsWith("/")) override else "$override/"
        val newBase = normalized.toHttpUrlOrNull()
        if (newBase == null || (newBase.scheme != "http" && newBase.scheme != "https")) {
            throw ServerNotConfiguredException()
        }
        return chain.proceed(rewrite(original, newBase))
    }

    private fun rewrite(original: Request, base: HttpUrl?): Request {
        if (base == null) throw ServerNotConfiguredException()
        val rebuilt = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return original.newBuilder().url(rebuilt).build()
    }
}
