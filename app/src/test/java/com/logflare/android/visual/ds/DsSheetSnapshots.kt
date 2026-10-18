package com.logflare.android.visual.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.MultiSelectSheet
import com.example.logflare.core.designsystem.v2.components.SelectionSheet
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Sheets live in their own window, so these capture the whole screen rather than the compose root. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class DsSheetSnapshots {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, darkTheme: Boolean, sheet: @Composable () -> Unit) {
        compose.setContent {
            LogFlareTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(Lf.colors.background))
                sheet()
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("ds_${name}_${if (darkTheme) "dark" else "light"}.png")
    }

    @Composable
    private fun Selection() = SelectionSheet(
        title = "Log level",
        options = listOf(
            SheetOption("all", "All levels"),
            SheetOption("critical", "Critical"),
            SheetOption("error", "Error"),
            SheetOption("warn", "Warning"),
            SheetOption("info", "Info"),
        ),
        selected = "error",
        onSelect = {},
        onDismiss = {},
    )

    @Composable
    private fun Members() = MultiSelectSheet(
        title = "Members",
        options = listOf(
            SheetOption("admin", "admin", "Super Admin · always has access"),
            SheetOption("jihyun", "jihyun", "Admin"),
            SheetOption("devops-bot", "devops-bot", "Member"),
            SheetOption("minsu", "minsu", "Member"),
        ),
        selected = setOf("admin", "jihyun", "devops-bot"),
        locked = setOf("admin"),
        onConfirm = {},
        onDismiss = {},
    )

    @Composable
    private fun DeleteConfirm() = ConfirmSheet(
        title = "Delete project?",
        body = "This permanently removes api-server and all of its logs. Type the project name to confirm.",
        confirmLabel = "Delete",
        destructive = true,
        onConfirm = {},
        onDismiss = {},
        confirmEnabled = false,
        extra = { AppTextField("", {}, "Project name", placeholder = "api-server") },
    )

    @Test fun selectionLight() = capture("sheet_selection", false) { Selection() }
    @Test fun selectionDark() = capture("sheet_selection", true) { Selection() }
    @Test fun membersLight() = capture("sheet_members", false) { Members() }
    @Test fun membersDark() = capture("sheet_members", true) { Members() }
    @Test fun confirmLight() = capture("sheet_confirm", false) { DeleteConfirm() }
    @Test fun confirmDark() = capture("sheet_confirm", true) { DeleteConfirm() }
}
