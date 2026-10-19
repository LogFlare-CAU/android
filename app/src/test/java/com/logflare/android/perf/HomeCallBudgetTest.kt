package com.logflare.android.perf

import com.example.logflare.core.network.HttpUnauthorizedAction
import com.example.logflare.core.network.auth.AccessServiceTokenProvider
import com.example.logflare.core.network.auth.AuthTokenProvider
import com.example.logflare.core.network.di.NetworkModule
import com.example.logflare.core.network.host.BaseUrlProvider
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.LogStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.feature.home.HomeViewModel
import com.logflare.android.push.NotificationHistory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Home's network budget over the real stack (Retrofit + the app's OkHttp interceptors + real
 * stores and file cache) against a MockWebServer: one project list, one summary and one events
 * request on a cold open, and nothing when Home is re-created while the cache is still fresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeCallBudgetTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private val json = NetworkModule.provideJson()
    private var now = 1_000_000L
    private val scopes = mutableListOf<CoroutineScope>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val body = when (request.requestUrl?.encodedPath) {
                        "/project/" -> """{"success":true,"message":"ok","data":[{"id":1,"name":"alpha"},{"id":2,"name":"beta"}]}"""
                        "/log/summary" -> """{"success":true,"message":"ok","data":{"window_hours":24,"since":"2026-10-17T00:00:00",
                            "counts":{"ERROR":3,"WARNING":1},"projects":[{"project_id":1,"error_count":3,"warning_count":1,"last_error_at":null}]}}"""
                        "/log/events" -> """{"success":true,"message":"ok","data":{"items":[{"id":9,"project_id":1,"logfile_id":null,
                            "logfile_name":null,"errortype":null,"message":"boom","level":"ERROR","timestamp":"2026-10-18T01:00:00",
                            "occurrences":4,"first_seen":"2026-10-17T01:00:00","last_seen":"2026-10-18T01:00:00"}],"next_cursor":null}}"""
                        else -> return MockResponse().setResponseCode(404)
                    }
                    return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
                }
            }
            start()
        }
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        server.shutdown()
        Dispatchers.resetMain()
    }

    private val session = MutableStateFlow<SessionState>(
        SessionState.SignedIn(
            serverUrl = "",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 100,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        ),
    )

    /** A fresh process: new OkHttp/Retrofit, stores and VM over the same cache directory. */
    private fun newHome(): HomeViewModel {
        val baseUrl = server.url("/").toString()
        val client = NetworkModule.provideOkHttp(
            baseUrlProvider = object : BaseUrlProvider { override fun getBaseUrl() = baseUrl },
            authTokenProvider = AuthTokenProvider { "Bearer jwt" },
            accessTokens = AccessServiceTokenProvider { null },
            httpUnauthorizedAction = HttpUnauthorizedAction { },
        )
        val api = NetworkModule.provideLogflareApi(NetworkModule.provideRetrofit(client, json))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scopes += it }
        val cache = FileCacheStore(tmp.root, json, Dispatchers.IO)
        val vm = HomeViewModel(
            ProjectStore(api, session, cache, scope) { now },
            LogStore(api, session, cache, scope) { now },
            NotificationHistory(session, cache),
        )
        scope.launch { vm.state.collect {} }
        return vm
    }

    private fun recordedPaths(): List<String> =
        (0 until server.requestCount).map { server.takeRequest(1, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath }

    /** Waits for Home to show content, then a moment longer so any stray request would land. */
    private fun HomeViewModel.awaitLoaded() = runBlocking {
        withTimeout(10_000) { state.first { !it.initialLoad && it.summary != null } }
        delay(750)
    }

    @Test
    fun coldHome_makesOneRequestPerKind_andReopenWithinCacheAgeMakesNone() {
        newHome().awaitLoaded()

        val cold = recordedPaths()
        assertEquals(1, cold.count { it == "/project/" })
        assertEquals(1, cold.count { it == "/log/summary" })
        assertEquals(1, cold.count { it == "/log/events" })
        assertFalse(cold.any { it.startsWith("/fcm/") })
        assertFalse(cold.any { it == "/user/me" })
        assertEquals("unexpected extra requests: $cold", 3, cold.size)

        // Re-create within the 30 s cache age: content comes from the cache, no network at all.
        now += 29_000
        val vm = newHome()
        vm.awaitLoaded()
        assertEquals(listOf("alpha", "beta"), vm.state.value.projectNames.values.toList())
        assertEquals(cold.size, server.requestCount)
    }
}
