package com.logflare.android.visual.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
abstract class DsSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    protected fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LogFlareTheme(darkTheme = darkTheme) {
                Box(
                    Modifier
                        .requiredSize(412.dp, 915.dp)
                        .background(Lf.colors.background),
                ) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(250L)
        compose.onRoot().captureRoboImage(
            "${name}_${if (darkTheme) "dark" else "light"}.png",
        )
    }
}
