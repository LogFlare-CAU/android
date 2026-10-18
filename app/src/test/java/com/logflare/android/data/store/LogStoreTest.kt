package com.logflare.android.data.store

import app.cash.turbine.test
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi.Companion.page
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.parseServerTime
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

    @Test
    fun parseServerTime_handlesFractionAndNoOffset() {
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T10:00:00"))
        assertEquals(Instant.parse("2026-10-18T10:00:00.123456Z"), parseServerTime("2026-10-18T10:00:00.123456"))
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T10:00:00Z"))
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), parseServerTime("2026-10-18T19:00:00+09:00"))
        assertEquals(Instant.parse("2026-10-18T10:00:00.5Z"), parseServerTime("2026-10-18 10:00:00.5"))
    }
}
