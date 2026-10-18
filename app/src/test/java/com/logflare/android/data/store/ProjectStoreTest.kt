package com.logflare.android.data.store

import app.cash.turbine.test
import com.example.logflare.core.model.LogFileDTO
import com.example.logflare.core.model.LogSummaryResponse
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.ProjectSummaryDTO
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi.Companion.ok
import com.logflare.android.data.store.FakeLogflareApi.Companion.project
import com.logflare.android.data.store.FakeLogflareApi.Companion.summaryOf
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.LogFileRef
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.data.store.model.ProjectWithToken
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(signedIn())
    private var now = 1_000_000L

    private fun TestScope.newStore() = ProjectStore(
        api = api,
        sessions = sessions,
        store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
        scope = backgroundScope,
        clock = { now },
    )

    @Test
    fun projects_mergesListAndSummary_inParallel() = runTest {
        val list = CompletableDeferred<ProjectSequenceResponse>()
        val summary = CompletableDeferred<LogSummaryResponse>()
        api.projects = { list.await() }
        api.summary = { summary.await() }
        val store = newStore()

        store.projects().test {
            assertTrue(awaitItem().isInitialLoad)
            runCurrent()
            // Both requests are out while neither has answered.
            assertEquals(1, api.count("listProjects"))
            assertEquals(1, api.count("getLogSummary"))

            list.complete(
                ok(
                    listOf(
                        ProjectDTO(1, "alpha", logfiles = listOf(LogFileDTO(5, 1, "/var/log/a.log", "a.log"))),
                        project(2, "beta"),
                    ),
                ),
            )
            summary.complete(
                summaryOf(projects = listOf(ProjectSummaryDTO(1, errorCount = 3, warningCount = 4, lastErrorAt = "2026-10-18T09:30:00"))),
            )
            val loaded = awaitItem()
            assertFalse(loaded.refreshing)
            assertEquals(
                listOf(
                    ProjectItem(1, "alpha", listOf(LogFileRef(5, "a.log")), 3, 4, Instant.parse("2026-10-18T09:30:00Z")),
                    ProjectItem(2, "beta", emptyList(), 0, 0, null),
                ),
                loaded.data,
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(TOKEN), api.tokens)
    }

    @Test
    fun projects_secondCollectWithin30s_noNetwork() = runTest {
        val store = newStore()
        store.projects().first { it.data != null && !it.refreshing }

        now += 29_000
        val second = store.projects().first()

        assertEquals(listOf(ProjectItem(1, "alpha", emptyList(), 0, 0, null)), second.data)
        assertFalse(second.refreshing)
        assertEquals(1, api.count("listProjects"))
        assertEquals(1, api.count("getLogSummary"))
    }

    @Test
    fun projects_signedOut_emitsEmptyResource() = runTest {
        sessions.value = SessionState.SignedOut
        val store = newStore()

        assertEquals(Resource<List<ProjectItem>>(null, false, null, null), store.projects().first())
        assertEquals(0, api.calls.size)
    }

    @Test
    fun create_refreshesProjects() = runTest {
        val store = newStore()
        store.projects().test {
            assertTrue(awaitItem().isInitialLoad)
            assertEquals(1, awaitItem().data?.size)

            api.projects = { ok(listOf(project(1, "alpha"), project(99, "gamma"))) }
            val created = store.create("gamma")
            assertEquals(Result.success(ProjectWithToken(99, "gamma", "tok-99")), created)

            var item = awaitItem()
            while (item.refreshing) item = awaitItem()
            assertEquals(listOf("alpha", "gamma"), item.data?.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, api.count("listProjects"))
    }

    @Test
    fun create_failureEnvelope_isFailureResult() = runTest {
        api.createProjectResult = { com.example.logflare.core.model.ProjectResponseWithToken(false, "name taken", 409) }
        val store = newStore()

        val result = store.create("alpha")

        assertTrue(result.exceptionOrNull() is ApiException)
        assertEquals("name taken", result.exceptionOrNull()?.message)
    }

    @Test
    fun summary_countsErrorsAndWarnings() = runTest {
        api.summary = {
            summaryOf(
                counts = mapOf("DEBUG" to 9, "INFO" to 8, "WARNING" to 2, "ERROR" to 3, "CRITICAL" to 1),
                projects = listOf(ProjectSummaryDTO(1, 4, 2), ProjectSummaryDTO(2, 0, 0)),
            )
        }
        val store = newStore()

        val summary = store.summary().first { it.data != null }.data

        assertEquals(HomeSummary(errors24h = 4, warnings24h = 2, projectCount = 2), summary)
    }

    companion object {
        const val TOKEN = "Bearer jwt"

        fun signedIn() = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = TOKEN,
            username = "me",
            userIdx = 7,
            permission = 100,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        )
    }
}
