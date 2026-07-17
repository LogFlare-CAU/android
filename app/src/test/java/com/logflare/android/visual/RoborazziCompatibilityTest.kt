package com.logflare.android.visual

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class RoborazziCompatibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersComposeOnAgp9() {
        compose.setContent { Text("LogFlare visual QA") }
        compose.onRoot().captureRoboImage("compatibility.png")
    }
}
