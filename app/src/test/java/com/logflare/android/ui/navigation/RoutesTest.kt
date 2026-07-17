package com.logflare.android.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoutesTest {

    @Test
    fun logDetailRouteContainsNoUnboundArgument() {
        assertEquals("log/detail", Route.LogDetail.path)
        assertFalse(Route.LogDetail.path.contains("{"))
    }
}
