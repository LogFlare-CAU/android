package com.example.logflare.core.network

import com.example.logflare.core.network.auth.AccessHeaderInterceptor
import com.example.logflare.core.network.auth.AccessServiceToken
import com.example.logflare.core.network.auth.AccessServiceTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccessHeaderInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        server.enqueue(MockResponse().setResponseCode(200))
    }

    @After fun tearDown() = server.shutdown()

    private fun send(token: AccessServiceToken?, explicitId: String? = null): RecordedRequest {
        val client = OkHttpClient.Builder()
            .addInterceptor(AccessHeaderInterceptor(AccessServiceTokenProvider { token }))
            .build()
        val req = Request.Builder().url(server.url("/user/auth/access")).apply {
            if (explicitId != null) header(AccessHeaderInterceptor.CLIENT_ID, explicitId)
        }.build()
        client.newCall(req).execute().close()
        return server.takeRequest()
    }

    @Test fun addsBothHeaders() {
        val recorded = send(AccessServiceToken("client-id", "client-secret"))
        assertEquals("client-id", recorded.getHeader(AccessHeaderInterceptor.CLIENT_ID))
        assertEquals("client-secret", recorded.getHeader(AccessHeaderInterceptor.CLIENT_SECRET))
    }

    @Test fun noHeadersWhenTokenMissing() {
        val recorded = send(null)
        assertNull(recorded.getHeader(AccessHeaderInterceptor.CLIENT_ID))
        assertNull(recorded.getHeader(AccessHeaderInterceptor.CLIENT_SECRET))
    }

    @Test fun keepsExplicitHeader() {
        val recorded = send(AccessServiceToken("client-id", "client-secret"), explicitId = "already")
        assertEquals("already", recorded.getHeader(AccessHeaderInterceptor.CLIENT_ID))
        assertNull(recorded.getHeader(AccessHeaderInterceptor.CLIENT_SECRET))
    }

    @Test fun tokenToStringOmitsSecret() {
        assertEquals("AccessServiceToken", AccessServiceToken("id", "secret").toString())
    }
}
