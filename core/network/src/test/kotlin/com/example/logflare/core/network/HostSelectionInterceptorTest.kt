package com.example.logflare.core.network

import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.HostSelectionInterceptor
import com.example.logflare.core.network.host.ServerNotConfiguredException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class HostSelectionInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After fun tearDown() = server.shutdown()

    private fun client(base: String?) = OkHttpClient.Builder()
        .addInterceptor(HostSelectionInterceptor(object : BaseUrlProvider { override fun getBaseUrl() = base }))
        .build()

    @Test fun throwsWhenNotConfigured() {
        val req = Request.Builder().url("http://10.0.2.2:8000/api/x").build()
        listOf(null, "", "  ", "ftp://example.com").forEach { base ->
            assertThrows(ServerNotConfiguredException::class.java) {
                client(base).newCall(req).execute()
            }
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun rewritesHostAndKeepsPathAndQuery() {
        server.enqueue(MockResponse().setResponseCode(200))
        val req = Request.Builder().url("http://10.0.2.2:8000/api/x?a=1&b=2").build()
        client(server.url("/").toString()).newCall(req).execute().close()
        assertEquals("/api/x?a=1&b=2", server.takeRequest().path)
    }
}
