package com.logflare.android.visual

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.logflare.core.model.ProjectDTO
import com.logflare.android.feature.project.ProjectCreateScreenContent
import com.logflare.android.feature.project.ProjectCreateUiState
import com.logflare.android.feature.project.ProjectListScreenContent
import com.logflare.android.feature.project.ProjectSettingsScreenContent
import com.logflare.android.feature.project.ProjectsUiState
import com.logflare.android.feature.projectdetail.ProjectDetailScreenContent
import com.logflare.android.feature.projectdetail.ProjectDetailUiState
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun projectListRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectListScreenContent(
                    uiState = ProjectsUiState(
                        loading = false,
                        items = listOf(ProjectDTO(id = 1, name = "Payments")),
                    ),
                    onProjectClick = { _ -> },
                    onRefresh = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Projects).assertExists()
    }

    @Test fun projectCreateRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectCreateScreenContent(
                    uiState = ProjectCreateUiState(name = "New Project", nameValid = true),
                    onAction = { _ -> },
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectCreate).assertExists()
    }

    @Test fun projectCreateInitialSaveIsDisabled() {
        val uiState = SnapshotFixtures.projectEditor()
        assertFalse(uiState.nameValid)
        assertFalse(uiState.saved)
        assertTrue(uiState.name.isEmpty())

        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectCreateScreenContent(
                    uiState = uiState,
                    onAction = { _ -> },
                )
            }
        }
        // Project name Save is the first "Save" affordance; nameValid=false disables it.
        compose.onAllNodesWithText("Save")[0].assertIsNotEnabled()
    }

    @Test fun projectSettingsRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectSettingsScreenContent(
                    uiState = ProjectCreateUiState(
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

    @Test fun projectDetailRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectDetailScreenContent(
                    uiState = ProjectDetailUiState(loading = false, projectName = "Payments"),
                    onBack = {},
                    onOpenProjectSettings = { _ -> },
                    onLevelSelected = { _ -> },
                    onLogfileSelected = { _ -> },
                    onSortSelected = { _ -> },
                    onLogClick = { _ -> },
                    onLoadMore = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectDetail).assertExists()
    }
}
