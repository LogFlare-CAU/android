package com.logflare.android.data.session

import com.example.logflare.core.model.AuthModes
import com.example.logflare.core.model.AuthModesResponse
import com.logflare.android.data.store.FakeLogflareApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthModesLoaderTest {
    private val api = FakeLogflareApi()
    private val loader = AuthModesLoader(api)

    private fun modes(vararg names: String) = AuthModesResponse(true, "ok", data = AuthModes(names.toList()))

    /**
     * The loader holds no base-URL provider at all, so it cannot persist the probed server; the
     * call is aimed at the typed server through the probe tag (wire behaviour: ProbeBaseUrlTest).
     */
    @Test fun probesTheTypedServer_normalized() = runTest {
        api.authModesResult = { modes("password", "access") }

        val result = loader.load("https://logs.example.com")

        assertEquals(AuthModesResult.Ready(listOf("password", "access")), result)
        assertEquals(listOf("https://logs.example.com/"), api.probedUrls)
    }

    @Test fun unknownModesAreDropped() = runTest {
        api.authModesResult = { modes("password", "saml") }
        assertEquals(AuthModesResult.Ready(listOf("password")), loader.load("https://logs.example.com"))
    }

    @Test fun emptyListMeansNoSignInMethod() = runTest {
        api.authModesResult = { modes() }
        assertEquals(AuthModesResult.Ready(emptyList()), loader.load("https://logs.example.com"))
    }

    @Test fun oldServer404_isPasswordOnly() = runTest {
        api.authModesResult = { throw HttpException(Response.error<Any>(404, "{}".toResponseBody(null))) }
        assertEquals(AuthModesResult.Ready(listOf("password")), loader.load("https://logs.example.com"))
    }

    @Test fun networkAndServerErrors_areUnreachable() = runTest {
        api.authModesResult = { throw IOException("down") }
        assertEquals(AuthModesResult.Unreachable, loader.load("https://logs.example.com"))
        api.authModesResult = { throw HttpException(Response.error<Any>(500, "{}".toResponseBody(null))) }
        assertEquals(AuthModesResult.Unreachable, loader.load("https://logs.example.com"))
    }

    @Test fun blankUrl_isNotProbed() = runTest {
        assertEquals(AuthModesResult.Unreachable, loader.load("  "))
        assertEquals(0, api.probedUrls.size)
    }
}
