package com.example.logflare.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Runs [HttpUnauthorizedAction] once per 401 response. Registered last on the OkHttp client
 * so it observes the server response before outer interceptors.
 */
class UnauthorizedResponseInterceptor(
    private val onUnauthorized: HttpUnauthorizedAction,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            onUnauthorized.onUnauthorized()
        }
        return response
    }
}
