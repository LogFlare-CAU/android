package com.logflare.android.data.store

import app.cash.turbine.test
import com.logflare.android.data.session.SessionKey
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CachedResourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val a = SessionKey("aaaaaaaaaaaaaaaa")
    private val b = SessionKey("bbbbbbbbbbbbbbbb")

    // Note: the shared fetch runs in backgroundScope, which advanceUntilIdle() does not drive;
    // runCurrent() runs every task due now (there are no delays in these tests).
    private var now = 100_000L
    private var fetchCount = 0
    private val fetchedFor = mutableListOf<SessionKey>()

    private fun TestScope.newStore(): CacheStore =
        FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler))

    private fun TestScope.resource(
        store: CacheStore,
        fetch: suspend (SessionKey) -> String = { "fresh" },
    ) = CachedResource(
        key = "projects",
        serializer = String.serializer(),
        store = store,
        clock = { now },
        scope = backgroundScope,
        fetch = { session ->
            fetchCount++
            fetchedFor += session
            fetch(session)
        },
    )

    @Test
    fun emitsCachedThenFresh_whenStale() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 31_000)
        val res = resource(store)

        res.stream(a).test {
            assertEquals(Resource("cached", refreshing = true, error = null, fetchedAt = now - 31_000), awaitItem())
            assertEquals(Resource("fresh", refreshing = false, error = null, fetchedAt = now), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fetchCount)
        assertEquals(Cached("fresh", now), store.read(a, "projects", String.serializer()))
    }

    @Test
    fun doesNotFetch_whenFresh() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 29_000)
        val res = resource(store)

        res.stream(a).test {
            assertEquals(Resource("cached", refreshing = false, error = null, fetchedAt = now - 29_000), awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, fetchCount)
    }

    @Test
    fun force_fetchesEvenWhenFresh() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 1_000)
        val res = resource(store)

        res.stream(a, force = true).test {
            assertEquals(Resource("cached", refreshing = true, error = null, fetchedAt = now - 1_000), awaitItem())
            assertEquals(Resource("fresh", refreshing = false, error = null, fetchedAt = now), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fetchCount)
    }

    @Test
    fun concurrentCollectors_fetchOnce() = runTest {
        val gate = CompletableDeferred<String>()
        val res = resource(newStore()) { gate.await() }

        val first = backgroundScope.launch { res.stream(a).first { it.data == "fresh" } }
        val second = backgroundScope.launch { res.stream(a).first { it.data == "fresh" } }
        runCurrent()
        assertEquals(1, fetchCount)

        gate.complete("fresh")
        runCurrent()
        assertTrue(first.isCompleted && second.isCompleted)
        assertEquals(1, fetchCount)
    }

    @Test
    fun failure_keepsCachedData() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 60_000)
        val boom = IOException("offline")
        val res = resource(store) { throw boom }

        res.stream(a).test {
            assertEquals(Resource("cached", refreshing = true, error = null, fetchedAt = now - 60_000), awaitItem())
            val failed = awaitItem()
            assertEquals("cached", failed.data)
            assertFalse(failed.refreshing)
            assertSame(boom, failed.error)
            assertEquals(now - 60_000, failed.fetchedAt)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(Cached("cached", now - 60_000), store.read(a, "projects", String.serializer()))
    }

    @Test
    fun noCache_emitsInitialLoadThenData() = runTest {
        val res = resource(newStore())

        res.stream(a).test {
            val first = awaitItem()
            assertTrue(first.isInitialLoad)
            assertEquals(Resource<String>(null, refreshing = true, error = null, fetchedAt = null), first)
            assertEquals(Resource("fresh", refreshing = false, error = null, fetchedAt = now), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cache_isolatedBySession() = runTest {
        val store = newStore()
        store.write(a, "projects", "data-of-A", String.serializer(), fetchedAt = now)
        val res = resource(store) { session -> "data-of-${session.value.first().uppercase()}" }

        assertNull(store.read(b, "projects", String.serializer()))
        res.stream(b).test {
            assertTrue(awaitItem().isInitialLoad)
            assertEquals("data-of-B", awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(b), fetchedFor)
        assertEquals(Cached("data-of-A", now), store.read(a, "projects", String.serializer()))
    }

    @Test
    fun refresh_updatesActiveStream() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now)
        val res = resource(store)

        res.stream(a).test {
            assertEquals("cached", awaitItem().data)
            now += 1_000
            res.refresh(a)
            assertEquals(Resource("cached", refreshing = true, error = null, fetchedAt = now - 1_000), awaitItem())
            assertEquals(Resource("fresh", refreshing = false, error = null, fetchedAt = now), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancelledCollector_doesNotCancelSharedFetch() = runTest {
        val gate = CompletableDeferred<String>()
        val res = resource(newStore()) { gate.await() }

        val quitter = backgroundScope.launch { res.stream(a).collect {} }
        runCurrent()
        val stayer = backgroundScope.launch { res.stream(a).take(2).toList() }
        runCurrent()
        quitter.cancel()

        gate.complete("fresh")
        runCurrent()
        assertTrue(stayer.isCompleted)
        assertEquals(1, fetchCount)
    }
}
