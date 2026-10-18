package com.example.logflare.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the stored token as the `Authorization` header when the request does not already
 * carry one and a token is available. The token is sent verbatim.
 */
class AuthHeaderInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) return chain.proceed(original)
        val token = tokenProvider.currentToken()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(original)
        return chain.proceed(original.newBuilder().header("Authorization", token).build())
    }
}
