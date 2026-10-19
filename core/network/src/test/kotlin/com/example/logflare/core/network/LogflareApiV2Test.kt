package com.example.logflare.core.network

import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.model.LogoutParams
import com.example.logflare.core.network.auth.AuthHeaderInterceptor
import com.example.logflare.core.network.auth.AuthTokenProvider
import com.example.logflare.core.network.di.NetworkModule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class LogflareApiV2Test {
    private lateinit var server: MockWebServer
    private lateinit var api: LogflareApi

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(AuthTokenProvider { "Bearer tok" }))
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(
                NetworkModule.provideJson().asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(LogflareApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    private fun enqueue(body: String) {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(body)
        )
    }

    @Test fun getLogSummary() = runTest {
        enqueue(
            """{"success":true,"message":"ok","error_code":null,"data":{"window_hours":24,
            "since":"2026-10-17T00:00:00","counts":{"ERROR":3,"WARNING":1},
            "projects":[{"project_id":3,"error_count":3,"warning_count":1,"last_error_at":null}]}}"""
        )
        val res = api.getLogSummary(projectId = 3)
        val req = server.takeRequest()
        assertEquals("/log/summary?project_id=3&hours=24", req.path)
        assertEquals("Bearer tok", req.getHeader("Authorization"))
        val data = res.data!!
        assertEquals(24, data.windowHours)
        assertEquals(3, data.counts["ERROR"])
        assertEquals(3, data.projects.single().projectId)
        assertNull(data.projects.single().lastErrorAt)
    }

    @Test fun getLogEvents() = runTest {
        enqueue(
            """{"success":true,"message":"ok","data":{"items":[{"id":9,"project_id":3,
            "logfile_id":null,"logfile_name":null,"errortype":null,"message":"boom","level":"ERROR",
            "timestamp":"2026-10-18T01:00:00","occurrences":4,"first_seen":"2026-10-17T01:00:00",
            "last_seen":"2026-10-18T01:00:00"}],"next_cursor":null}}"""
        )
        val res = api.getLogEvents(projectId = 3, minLevel = "ERROR", logfileId = null, query = null)
        assertEquals(
            "/log/events?project_id=3&min_level=ERROR&sort=newest&grouped=true&limit=50",
            server.takeRequest().path
        )
        val page = res.data!!
        assertNull(page.nextCursor)
        val item = page.items.single()
        assertNull(item.logfileName)
        assertNull(item.logfileId)
        assertEquals(4, item.occurrences)
        assertEquals("2026-10-17T01:00:00", item.firstSeen)
    }

    @Test fun getLogEventsWithCursorAndQuery() = runTest {
        enqueue("""{"success":true,"message":"ok","data":{"items":[],"next_cursor":"abc"}}""")
        val res = api.getLogEvents(
            projectId = null, minLevel = null, logfileId = 2, query = "a b",
            sort = "oldest", grouped = false, limit = 10, cursor = "xyz"
        )
        assertEquals(
            "/log/events?logfile_id=2&q=a%20b&sort=oldest&grouped=false&limit=10&cursor=xyz",
            server.takeRequest().path
        )
        assertEquals("abc", res.data!!.nextCursor)
    }

    @Test fun getLogEvent() = runTest {
        enqueue(
            """{"success":true,"message":"ok","data":{"id":9,"project_id":3,"logfile_id":1,
            "logfile_name":"app.log","errortype":"E","message":"boom","level":"ERROR",
            "timestamp":"2026-10-18T01:00:00","occurrences":4,"first_seen":"2026-10-17T01:00:00",
            "last_seen":"2026-10-18T01:00:00","occurred_at":"2026-10-17T05:00:00",
            "row_level":"WARNING","raw":null,"recent_ids":[8,7]}}"""
        )
        val res = api.getLogEvent(9)
        assertEquals("/log/events/9", server.takeRequest().path)
        val d = res.data!!
        assertEquals("app.log", d.logfileName)
        assertEquals("WARNING", d.rowLevel)
        assertEquals("2026-10-17T05:00:00", d.occurredAt)
        assertNull(d.raw)
        assertEquals(listOf(8, 7), d.recentIds)
    }

    @Test fun getProjectToken() = runTest {
        enqueue(
            """{"success":true,"message":"ok","data":{"id":5,"name":"p","token":"k"}}"""
        )
        val res = api.getProjectToken(5)
        assertEquals("/project/5/token", server.takeRequest().path)
        assertTrue(res.success)
    }

    @Test fun unregisterFcmToken() = runTest {
        enqueue("""{"success":true,"message":"ok","data":{"removed":false}}""")
        val res = api.unregisterFcmToken(FcmTokenParams("t1"))
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/fcm/token", req.path)
        assertEquals("""{"fcm_token":"t1"}""", req.body.readUtf8())
        assertFalse(res.data!!.removed)
    }

    @Test fun logout_postsOptionalTokenAndParsesResult() = runTest {
        enqueue("""{"success":true,"message":"ok","data":{"logged_out":true,"fcm_removed":true}}""")
        val res = api.logout(LogoutParams("t1"))
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/user/logout", req.path)
        assertEquals("Bearer tok", req.getHeader("Authorization"))
        assertEquals("""{"fcm_token":"t1"}""", req.body.readUtf8())
        assertTrue(res.data!!.loggedOut)
        assertTrue(res.data!!.fcmRemoved)

        enqueue("""{"success":true,"message":"ok","data":{"logged_out":true,"fcm_removed":false}}""")
        api.logout(LogoutParams())
        assertEquals("{}", server.takeRequest().body.readUtf8())
    }

    @Test fun formerHeaderEndpoints_getInterceptorToken() = runTest {
        enqueue("""{"success":true,"message":"ok","data":[]}""")
        api.listProjects()
        val req = server.takeRequest()
        assertEquals("/project/", req.path)
        assertEquals("Bearer tok", req.getHeader("Authorization"))
    }

    @Test fun getMeAs_sendsItsOwnBearer() = runTest {
        enqueue("""{"success":true,"message":"ok","data":{"idx":1,"username":"a","permission":0}}""")
        api.getMeAs("Bearer new")
        val req = server.takeRequest()
        assertEquals("/user/me", req.path)
        assertEquals(listOf("Bearer new"), req.headers.values("Authorization"))
    }
}
