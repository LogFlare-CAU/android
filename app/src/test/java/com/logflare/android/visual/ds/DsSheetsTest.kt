package com.logflare.android.visual.ds

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.MultiSelectSheet
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class DsSheetsTest {
    @get:Rule
    val compose = createComposeRule()

    private val levels = listOf(
        SheetOption("all", "All levels"),
        SheetOption("error", "Error"),
        SheetOption("warn", "Warning", description = "Warnings and above"),
    )

    @Test
    fun selectionSheet_selectingOption_callsSelectThenDismiss() {
        val events = mutableListOf<String>()
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                SelectionSheet(
                    title = "Log level",
                    options = levels,
                    selected = "all",
                    onSelect = { events += "select:$it" },
                    onDismiss = { events += "dismiss" },
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Error").performClick()
        compose.waitForIdle()

        assertEquals(listOf("select:error", "dismiss"), events)
    }

    @Test
    fun selectionSheet_showsCheckOnSelected() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                SelectionSheet(
                    title = "Log level",
                    options = levels,
                    selected = "warn",
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Log level").assertExists()
        compose.onNodeWithText("Warnings and above").assertExists()
        compose.onAllNodes(hasContentDescription("Selected")).assertCountEquals(1)
        // The check sits inside the "Warning" row (merged semantics).
        compose.onNode(hasText("Warning") and hasContentDescription("Selected"), useUnmergedTree = false)
            .assertExists()
    }

    @Test
    fun multiSelect_lockedCannotBeUnchecked() {
        var confirmed: Set<String>? = null
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                MultiSelectSheet(
                    title = "Members",
                    options = listOf(
                        SheetOption("admin", "admin", "Super Admin · always has access"),
                        SheetOption("jihyun", "jihyun", "Admin"),
                        SheetOption("minsu", "minsu", "Member"),
                    ),
                    selected = setOf("admin", "jihyun"),
                    locked = setOf("admin"),
                    onConfirm = { confirmed = it },
                    onDismiss = {},
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("admin").assertIsOn().assertIsNotEnabled()
        compose.onNodeWithText("admin").performClick()
        compose.onNodeWithText("admin").assertIsOn()

        compose.onNodeWithText("jihyun").performClick()
        compose.onNodeWithText("jihyun").assertIsOff()
        compose.onNodeWithText("minsu").performClick()
        compose.onNodeWithText("minsu").assertIsOn()

        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()

        assertEquals(setOf("admin", "minsu"), confirmed)
    }

    @Test
    fun confirmSheet_disabledConfirmDoesNotFire() {
        var confirms = 0
        var dismisses = 0
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                ConfirmSheet(
                    title = "Delete project?",
                    body = "This permanently removes api-server and all of its logs.",
                    confirmLabel = "Delete",
                    destructive = true,
                    onConfirm = { confirms++ },
                    onDismiss = { dismisses++ },
                    confirmEnabled = false,
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Delete").assertIsNotEnabled().performClick()
        compose.waitForIdle()
        assertEquals(0, confirms)

        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(1, dismisses)
    }
}
