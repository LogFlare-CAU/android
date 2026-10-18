package com.logflare.android.data.store

import com.logflare.android.data.session.SessionKey
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CacheStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val a = SessionKey("aaaaaaaaaaaaaaaa")
    private val b = SessionKey("bbbbbbbbbbbbbbbb")
    private val strings = ListSerializer(String.serializer())

    private fun TestScope.newStore() =
        FileCacheStore(tmp.root, Json { ignoreUnknownKeys = true }, StandardTestDispatcher(testScheduler))

    @Test
    fun clear_removesOnlyThatSession() = runTest {
        val store = newStore()
        store.write(a, "projects", listOf("x"), strings, fetchedAt = 1L)
        store.write(b, "projects", listOf("y"), strings, fetchedAt = 2L)

        store.clear(a)

        assertNull(store.read(a, "projects", strings))
        assertEquals(Cached(listOf("y"), 2L), store.read(b, "projects", strings))
        // Cleared from disk too, not just from memory.
        assertNull(newStore().read(a, "projects", strings))
        assertEquals(Cached(listOf("y"), 2L), newStore().read(b, "projects", strings))
    }

    @Test
    fun survivesNewInstance() = runTest {
        newStore().write(a, "events:q=1", listOf("one", "two"), strings, fetchedAt = 42L)

        val reopened = newStore()

        assertEquals(Cached(listOf("one", "two"), 42L), reopened.read(a, "events:q=1", strings))
        assertNull(reopened.read(a, "events:q=2", strings))
        assertNull(reopened.read(b, "events:q=1", strings))
    }

    @Test
    fun write_overwritesExistingEntry() = runTest {
        val store = newStore()
        store.write(a, "k", listOf("old"), strings, fetchedAt = 1L)
        store.write(a, "k", listOf("new"), strings, fetchedAt = 2L)

        assertEquals(Cached(listOf("new"), 2L), newStore().read(a, "k", strings))
    }

    @Test
    fun corruptFile_readsAsMissing() = runTest {
        val store = newStore()
        store.write(a, "k", listOf("v"), strings, fetchedAt = 1L)
        tmp.root.resolve(a.value).listFiles()!!.single().writeText("{not json")

        assertNull(newStore().read(a, "k", strings))
    }
}
