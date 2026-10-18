package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.logflare.android.feature.project.ProjectEditorUiState
import com.logflare.android.feature.project.ProjectSettingsScreenContent
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun projectSettingsRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectSettingsScreenContent(
                    uiState = ProjectEditorUiState(
                        name = "Payments",
                        nameValid = true,
                        saved = true,
                    ),
                    onAction = { _ -> },
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectSettings).assertExists()
    }
}
