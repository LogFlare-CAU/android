package com.example.logflare.core.network

import com.example.logflare.core.network.auth.AuthHeaderInterceptor
import com.example.logflare.core.network.auth.AuthTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthHeaderInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        server.enqueue(MockResponse().setResponseCode(200))
    }

    @After fun tearDown() = server.shutdown()

    private fun send(token: String?, explicit: String? = null): String? {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(AuthTokenProvider { token }))
            .build()
        val req = Request.Builder().url(server.url("/x")).apply {
            if (explicit != null) header("Authorization", explicit)
        }.build()
        client.newCall(req).execute().close()
        return server.takeRequest().getHeader("Authorization")
    }

    @Test fun addsHeaderWhenMissing() = assertEquals("Bearer abc", send("Bearer abc"))

    @Test fun keepsExplicitHeader() = assertEquals("Bearer own", send("Bearer abc", "Bearer own"))

    @Test fun noHeaderWhenTokenNull() = assertNull(send(null))
}
