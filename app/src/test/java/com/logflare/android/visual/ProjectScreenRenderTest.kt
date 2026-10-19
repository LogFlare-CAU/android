package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.feature.project.ProjectSettingsActions
import com.logflare.android.feature.project.ProjectSettingsContent
import com.logflare.android.ui.VisualQaTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun projectSettingsRendersFromStateOnly() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                ProjectSettingsContent(
                    state = ProjectSettingsFixtures.admin,
                    actions = ProjectSettingsActions(),
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectSettings).assertExists()
    }
}
