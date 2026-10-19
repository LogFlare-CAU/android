package com.logflare.android.push

import org.junit.Assert.assertEquals
import org.junit.Test

class PushBodyTest {
    @Test
    fun `all parts are joined by newlines`() {
        assertEquals("Error: Db\nboom\nat 10:00", composePushBody("Error: Db", "boom", "at 10:00"))
    }

    @Test
    fun `missing parts are omitted and never render null`() {
        assertEquals("Db", composePushBody("Db", null, null))
        assertEquals("Db\nboom", composePushBody("Db", "boom", null))
    }
}
