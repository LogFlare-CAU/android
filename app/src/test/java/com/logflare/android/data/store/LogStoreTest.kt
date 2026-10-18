package com.logflare.android.data.store

import app.cash.turbine.test
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionState
import com.example.logflare.core.model.LogEventPageDTO
import com.example.logflare.core.model.LogEventPageResponse
import com.logflare.android.data.store.FakeLogflareApi.Companion.event
import com.logflare.android.data.store.FakeLogflareApi.Companion.page
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.parseServerTime
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LogStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi()
    private val sessions = MutableStateFlow<SessionState>(ProjectStoreTest.signedIn())
    private var now = 1_000_000L

    private fun TestScope.newStore() = LogStore(
        api = api,
        sessions = sessions,
        store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
        scope = backgroundScope,
        clock = { now },
    )

    @Test
    fun events_queryChangesCacheKey() = runTest {
        api.events = { call -> if (call.minLevel == "ERROR") page(2) else page(1) }
        val store = newStore()
        val all = LogQuery(projectId = 1)
        val errors = LogQuery(projectId = 1, minLevel = LfLevel.Error)

        assertEquals(listOf(1), store.events(all).first { it.data != null }.data?.items?.map { it.id })
        assertEquals(listOf(2), store.events(errors).first { it.data != null }.data?.items?.map { it.id })
        // Back to the first query within 30 s: served from its own cache entry.
        val again = store.events(all).first()
        assertEquals(listOf(1), again.data?.items?.map { it.id })
        assertFalse(again.refreshing)

        assertEquals(
            listOf(
                EventsCall(1, null, null, null, "newest", null),
                EventsCall(1, "ERROR", null, null, "newest", null),
            ),
            api.eventsCalls,
        )
    }

    @Test
    fun hashKey_coversEveryField() {
        val base = LogQuery()
        val variants = listOf(
            base,
            base.copy(projectId = 1),
            base.copy(minLevel = LfLevel.Warning),
            base.copy(logFileId = 3),
            base.copy(text = "timeout"),
            base.copy(sort = LogSortOrder.Oldest),
        )
        assertEquals(variants.size, variants.map { it.hashKey() }.toSet().size)
        assertEquals(LogQuery(projectId = 1, text = "x").hashKey(), LogQuery(projectId = 1, text = "x").hashKey())
        // A text that mimics another field can't collide with it.
        assertNotEquals(LogQuery(text = "a|s=oldest").hashKey(), LogQuery(text = "a", sort = LogSortOrder.Oldest).hashKey())
    }

    @Test
    fun loadMore_appendsAndStopsWhenNoCursor() = runTest {
        api.events = { call -> if (call.cursor == null) page(1, 2, cursor = "c1") else page(3) }
        val store = newStore()
        val query = LogQuery(text = "boom")

        store.events(query).test {
            assertTrue(awaitItem().isInitialLoad)
            val first = awaitItem().data!!
            assertEquals(listOf(1, 2), first.items.map { it.id })
            assertTrue(first.hasMore)

            assertTrue(store.loadMore(query).isSuccess)
            val merged = awaitItem().data!!
            assertEquals(listOf(1, 2, 3), merged.items.map { it.id })
            assertFalse(merged.hasMore)

            assertTrue(store.loadMore(query).isSuccess)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(null, "c1"), api.eventsCalls.map { it.cursor })
        assertEquals(setOf("boom"), api.eventsCalls.map { it.query }.toSet())
        // The appended page is cached too.
        assertEquals(listOf(1, 2, 3), store.events(query).first().data?.items?.map { it.id })
    }

    @Test
    fun recent_isErrorFirstPageTruncated() = runTest {
        api.events = { page(1, 2, 3, cursor = "c1") }
        val store = newStore()

        val recent = store.recent(limit = 2).first { it.data != null }.data!!

        assertEquals(listOf(1, 2), recent.map { it.id })
        assertEquals("ERROR", api.eventsCalls.single().minLevel)
        assertEquals(
            LogEvent(
                id = 1,
                projectId = 1,
                logFileName = "app.log",
                type = "RuntimeError",
                message = "boom 1",
                level = LfLevel.Error,
                lastSeen = Instant.parse("2026-10-18T09:00:00Z"),
                firstSeen = Instant.parse("2026-10-18T08:00:00.5Z"),
                occurrences = 2,
            ),
            recent.first(),
        )
    }

    /** Old first page 1,2 (cursor c1, continues with 3); fresh first page 10,1,2 (cursor c2, continues with 4). */
    private fun refreshScenario(freshGate: CompletableDeferred<Unit>?, nextGate: CompletableDeferred<Unit>?) {
        var firstPages = 0
        api.events = { call ->
            when (call.cursor) {
                null -> if (firstPages++ == 0) {
                    page(1, 2, cursor = "c1")
                } else {
                    freshGate?.await()
                    page(10, 1, 2, cursor = "c2")
                }
                "c1" -> {
                    nextGate?.await()
                    page(3)
                }
                else -> page(4)
            }
        }
    }

    @Test
    fun loadMore_duringRefresh_waitsAndContinuesFreshList() = runTest {
        val fresh = CompletableDeferred<Unit>()
        refreshScenario(freshGate = fresh, nextGate = null)
        val store = newStore()
        val query = LogQuery()
        assertEquals(listOf(1, 2), store.events(query).first { it.data != null }.data?.items?.map { it.id })

        val refresher = backgroundScope.launch { store.events(query, force = true).collect {} }
        runCurrent() // first-page refresh is now in flight
        val more = backgroundScope.async { store.loadMore(query) }
        runCurrent()
        assertFalse(more.isCompleted) // waiting for the refresh, not fetching page "c1"
        fresh.complete(Unit)
        runCurrent()

        assertTrue(more.await().isSuccess)
        refresher.cancel()
        assertEquals(listOf(null, null, "c2"), api.eventsCalls.map { it.cursor })
        val page = store.events(query).first().data!!
        assertEquals(listOf(10, 1, 2, 4), page.items.map { it.id })
        assertFalse(page.hasMore)
    }

    @Test
    fun refreshLandingDuringLoadMore_winsOverStaleAppend() = runTest {
        val next = CompletableDeferred<Unit>()
        refreshScenario(freshGate = null, nextGate = next)
        val store = newStore()
        val query = LogQuery()
        assertEquals(listOf(1, 2), store.events(query).first { it.data != null }.data?.items?.map { it.id })

        val more = backgroundScope.async { store.loadMore(query) }
        runCurrent() // page "c1" is loading
        assertEquals(listOf(10, 1, 2), store.events(query, force = true).first { !it.refreshing }.data?.items?.map { it.id })
        next.complete(Unit)
        runCurrent()

        assertTrue(more.await().isSuccess)
        val page = store.events(query).first().data!!
        assertEquals(listOf(10, 1, 2), page.items.map { it.id }) // old rows 1,2,3 did not come back
        assertTrue(page.hasMore)
    }

    @Test
    fun loadMore_skipsGroupsAlreadyListed() = runTest {
        api.events = { call ->
            if (call.cursor == null) {
                page(1, 2, cursor = "c1")
            } else {
                // Group "boom 2" got a newer row (id 7) between the pages; group 3 is new.
                LogEventPageResponse(true, "ok", data = LogEventPageDTO(listOf(event(7, message = "boom 2"), event(3)), null))
            }
        }
        val store = newStore()
        val query = LogQuery(sort = LogSortOrder.Oldest)
        store.events(query).first { it.data != null }

        assertTrue(store.loadMore(query).isSuccess)

        assertEquals(listOf(1, 2, 3), store.events(query).first().data?.items?.map { it.id })
    }

    @Test
    fun events_force_refetchesFreshCache() = runTest {
        api.events = { page(1) }
        val store = newStore()
        store.events(LogQuery()).first { it.data != null }

        store.events(LogQuery(), force = true).first { it.data != null && !it.refreshing }

        assertEquals(2, api.count("getLogEvents"))
    }

    @Test
    fun loadMore_whileSignedOut_isNotSignedInFailure() = runTest {
        sessions.value = SessionState.SignedOut
        assertTrue(newStore().loadMore(LogQuery()).exceptionOrNull() is NotSignedInException)
        assertEquals(0, api.calls.size)
    }

    @Test
    fun parseServerTime_handlesFractionAndNoOffset() {
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T10:00:00"))
        assertEquals(Instant.parse("2026-10-18T10:00:00.123456Z"), parseServerTime("2026-10-18T10:00:00.123456"))
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T10:00:00Z"))
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T19:00:00+09:00"))
        assertEquals(Instant.parse("2026-10-18T10:00:00.5Z"), parseServerTime("2026-10-18 10:00:00.5"))
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T19:00:00+0900"))
        assertEquals(Instant.parse("2026-10-18T10:00:00.25Z"), parseServerTime("2026-10-18T10:00:00.25z"))
    }
}
