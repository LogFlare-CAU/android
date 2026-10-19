package com.example.logflare.core.network

import com.example.logflare.core.network.auth.AccessHeaderInterceptor
import com.example.logflare.core.network.auth.AccessServiceToken
import com.example.logflare.core.network.auth.AuthHeaderInterceptor
import com.example.logflare.core.network.auth.AuthTokenProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.HostSelectionInterceptor
import com.example.logflare.core.network.host.ProbeBaseUrl
import com.example.logflare.core.network.host.ServerNotConfiguredException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ProbeBaseUrlTest {
    private var sent: Request? = null

    /** Ends the chain with a canned answer so no network is needed and the https rewrite can be read. */
    private val terminal = Interceptor { chain ->
        sent = chain.request()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("ok").body("".toResponseBody(null)).build()
    }

    private fun client(saved: String?) = OkHttpClient.Builder()
        .addInterceptor(AuthHeaderInterceptor(AuthTokenProvider { "Bearer saved" }))
        .addInterceptor(HostSelectionInterceptor(object : BaseUrlProvider { override fun getBaseUrl() = saved }))
        .addInterceptor(AccessHeaderInterceptor { AccessServiceToken("id", "secret") })
        .addInterceptor(terminal)
        .build()

    private fun probe(url: String, saved: String? = "https://saved.example.com") {
        val req = Request.Builder().url("https://placeholder.invalid/user/auth/modes")
            .tag(ProbeBaseUrl::class.java, ProbeBaseUrl(url)).build()
        client(saved).newCall(req).execute().close()
    }

    @Test fun probeGoesToTheTypedHost_withoutSavedUrl() {
        probe("https://typed.example.com:8443", saved = null)
        assertEquals("https://typed.example.com:8443/user/auth/modes", sent!!.url.toString())
    }

    @Test fun probeCarriesNeitherSessionNorAccessToken() {
        probe("https://typed.example.com")
        assertNull(sent!!.header("Authorization"))
        assertNull(sent!!.header(AccessHeaderInterceptor.CLIENT_ID))
        assertNull(sent!!.header(AccessHeaderInterceptor.CLIENT_SECRET))
    }

    @Test fun probeRefusesPlainHttp() {
        assertThrows(ServerNotConfiguredException::class.java) { probe("http://typed.example.com") }
        assertThrows(ServerNotConfiguredException::class.java) { probe("not a url") }
    }

    @Test fun untaggedRequestStillUsesSavedUrlAndTokens() {
        client("https://saved.example.com").newCall(Request.Builder().url("https://placeholder.invalid/user/me").build()).execute().close()
        assertEquals("https://saved.example.com/user/me", sent!!.url.toString())
        assertEquals("Bearer saved", sent!!.header("Authorization"))
        assertEquals("id", sent!!.header(AccessHeaderInterceptor.CLIENT_ID))
    }
}
