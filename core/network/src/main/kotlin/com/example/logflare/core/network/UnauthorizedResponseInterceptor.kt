package com.example.logflare.core.network

import com.example.logflare.core.network.host.ProbeBaseUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Runs [HttpUnauthorizedAction] once per 401 response, with the `Authorization` header that was
 * sent. Registered last on the OkHttp client so it sees the request as sent (header included)
 * and observes the server response before outer interceptors.
 */
class UnauthorizedResponseInterceptor(
    private val onUnauthorized: HttpUnauthorizedAction,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && request.tag(ProbeBaseUrl::class.java) == null) {
            onUnauthorized.onUnauthorized(request.header("Authorization"))
        }
        return response
    }
}
