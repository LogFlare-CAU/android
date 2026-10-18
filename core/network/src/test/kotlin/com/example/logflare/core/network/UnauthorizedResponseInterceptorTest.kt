package com.example.logflare.core.network

import com.example.logflare.core.network.auth.AuthHeaderInterceptor
import com.example.logflare.core.network.auth.AuthTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UnauthorizedResponseInterceptorTest {
    private lateinit var server: MockWebServer
    private val rejected = mutableListOf<String?>()

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After fun tearDown() = server.shutdown()

    /** Same order as NetworkModule: the 401 interceptor runs after the auth header was added. */
    private fun send(code: Int, token: String?, explicit: String? = null) {
        server.enqueue(MockResponse().setResponseCode(code))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(AuthTokenProvider { token }))
            .addInterceptor(UnauthorizedResponseInterceptor { rejected += it })
            .build()
        val req = Request.Builder().url(server.url("/x")).apply {
            if (explicit != null) header("Authorization", explicit)
        }.build()
        client.newCall(req).execute().close()
    }

    @Test fun passesTheRejectedAuthorizationHeader() {
        send(401, "Bearer abc")
        assertEquals(listOf<String?>("Bearer abc"), rejected)
    }

    @Test fun passesAnExplicitHeader() {
        send(401, "Bearer current", explicit = "Bearer old")
        assertEquals(listOf<String?>("Bearer old"), rejected)
    }

    @Test fun passesNullWithoutHeader() {
        send(401, null)
        assertEquals(listOf<String?>(null), rejected)
    }

    @Test fun ignoresOtherCodes() {
        send(200, "Bearer abc")
        send(403, "Bearer abc")
        assertEquals(emptyList<String?>(), rejected)
    }
}
