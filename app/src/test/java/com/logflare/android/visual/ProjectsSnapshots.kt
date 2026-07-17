package com.logflare.android.visual

import com.logflare.android.feature.project.ProjectCreateScreenContent
import com.logflare.android.feature.project.ProjectListScreenContent
import org.junit.Test

class ProjectsSnapshots : VisualSnapshotTest() {
    @Test fun projects_populated_light() {
        capture("projects_populated", darkTheme = false) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_populated_dark() {
        capture("projects_populated", darkTheme = true) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_empty_light() {
        capture("projects_empty", darkTheme = false) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(empty = true),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_empty_dark() {
        capture("projects_empty", darkTheme = true) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(empty = true),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_error_light() {
        capture("projects_error", darkTheme = false) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(error = "Unable to load projects"),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_error_dark() {
        capture("projects_error", darkTheme = true) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(error = "Unable to load projects"),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_loading_light() {
        capture("projects_loading", darkTheme = false) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(loading = true),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun projects_loading_dark() {
        capture("projects_loading", darkTheme = true) {
            ProjectListScreenContent(
                uiState = SnapshotFixtures.projects(loading = true),
                onProjectClick = { _ -> },
                onRefresh = {},
            )
        }
    }

    @Test fun project_create_initial_light() {
        capture("project_create_initial", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_initial_dark() {
        capture("project_create_initial", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_invalid_light() {
        capture("project_create_invalid", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(invalid = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_invalid_dark() {
        capture("project_create_invalid", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(invalid = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_saved_light() {
        capture("project_create_saved", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_saved_dark() {
        capture("project_create_saved", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(saved = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_loading_light() {
        capture("project_create_loading", darkTheme = false) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(loading = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun project_create_loading_dark() {
        capture("project_create_loading", darkTheme = true) {
            ProjectCreateScreenContent(
                uiState = SnapshotFixtures.projectEditor(loading = true),
                onAction = { _ -> },
            )
        }
    }
}
