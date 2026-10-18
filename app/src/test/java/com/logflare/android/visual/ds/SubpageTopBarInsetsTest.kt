package com.logflare.android.visual.ds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class SubpageTopBarInsetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun row_is56dp_belowStatusBar() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                Box(Modifier.testTag("root")) {
                    SubpageTopBar(
                        title = "api-server",
                        onNav = {},
                        windowInsets = WindowInsets(top = 24.dp),
                    )
                }
            }
        }
        compose.onNodeWithTag("SubpageTopBar.row").assertHeightIsEqualTo(56.dp)
        // The inset is added outside the row: 24dp + 56dp, never subtracted from it.
        compose.onNodeWithTag("root").assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun rootIsAtLeast56dp_withDefaultInsets() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                Box(Modifier.testTag("root")) { SubpageTopBar(title = "t", onNav = {}) }
            }
        }
        compose.onNodeWithTag("SubpageTopBar.row").assertHeightIsEqualTo(56.dp)
        compose.onNodeWithTag("root").assertHeightIsAtLeast(56.dp)
    }
}
