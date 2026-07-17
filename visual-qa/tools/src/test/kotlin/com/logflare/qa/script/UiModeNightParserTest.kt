package com.logflare.qa.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiModeNightParserTest {
    @Test
    fun parsesExactNightModeYes() {
        assertEquals(true, UiModeNightParser.isNightEnabled("Night mode: yes"))
        assertEquals(true, UiModeNightParser.isNightEnabled("Night mode: yes\n"))
    }

    @Test
    fun parsesExactNightModeNo() {
        assertEquals(false, UiModeNightParser.isNightEnabled("Night mode: no"))
        assertEquals(false, UiModeNightParser.isNightEnabled("  Night mode: no  "))
    }

    @Test
    fun darkMustNotMatchNightModeNo() {
        // Regression: loose 'night' matching previously treated "Night mode: no" as dark.
        assertEquals(false, UiModeNightParser.isNightEnabled("Night mode: no"))
        assertEquals(false, UiModeNightParser.matchesExpected(theme = "dark", raw = "Night mode: no"))
        assertEquals(true, UiModeNightParser.matchesExpected(theme = "light", raw = "Night mode: no"))
        assertEquals(true, UiModeNightParser.matchesExpected(theme = "dark", raw = "Night mode: yes"))
    }

    @Test
    fun unknownRawReturnsNull() {
        assertNull(UiModeNightParser.isNightEnabled("Night mode: maybe"))
        assertNull(UiModeNightParser.isNightEnabled(""))
    }
}
