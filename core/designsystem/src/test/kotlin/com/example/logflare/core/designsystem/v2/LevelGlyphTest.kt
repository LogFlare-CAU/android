package com.example.logflare.core.designsystem.v2

import com.example.logflare.core.designsystem.v2.components.levelGlyph
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelGlyphTest {
    /** Shape alone must tell levels apart (colour-blind users, Error vs Critical in dark). */
    @Test
    fun everyLevelHasItsOwnGlyph() {
        val names = LfLevel.entries.map { levelGlyph(it).name }
        assertEquals(names.toString(), LfLevel.entries.size, names.toSet().size)
    }
}
