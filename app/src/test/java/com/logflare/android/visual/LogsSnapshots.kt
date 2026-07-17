package com.logflare.android.visual

import com.logflare.android.feature.log.LogDetailScreenContent
import com.logflare.android.feature.log.LogListScreenContent
import org.junit.Test

class LogsSnapshots : VisualSnapshotTest() {
    @Test fun logs_populated_light() {
        capture("logs_populated", darkTheme = false) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_populated_dark() {
        capture("logs_populated", darkTheme = true) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_empty_light() {
        capture("logs_empty", darkTheme = false) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(empty = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_empty_dark() {
        capture("logs_empty", darkTheme = true) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(empty = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_filtered_empty_light() {
        capture("logs_filtered_empty", darkTheme = false) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(filteredEmpty = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_filtered_empty_dark() {
        capture("logs_filtered_empty", darkTheme = true) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(filteredEmpty = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_error_light() {
        capture("logs_error", darkTheme = false) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(error = "Network unavailable"),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_error_dark() {
        capture("logs_error", darkTheme = true) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(error = "Network unavailable"),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_loading_more_light() {
        capture("logs_loading_more", darkTheme = false) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(loadingMore = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun logs_loading_more_dark() {
        capture("logs_loading_more", darkTheme = true) {
            LogListScreenContent(
                uiState = SnapshotFixtures.logs(loadingMore = true),
                onAction = { _ -> },
            )
        }
    }

    @Test fun log_detail_populated_light() {
        capture("log_detail_populated", darkTheme = false) {
            LogDetailScreenContent(
                onBack = {},
                log = SnapshotFixtures.logDetailPopulated(),
            )
        }
    }

    @Test fun log_detail_populated_dark() {
        capture("log_detail_populated", darkTheme = true) {
            LogDetailScreenContent(
                onBack = {},
                log = SnapshotFixtures.logDetailPopulated(),
            )
        }
    }

    @Test fun log_detail_empty_light() {
        capture("log_detail_empty", darkTheme = false) {
            LogDetailScreenContent(
                onBack = {},
                log = null,
            )
        }
    }

    @Test fun log_detail_empty_dark() {
        capture("log_detail_empty", darkTheme = true) {
            LogDetailScreenContent(
                onBack = {},
                log = null,
            )
        }
    }
}
