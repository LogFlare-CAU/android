package com.example.logflare.core.network.host

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * OkHttp interceptor that rewrites request URL host/scheme/port using a dynamic base URL
 * supplied by [BaseUrlProvider]. Path and query of the original request are preserved so
 * that API interface definitions remain unchanged.
 *
 * Security considerations: Only http/https schemes are permitted. If the provided base URL
 * is malformed or uses an unsupported scheme the original request is executed unmodified.
 */
class HostSelectionInterceptor(
    private val baseUrlProvider: BaseUrlProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val override = baseUrlProvider.getBaseUrl()?.trim()?.takeIf { it.isNotEmpty() }
        if (override.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val normalized = if (override.endsWith("/")) override else "$override/"
        val newBase = normalized.toHttpUrlOrNull()
        if (newBase == null || (newBase.scheme != "http" && newBase.scheme != "https")) {
            return chain.proceed(original)
        }

        val oldUrl = original.url
        val rebuilt = oldUrl.newBuilder()
            .scheme(newBase.scheme)
            .host(newBase.host)
            .port(newBase.port)
            .build()

        val newRequest = original.newBuilder().url(rebuilt).build()
        return chain.proceed(newRequest)
    }
}
