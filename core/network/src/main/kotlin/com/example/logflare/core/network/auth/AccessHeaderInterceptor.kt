package com.example.logflare.core.network.auth

import com.example.logflare.core.network.host.ProbeBaseUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the stored Cloudflare Access service token so a hostname behind Access
 * lets the request through. Headers already on the request are left alone.
 * Added after the logging interceptor so the secret is not written to logs.
 */
class AccessHeaderInterceptor(
    private val tokens: AccessServiceTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // A probe of a not-yet-chosen server must not carry the saved server's Access token.
        if (original.tag(ProbeBaseUrl::class.java) != null) return chain.proceed(original)
        if (original.header(CLIENT_ID) != null || original.header(CLIENT_SECRET) != null) {
            return chain.proceed(original)
        }
        val token = tokens.current() ?: return chain.proceed(original)
        if (token.clientId.isBlank() || token.secret.isBlank()) return chain.proceed(original)
        return chain.proceed(
            original.newBuilder()
                .header(CLIENT_ID, token.clientId)
                .header(CLIENT_SECRET, token.secret)
                .build()
        )
    }

    companion object {
        const val CLIENT_ID = "CF-Access-Client-Id"
        const val CLIENT_SECRET = "CF-Access-Client-Secret"
    }
}
