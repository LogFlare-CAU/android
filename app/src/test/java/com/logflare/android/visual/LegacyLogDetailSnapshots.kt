package com.logflare.android.visual

import com.logflare.android.feature.log.LogDetailScreenContent
import org.junit.Test

/** The pre-rework Log detail screen (moved out of LogsSnapshots); replaced with that screen. */
class LegacyLogDetailSnapshots : VisualSnapshotTest() {
    @Test fun log_detail_populated_light() {
        capture("log_detail_populated", darkTheme = false) {
            LogDetailScreenContent(onBack = {}, log = SnapshotFixtures.logDetailPopulated())
        }
    }

    @Test fun log_detail_populated_dark() {
        capture("log_detail_populated", darkTheme = true) {
            LogDetailScreenContent(onBack = {}, log = SnapshotFixtures.logDetailPopulated())
        }
    }

    @Test fun log_detail_empty_light() {
        capture("log_detail_empty", darkTheme = false) {
            LogDetailScreenContent(onBack = {}, log = null)
        }
    }

    @Test fun log_detail_empty_dark() {
        capture("log_detail_empty", darkTheme = true) {
            LogDetailScreenContent(onBack = {}, log = null)
        }
    }
}
