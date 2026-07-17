package com.logflare.android.visual

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun lightAndDarkExposeDifferentSurfaces() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        compose.setContent {
            Column {
                LogflareandroidTheme(darkTheme = false) {
                    light = MaterialTheme.colorScheme.background
                }
                LogflareandroidTheme(darkTheme = true) {
                    dark = MaterialTheme.colorScheme.background
                }
            }
        }
        assertNotEquals(light, dark)
        assertTrue(light.luminance() > dark.luminance())
    }
}
