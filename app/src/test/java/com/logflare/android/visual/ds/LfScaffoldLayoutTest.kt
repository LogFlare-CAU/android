package com.logflare.android.visual.ds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.example.logflare.core.designsystem.v2.components.BottomNavBar
import com.example.logflare.core.designsystem.v2.components.LfScaffold
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.NavItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class LfScaffoldLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val navItems = listOf(NavItem("a", "A", Icons.Filled.Home, Icons.Filled.Home))

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun toast_overlaysWithoutResizingContent() {
        val toast = LfToastState(CoroutineScope(Dispatchers.Main))
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                LfScaffold(
                    modifier = Modifier.testTag("scaffold"),
                    toastHost = toast,
                    contentInsets = WindowInsets(0.dp),
                ) { Box(Modifier.fillMaxSize().testTag("content")) }
            }
        }
        val before = bounds("content")
        compose.runOnIdle { toast.show("Copied", durationMs = 600_000) }
        compose.waitForIdle()
        compose.onNodeWithTag("LfToast").assertExists()
        assertEquals(before.height, bounds("content").height)
    }

    @Test
    fun toast_withoutBottomBar_staysAboveNavigationInset() {
        val toast = LfToastState(CoroutineScope(Dispatchers.Main))
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                LfScaffold(
                    modifier = Modifier.testTag("scaffold"),
                    toastHost = toast,
                    contentInsets = WindowInsets(bottom = 48.dp),
                ) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.runOnIdle { toast.show("Copied", durationMs = 600_000) }
        compose.waitForIdle()
        val root = bounds("scaffold")
        val t = bounds("LfToast")
        assertTrue("toast bottom ${t.bottom} must be <= ${root.bottom - 48.dp}", t.bottom <= root.bottom - 48.dp)
    }

    @Test
    fun toast_withBottomBar_sitsAboveBar() {
        val toast = LfToastState(CoroutineScope(Dispatchers.Main))
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                LfScaffold(
                    toastHost = toast,
                    contentInsets = WindowInsets(0.dp),
                    bottomBar = {
                        BottomNavBar(navItems, "a", {}, Modifier.testTag("nav"), windowInsets = WindowInsets(0.dp))
                    },
                ) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.runOnIdle { toast.show("Copied", durationMs = 600_000) }
        compose.waitForIdle()
        assertTrue(bounds("LfToast").bottom <= bounds("nav").top)
    }

    @Test
    fun bottomNavBar_ownsNavigationInset_andSurfaceCoversIt() {
        var inset by mutableStateOf(48.dp)
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                LfScaffold(
                    modifier = Modifier.testTag("scaffold"),
                    contentInsets = WindowInsets(0.dp),
                    bottomBar = {
                        BottomNavBar(
                            navItems, "a", {}, Modifier.testTag("nav"),
                            windowInsets = WindowInsets(bottom = inset),
                        )
                    },
                ) { Box(Modifier.fillMaxSize().testTag("content")) }
            }
        }
        val withInset: Dp = bounds("nav").height
        val root = bounds("scaffold")
        // Nav bar bounds (which carry the surface background) reach the very bottom of the screen.
        assertEquals(root.bottom, bounds("nav").bottom)
        // The content area is not padded by the inset: content + nav fill the root exactly.
        assertEquals(root.height, bounds("content").height + withInset)
        compose.runOnIdle { inset = 0.dp }
        compose.waitForIdle()
        val without = bounds("nav").height
        assertEquals(48f, (withInset - without).value, 0.5f)
        compose.onNodeWithTag("nav").assertHeightIsEqualTo(without)
    }
}
