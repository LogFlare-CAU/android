package com.example.logflare_android.feature.log

import com.example.logflare_android.ui.common.LogCardInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingLogDetailStoreTest {

    @Test
    fun setPending_takePending_returnsOnceAndClears() {
        val store = PendingLogDetailStore()
        val log = LogCardInfo(
            level = "ERROR",
            timestamp = "2026-01-01T00:00:00Z",
            message = "msg",
            prefix = "P",
            suffix = "S",
        )
        store.setPending(log)
        assertEquals(log, store.takePending())
        assertNull(store.takePending())
    }
}
