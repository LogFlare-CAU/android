package com.logflare.android.data.store

import app.cash.turbine.test
import com.logflare.android.data.session.SessionKey
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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

    @Test
    fun cancelledCollector_fetchStillWritesToStore() = runTest {
        val store = newStore()
        val gate = CompletableDeferred<String>()
        val res = resource(store) { gate.await() }

        val only = backgroundScope.launch { res.stream(a).collect {} }
        runCurrent()
        only.cancel()
        gate.complete("fresh")
        runCurrent()

        assertEquals(Cached("fresh", now), store.read(a, "projects", String.serializer()))
        assertEquals(Cached("fresh", now), newStore().read(a, "projects", String.serializer()))
    }

    @Test
    fun staleness_boundary() = runTest {
        val store = newStore()
        val res = resource(store)

        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 30_000)
        assertTrue(res.stream(a).first().refreshing) // age == maxAge → stale

        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 29_999)
        assertFalse(res.stream(a).first().refreshing) // just inside the window → fresh

        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now + 5_000)
        assertTrue(res.stream(a).first().refreshing) // negative age (clock moved back) → stale
    }

    @Test
    fun failedRefresh_surfacesErrorOnOpenStream() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now)
        val boom = IOException("offline")
        val res = resource(store) { throw boom }

        res.stream(a).test {
            assertEquals(Resource("cached", refreshing = false, error = null, fetchedAt = now), awaitItem())
            res.refresh(a) // does not throw
            assertEquals(Resource("cached", refreshing = true, error = null, fetchedAt = now), awaitItem())
            assertEquals(Resource("cached", refreshing = false, error = boom, fetchedAt = now), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fetchTimeout_releasesStreams_andAllowsNewFetch() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now - 60_000)
        var calls = 0
        val res = resource(store) {
            if (calls++ == 0) withTimeout(1) { awaitCancellation() } else "fresh"
        }

        res.stream(a).test {
            assertTrue(awaitItem().refreshing)
            val failed = awaitItem()
            assertEquals("cached", failed.data)
            assertFalse(failed.refreshing)
            assertTrue(failed.error is TimeoutCancellationException)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(Cached("cached", now - 60_000), store.read(a, "projects", String.serializer()))

        res.stream(a).test {
            assertTrue(awaitItem().refreshing)
            assertEquals("fresh", awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, fetchCount)
    }

    @Test
    fun clearDuringFetch_dropsResult() = runTest {
        val store = newStore()
        val gate = CompletableDeferred<String>()
        val res = resource(store) { gate.await() }

        val collector = backgroundScope.launch { res.stream(a).collect {} }
        runCurrent()
        assertEquals(1, fetchCount)
        store.clear(a) // sign-out while the request is still running
        gate.complete("fresh")
        runCurrent()
        collector.cancel()

        assertNull(store.read(a, "projects", String.serializer()))
        assertNull(newStore().read(a, "projects", String.serializer()))
    }

    @Test
    fun put_updatesStoreAndOpenStreams_withoutFetching() = runTest {
        val store = newStore()
        store.write(a, "projects", "cached", String.serializer(), fetchedAt = now)
        val res = resource(store)

        res.stream(a).test {
            assertEquals("cached", awaitItem().data)
            res.put(a, "appended", fetchedAt = now - 5)
            assertEquals(Resource("appended", refreshing = false, error = null, fetchedAt = now - 5), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, fetchCount)
        assertEquals(Cached("appended", now - 5), store.read(a, "projects", String.serializer()))
    }

    @Test
    fun put_withGenerationFromBeforeClear_isDropped() = runTest {
        val store = newStore()
        val res = resource(store)
        val generation = store.generation(a)
        store.clear(a)

        res.put(a, "stale", generation = generation)

        assertNull(store.read(a, "projects", String.serializer()))
    }
}
