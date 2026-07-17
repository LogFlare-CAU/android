package com.logflare.android.visual

import com.logflare.android.feature.project.ProjectSettingsScreenContent
import com.logflare.android.feature.projectdetail.ProjectDetailScreenContent
import org.junit.Test

class ProjectDetailSnapshots : VisualSnapshotTest() {
    @Test fun project_settings_populated_light() {
        capture("project_settings_populated", darkTheme = false) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_settings_populated_dark() {
        capture("project_settings_populated", darkTheme = true) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_settings_error_light() {
        capture("project_settings_error", darkTheme = false) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(
                    saved = true,
                    error = "Unable to save project settings",
                ),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_settings_error_dark() {
        capture("project_settings_error", darkTheme = true) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(
                    saved = true,
                    error = "Unable to save project settings",
                ),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_settings_loading_light() {
        capture("project_settings_loading", darkTheme = false) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true, loading = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_settings_loading_dark() {
        capture("project_settings_loading", darkTheme = true) {
            ProjectSettingsScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true, loading = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_detail_populated_light() {
        capture("project_detail_populated", darkTheme = false) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(),
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

    @Test fun project_detail_populated_dark() {
        capture("project_detail_populated", darkTheme = true) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(),
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

    @Test fun project_detail_empty_light() {
        capture("project_detail_empty", darkTheme = false) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(empty = true),
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

    @Test fun project_detail_empty_dark() {
        capture("project_detail_empty", darkTheme = true) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(empty = true),
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

    @Test fun project_detail_error_light() {
        capture("project_detail_error", darkTheme = false) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(error = "Unable to load project detail"),
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

    @Test fun project_detail_error_dark() {
        capture("project_detail_error", darkTheme = true) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(error = "Unable to load project detail"),
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

    @Test fun project_detail_loading_light() {
        capture("project_detail_loading", darkTheme = false) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(loading = true),
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

    @Test fun project_detail_loading_dark() {
        capture("project_detail_loading", darkTheme = true) {
            ProjectDetailScreenContent(
                uiState = SnapshotFixtures.projectDetail(loading = true),
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
}
