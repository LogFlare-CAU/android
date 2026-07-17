package com.logflare.android.visual

import com.logflare.android.feature.home.HomeScreenContent
import org.junit.Test

class HomeSnapshots : VisualSnapshotTest() {
    @Test fun home_populated_light() {
        capture("home_populated", darkTheme = false) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth(),
                projectsState = SnapshotFixtures.projects(),
                logsState = SnapshotFixtures.logs(),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }

    @Test fun home_populated_dark() {
        capture("home_populated", darkTheme = true) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth(),
                projectsState = SnapshotFixtures.projects(),
                logsState = SnapshotFixtures.logs(),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }

    @Test fun home_empty_light() {
        capture("home_empty", darkTheme = false) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth(),
                projectsState = SnapshotFixtures.projects(empty = true),
                logsState = SnapshotFixtures.logs(empty = true),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }

    @Test fun home_empty_dark() {
        capture("home_empty", darkTheme = true) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth(),
                projectsState = SnapshotFixtures.projects(empty = true),
                logsState = SnapshotFixtures.logs(empty = true),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }

    @Test fun home_error_light() {
        capture("home_error", darkTheme = false) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth().copy(profileError = "Session expired"),
                projectsState = SnapshotFixtures.projects(error = "Unable to load projects"),
                logsState = SnapshotFixtures.logs(error = "Unable to load logs"),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }

    @Test fun home_error_dark() {
        capture("home_error", darkTheme = true) {
            HomeScreenContent(
                authState = SnapshotFixtures.auth().copy(profileError = "Session expired"),
                projectsState = SnapshotFixtures.projects(error = "Unable to load projects"),
                logsState = SnapshotFixtures.logs(error = "Unable to load logs"),
                onProjectSelected = { _ -> },
                onViewMoreLogs = {},
                onCreateProject = {},
            )
        }
    }
}
