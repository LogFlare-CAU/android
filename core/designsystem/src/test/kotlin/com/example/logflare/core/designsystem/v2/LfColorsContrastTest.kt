package com.example.logflare.core.designsystem.v2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.logflare.core.designsystem.v2.components.levelTileColors
import com.example.logflare.core.designsystem.v2.components.uncheckedCheckBoxBorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class LfColorsContrastTest {

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private val palettes = mapOf("light" to lfLightColors(), "dark" to lfDarkColors())

    private fun check(name: String, fg: (LfColors) -> Color, bg: (LfColors) -> Color, min: Double) {
        palettes.forEach { (p, c) ->
            val r = contrast(fg(c), bg(c))
            assertTrue("$p $name contrast $r < $min", r >= min)
        }
    }

    @Test fun textPrimaryOnBackground() = check("textPrimary/background", { it.textPrimary }, { it.background }, 4.5)
    @Test fun textSecondaryOnSurface() = check("textSecondary/surface", { it.textSecondary }, { it.surface }, 4.5)
    @Test fun textMutedOnSurface() = check("textMuted/surface", { it.textMuted }, { it.surface }, 4.5)
    @Test fun textMutedOnBackground() = check("textMuted/background", { it.textMuted }, { it.background }, 4.5)
    @Test fun onPrimaryOnPrimary() = check("onPrimaryAction/primaryAction", { it.onPrimaryAction }, { it.primaryAction }, 4.5)
    @Test fun errorOnContainer() = check("error/errorContainer", { it.error }, { it.errorContainer }, 4.5)
    @Test fun successOnContainer() = check("success/successContainer", { it.success }, { it.successContainer }, 3.0)
    @Test fun warningOnContainer() = check("warning/warningContainer", { it.warning }, { it.warningContainer }, 4.5)
    @Test fun iconOnSurface() = check("icon/surface", { it.icon }, { it.surface }, 3.0)
    @Test fun onCriticalOnCritical() = check("onCritical/critical", { it.onCritical }, { it.critical }, 4.5)
    @Test fun onDisabledOnDisabled() = check("onDisabled/disabled", { it.onDisabled }, { it.disabled }, 3.0)

    /** A control's boundary needs 3:1 (WCAG 1.4.11); the unchecked box has nothing else. */
    @Test fun uncheckedCheckBoxBorderOnSurface() =
        check("uncheckedCheckBoxBorder/surface", { uncheckedCheckBoxBorder(it) }, { it.surface }, 3.0)

    /** Glyph on its tile; LevelBadge draws its label with the same pair, so text contrast applies. */
    @Test
    fun levelGlyphOnTile() = LfLevel.entries.forEach { level ->
        check("$level glyph/tile", { levelTileColors(it, level).second }, { levelTileColors(it, level).first }, 4.5)
    }

    /** Critical must be a loud tile that stands off the card on its own (WCAG 1.4.11 3:1). */
    @Test fun criticalTileOnSurface() = check("critical/surface", { it.critical }, { it.surface }, 3.0)

    /** Error's dark tile is quiet, so its glyph colour must also read against the card itself. */
    @Test fun errorOnSurface() = check("error/surface", { it.error }, { it.surface }, 4.5)

    @Test fun textSecondaryOnBackground() = check("textSecondary/background", { it.textSecondary }, { it.background }, 4.5)
    @Test fun textSecondaryOnFill() = check("textSecondary/fill", { it.textSecondary }, { it.fill }, 4.5)

    /** Dark cards must visibly lift off the page (was 1.14:1). */
    @Test
    fun darkSurfaceSeparatesFromBackground() {
        val c = lfDarkColors()
        val r = contrast(c.surface, c.background)
        assertTrue("dark surface/background $r < 1.2", r >= 1.2)
    }

    @Test
    fun lightValuesMatchSpec() {
        val c = lfLightColors()
        fun hex(color: Color) = color.toArgb() and 0xFFFFFF
        assertEquals(0xF2F4F6, hex(c.background))
        assertEquals(0xFFFFFF, hex(c.surface))
        assertEquals(0x191F28, hex(c.textPrimary))
        assertEquals(0x4E5968, hex(c.textSecondary))
        assertEquals(0x657080, hex(c.textMuted))
        assertEquals(0x8B95A1, hex(c.icon))
        assertEquals(0xE5E8EB, hex(c.divider))
        assertEquals(0xE5E8EB, hex(c.fill))
        assertEquals(0xD22030, hex(c.error))
        assertEquals(0xFFEBEE, hex(c.errorContainer))
        assertEquals(0xA65100, hex(c.warning))
        assertEquals(0xFFF3E0, hex(c.warningContainer))
        assertEquals(0x2B0A0E, hex(c.critical))
        assertEquals(0xFFFFFF, hex(c.onCritical))
        assertEquals(0x191F28, hex(c.primaryAction))
        assertEquals(0xFFFFFF, hex(c.onPrimaryAction))
        assertEquals(0xD1D6DB, hex(c.disabled))
    }

    @Test
    fun darkValuesMatchSpec() {
        val c = lfDarkColors()
        fun hex(color: Color) = color.toArgb() and 0xFFFFFF
        assertEquals(0x0A0B0E, hex(c.background))
        assertEquals(0x1E2228, hex(c.surface))
        assertEquals(0xF2F4F6, hex(c.textPrimary))
        assertEquals(0xC2C9D1, hex(c.textSecondary))
        assertEquals(0xA7B0BB, hex(c.textMuted))
        assertEquals(0x9AA4B0, hex(c.icon))
        assertEquals(0x363C44, hex(c.divider))
        assertEquals(0x2E333B, hex(c.fill))
        assertEquals(0xFF8590, hex(c.error))
        assertEquals(0x4D1B22, hex(c.errorContainer))
        assertEquals(0xD8363D, hex(c.critical))
        assertEquals(0xFFFFFF, hex(c.onCritical))
    }
}
