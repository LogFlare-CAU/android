package com.logflare.android.visual

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeContractTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lightAndDarkExposeDifferentBackgrounds() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        var darkSurfaceVariant = Color.Unspecified
        var darkOnSurface = Color.Unspecified
        compose.setContent {
            Column {
                LogflareandroidTheme(darkTheme = false) {
                    light = MaterialTheme.colorScheme.background
                }
                LogflareandroidTheme(darkTheme = true) {
                    dark = MaterialTheme.colorScheme.background
                    darkSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                    darkOnSurface = MaterialTheme.colorScheme.onSurface
                }
            }
        }
        assertNotEquals(light, dark)
        assertTrue(light.luminance() > dark.luminance())
        assertNotEquals(darkSurfaceVariant, darkOnSurface)
    }

    @Test
    fun semanticTokensDifferAcrossThemes() {
        var lightMuted = Color.Unspecified
        var darkMuted = Color.Unspecified
        var lightInput = Color.Unspecified
        var darkInput = Color.Unspecified
        compose.setContent {
            Column {
                LogflareandroidTheme(darkTheme = false) {
                    lightMuted = AppTheme.colors.muted
                    lightInput = AppTheme.colors.input
                }
                LogflareandroidTheme(darkTheme = true) {
                    darkMuted = AppTheme.colors.muted
                    darkInput = AppTheme.colors.input
                }
            }
        }
        assertNotEquals(lightMuted, darkMuted)
        assertNotEquals(lightInput, darkInput)
        assertTrue(lightInput.luminance() > darkInput.luminance())
        assertTrue(MaterialThemeConsistentContrast(lightMuted, darkMuted))
    }

    private fun MaterialThemeConsistentContrast(lightMuted: Color, darkMuted: Color): Boolean {
        // Light muted should be darker than dark-theme muted (which sits on dark surfaces).
        return lightMuted.luminance() < darkMuted.luminance()
    }
}
