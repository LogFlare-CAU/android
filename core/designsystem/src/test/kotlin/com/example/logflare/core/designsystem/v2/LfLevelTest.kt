package com.example.logflare.core.designsystem.v2

import org.junit.Assert.assertEquals
import org.junit.Test

class LfLevelTest {
    @Test
    fun parse_isCaseInsensitive_andMapsWarn() {
        assertEquals(LfLevel.Warning, LfLevel.parse("WARN"))
        assertEquals(LfLevel.Warning, LfLevel.parse("warning"))
        assertEquals(LfLevel.Error, LfLevel.parse("Error"))
        assertEquals(LfLevel.Critical, LfLevel.parse("CRITICAL"))
        assertEquals(LfLevel.Info, LfLevel.parse("info"))
        assertEquals(LfLevel.Debug, LfLevel.parse("debug"))
    }

    @Test
    fun parse_unknownOrNull_isDebug() {
        assertEquals(LfLevel.Debug, LfLevel.parse(null))
        assertEquals(LfLevel.Debug, LfLevel.parse("bogus"))
    }
}
