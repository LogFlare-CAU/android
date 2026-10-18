package com.example.logflare.core.designsystem.v2

import com.example.logflare.core.designsystem.v2.components.thresholdHighlighted
import org.junit.Assert.assertEquals
import org.junit.Test

class ThresholdHighlightTest {
    @Test fun warningAndAbove() {
        assertEquals(setOf(LfLevel.Warning, LfLevel.Error, LfLevel.Critical), thresholdHighlighted(LfLevel.Warning))
    }

    @Test fun criticalOnly() {
        assertEquals(setOf(LfLevel.Critical), thresholdHighlighted(LfLevel.Critical))
    }

    @Test fun debugIsAll() {
        assertEquals(LfLevel.entries.toSet(), thresholdHighlighted(LfLevel.Debug))
    }
}
