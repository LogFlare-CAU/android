package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.logflare.android.feature.project.ProjectSettingsActions
import com.logflare.android.feature.project.ProjectSettingsContent
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
                ProjectSettingsContent(
                    state = ProjectSettingsFixtures.admin,
                    actions = ProjectSettingsActions(),
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectSettings).assertExists()
    }
}
