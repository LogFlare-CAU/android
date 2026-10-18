package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.home.HomeContent
import com.logflare.android.feature.home.HomeSheet
import com.logflare.android.feature.home.HomeUiState
import com.logflare.android.feature.home.SearchResults
import com.logflare.android.push.HistoryEntry
import com.logflare.android.visual.ds.DsSnapshotTest
import java.time.Instant
import org.junit.Test

class HomeSnapshots : DsSnapshotTest() {

    @Composable
    private fun Home(state: HomeUiState) = HomeContent(
        state = state,
        now = HomeFixtures.NOW,
        onRefresh = {},
        onOpenSheet = {},
        onCloseSheet = {},
        onSearch = {},
        onMarkRead = {},
        onMarkAllRead = {},
        onOpenLog = {},
        onOpenProject = {},
        onOpenLogs = {},
        onOpenProjects = {},
    )

    @Test fun home_populated_light() = capture("home_populated", false) { Home(HomeFixtures.populated) }
    @Test fun home_populated_dark() = capture("home_populated", true) { Home(HomeFixtures.populated) }
    @Test fun home_initial_load_light() = capture("home_initial_load", false) { Home(HomeUiState()) }
    @Test fun home_initial_load_dark() = capture("home_initial_load", true) { Home(HomeUiState()) }
    @Test fun home_empty_light() = capture("home_empty", false) { Home(HomeFixtures.empty) }
    @Test fun home_empty_dark() = capture("home_empty", true) { Home(HomeFixtures.empty) }
    @Test fun home_long_text_light() = capture("home_long_text", false) { Home(HomeFixtures.longText) }
    @Test fun home_long_text_dark() = capture("home_long_text", true) { Home(HomeFixtures.longText) }
    @Test fun home_search_sheet_light() = capture("home_search_sheet", false) { Home(HomeFixtures.searchSheet) }
    @Test fun home_search_sheet_dark() = capture("home_search_sheet", true) { Home(HomeFixtures.searchSheet) }
    @Test fun home_notifications_sheet_light() = capture("home_notifications_sheet", false) { Home(HomeFixtures.notificationsSheet) }
    @Test fun home_notifications_sheet_dark() = capture("home_notifications_sheet", true) { Home(HomeFixtures.notificationsSheet) }
}

object HomeFixtures {
    val NOW: Instant = Instant.parse("2026-10-18T10:00:00Z")

    private fun ago(minutes: Long): Instant = NOW.minusSeconds(minutes * 60)

    private fun log(id: Int, projectId: Int, message: String, level: LfLevel, minutesAgo: Long) = LogEvent(
        id = id,
        projectId = projectId,
        logFileName = "app.log",
        type = "RuntimeError",
        message = message,
        level = level,
        lastSeen = ago(minutesAgo),
        firstSeen = ago(minutesAgo + 60),
        occurrences = 3,
    )

    private fun project(id: Int, name: String, errors: Int, lastErrorMinutesAgo: Long?) = ProjectItem(
        id = id,
        name = name,
        logFiles = emptyList(),
        errorCount24h = errors,
        warningCount24h = 0,
        lastErrorAt = lastErrorMinutesAgo?.let(::ago),
    )

    private val projects = listOf(
        project(1, "api-server", 8, 3),
        project(2, "worker", 4, 12),
        project(3, "web-frontend", 0, null),
    )
    private val names = (projects + project(4, "batch-jobs", 0, null)).associate { it.id to it.name }

    private val recent = listOf(
        log(11, 1, "NullPointerException in PaymentService", LfLevel.Error, 3),
        log(12, 2, "Disk usage above 95% on /var/lib", LfLevel.Critical, 12),
        log(13, 2, "Connection pool exhausted (max=20)", LfLevel.Error, 18),
        log(14, 1, "TimeoutError: auth-service did not answer", LfLevel.Error, 41),
        log(15, 4, "Nightly export failed", LfLevel.Error, 60 * 26),
    )

    /** Newest first, as NotificationHistory stores them. */
    private val alerts = listOf(
        HistoryEntry(11, 1, "NullPointerException", LfLevel.Error, "NullPointerException ×3", NOW.minusSeconds(3 * 60).toEpochMilli(), read = false),
        HistoryEntry(12, 2, "DiskFull", LfLevel.Critical, "Disk usage above 95%", NOW.minusSeconds(12 * 60).toEpochMilli(), read = false),
        HistoryEntry(0, null, "Test", LfLevel.Error, "Test notification from LogFlare", NOW.minusSeconds(30 * 60).toEpochMilli(), read = false),
        HistoryEntry(14, 1, "TimeoutError", LfLevel.Error, "TimeoutError: auth-service", NOW.minusSeconds(60 * 60).toEpochMilli(), read = true),
    )

    val populated = HomeUiState(
        summary = HomeSummary(errors24h = 12, warnings24h = 34, projectCount = 4),
        recent = recent,
        projects = projects,
        projectNames = names,
        initialLoad = false,
        unreadAlerts = 3,
        alerts = alerts,
    )

    val empty = HomeUiState(summary = HomeSummary(0, 0, 0), initialLoad = false)

    private const val LONG_NAME = "payments-gateway-eu-west-1-production-canary-blue-green-shadow-traffic-mirror-replica"
    private const val LONG_MESSAGE =
        "java.lang.IllegalStateException:Expected_BEGIN_OBJECT_but_was_STRING_at_line_1_column_1_path_\$.data.items[0].payload"

    val longText = populated.copy(
        recent = listOf(log(21, 9, LONG_MESSAGE, LfLevel.Critical, 2)) + recent.take(2),
        projects = listOf(project(9, LONG_NAME, 1234, 2)) + projects.take(2),
        projectNames = names + (9 to LONG_NAME),
    )

    val searchSheet = populated.copy(
        sheet = HomeSheet.Search,
        search = "api",
        // Matches by project name: api-server's errors, then the project.
        searchResults = SearchResults(logs = recent.filter { it.projectId == 1 }, projects = projects.take(1)),
    )

    val notificationsSheet = populated.copy(sheet = HomeSheet.Notifications)
}
