package com.logflare.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlCacheCoordinatorTest {

    @Test
    fun staleNonNullAndNullCannotOverwriteWhilePending() {
        val coordinator = BaseUrlCacheCoordinator()
        coordinator.beginLocalWrite("http://10.0.2.2:8000/")

        coordinator.onPersistedEmission("http://stale.example:9000/")
        assertEquals("http://10.0.2.2:8000/", coordinator.current())

        coordinator.onPersistedEmission(null)
        assertEquals("http://10.0.2.2:8000/", coordinator.current())
    }

    @Test
    fun matchingValueAcknowledgesWithoutChangingDesired() {
        val coordinator = BaseUrlCacheCoordinator()
        coordinator.beginLocalWrite("http://10.0.2.2:8000/")

        coordinator.onPersistedEmission("http://10.0.2.2:8000/")
        assertEquals("http://10.0.2.2:8000/", coordinator.current())
    }

    @Test
    fun laterNormalValueCanUpdateAfterAcknowledge() {
        val coordinator = BaseUrlCacheCoordinator()
        coordinator.beginLocalWrite("http://10.0.2.2:8000/")
        coordinator.onPersistedEmission("http://10.0.2.2:8000/")

        coordinator.onPersistedEmission("http://other.example:8000/")
        assertEquals("http://other.example:8000/", coordinator.current())
    }

    @Test
    fun rollbackRestoresPreviousAndClearsPending() {
        val coordinator = BaseUrlCacheCoordinator()
        coordinator.beginLocalWrite("http://old.example:8000/")
        coordinator.onPersistedEmission("http://old.example:8000/")

        val previous = coordinator.beginLocalWrite("http://new.example:8000/")
        assertEquals("http://old.example:8000/", previous)
        assertEquals("http://new.example:8000/", coordinator.current())

        coordinator.rollback(previous)
        assertEquals("http://old.example:8000/", coordinator.current())

        // After rollback, stale pending is cleared — emissions may update again
        coordinator.onPersistedEmission("http://after-rollback.example:8000/")
        assertEquals("http://after-rollback.example:8000/", coordinator.current())
    }

    @Test
    fun beginLocalWritePublishesImmediately() {
        val coordinator = BaseUrlCacheCoordinator()
        assertNull(coordinator.current())
        coordinator.beginLocalWrite("http://10.0.2.2:8000/")
        assertEquals("http://10.0.2.2:8000/", coordinator.current())
    }
}
