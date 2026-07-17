package com.logflare.android.visual

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
abstract class VisualSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    protected fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LogflareandroidTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.requiredSize(412.dp, 915.dp),
                    color = MaterialTheme.colorScheme.background,
                    content = content,
                )
            }
        }
        // Advance far enough for CircularProgressIndicator's indeterminate arc to be visible.
        compose.mainClock.advanceTimeBy(250L)
        compose.onRoot().captureRoboImage(
            "${name}_${if (darkTheme) "dark" else "light"}.png",
        )
    }
}
