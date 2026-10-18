package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.LogSortOrder
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.log.LogsContent
import com.logflare.android.feature.log.LogsSheet
import com.logflare.android.feature.log.LogsUiState
import com.logflare.android.feature.log.groupByDay
import com.logflare.android.visual.ds.DsSnapshotTest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test

class LogsSnapshots : DsSnapshotTest() {

    @Composable
    private fun Logs(state: LogsUiState) = LogsContent(
        state = state,
        zone = LogsFixtures.ZONE,
        onToggleSearch = {},
        onSearch = {},
        onOpenSheet = {},
        onCloseSheet = {},
        onSelectProject = {},
        onSelectLevel = {},
        onSelectSort = {},
        onClearFilters = {},
        onRefresh = {},
        onEndReached = {},
        onOpenLog = {},
    )

    @Test fun logs_populated_light() = capture("logs_populated", false) { Logs(LogsFixtures.populated) }
    @Test fun logs_populated_dark() = capture("logs_populated", true) { Logs(LogsFixtures.populated) }
    @Test fun logs_filtered_active_chips_light() = capture("logs_filtered_active_chips", false) { Logs(LogsFixtures.filtered) }
    @Test fun logs_filtered_active_chips_dark() = capture("logs_filtered_active_chips", true) { Logs(LogsFixtures.filtered) }
    @Test fun logs_search_open_light() = capture("logs_search_open", false) { Logs(LogsFixtures.searchOpen) }
    @Test fun logs_search_open_dark() = capture("logs_search_open", true) { Logs(LogsFixtures.searchOpen) }
    @Test fun logs_empty_filtered_light() = capture("logs_empty_filtered", false) { Logs(LogsFixtures.emptyFiltered) }
    @Test fun logs_empty_filtered_dark() = capture("logs_empty_filtered", true) { Logs(LogsFixtures.emptyFiltered) }
    @Test fun logs_empty_light() = capture("logs_empty", false) { Logs(LogsFixtures.empty) }
    @Test fun logs_empty_dark() = capture("logs_empty", true) { Logs(LogsFixtures.empty) }
    @Test fun logs_initial_load_light() = capture("logs_initial_load", false) { Logs(LogsUiState()) }
    @Test fun logs_initial_load_dark() = capture("logs_initial_load", true) { Logs(LogsUiState()) }
    @Test fun logs_long_text_light() = capture("logs_long_text", false) { Logs(LogsFixtures.longText) }
    @Test fun logs_long_text_dark() = capture("logs_long_text", true) { Logs(LogsFixtures.longText) }
    @Test fun logs_long_project_chip_light() = capture("logs_long_project_chip", false) { Logs(LogsFixtures.longProjectChip) }
    @Test fun logs_long_project_chip_dark() = capture("logs_long_project_chip", true) { Logs(LogsFixtures.longProjectChip) }
    @Test fun logs_level_sheet_light() = capture("logs_level_sheet", false) { Logs(LogsFixtures.levelSheet) }
    @Test fun logs_level_sheet_dark() = capture("logs_level_sheet", true) { Logs(LogsFixtures.levelSheet) }
}

object LogsFixtures {
    val ZONE: ZoneId = ZoneOffset.UTC
    private val TODAY: LocalDate = LocalDate.of(2026, 10, 18)

    private fun at(day: LocalDate, hhmm: String): Instant = Instant.parse("${day}T$hhmm:00Z")

    private fun log(id: Int, projectId: Int, message: String, level: LfLevel, seen: Instant, count: Int = 1) = LogEvent(
        id = id,
        projectId = projectId,
        logFileName = "app.log",
        type = "RuntimeError",
        message = message,
        level = level,
        lastSeen = seen,
        firstSeen = seen,
        occurrences = count,
    )

    private fun project(id: Int, name: String) = ProjectItem(id, name, emptyList(), 0, 0, null)

    private val projects = listOf(project(1, "api-server"), project(2, "worker"), project(3, "web-frontend"), project(4, "batch-jobs"))

    private val yesterday = TODAY.minusDays(1)
    private val items = listOf(
        log(1, 1, "NullPointerException in PaymentService", LfLevel.Error, at(TODAY, "15:39"), 3),
        log(2, 2, "Connection pool exhausted (max=20)", LfLevel.Error, at(TODAY, "15:24")),
        log(3, 1, "Slow query: SELECT * FROM logs (2.4s)", LfLevel.Warning, at(TODAY, "15:01"), 2),
        log(4, 3, "Deployed build 1.4.2", LfLevel.Info, at(TODAY, "14:48")),
        log(5, 2, "Disk usage above 95% on /var/lib", LfLevel.Critical, at(TODAY, "14:02")),
        log(6, 1, "Cache miss ratio 0.42 for /projects", LfLevel.Debug, at(TODAY, "11:15"), 6),
        log(8, 1, "TimeoutError: upstream auth-service", LfLevel.Error, at(yesterday, "23:51")),
        log(9, 2, "Failed to parse webhook payload", LfLevel.Error, at(yesterday, "19:08"), 2),
        log(10, 3, "Uncaught TypeError in Checkout.tsx", LfLevel.Error, at(yesterday, "16:40"), 4),
    )

    private fun state(query: LogQuery, logs: List<LogEvent>, hasMore: Boolean = false) = LogsUiState(
        query = query,
        groups = groupByDay(logs, TODAY, ZONE),
        count = logs.size,
        hasMore = hasMore,
        initialLoad = false,
        projects = projects,
    )

    val populated = state(LogQuery(), items, hasMore = true)

    val filtered = state(
        LogQuery(projectId = 1, minLevel = LfLevel.Error, sort = LogSortOrder.Oldest),
        items.filter { it.projectId == 1 && it.level >= LfLevel.Error }.reversed(),
    )

    val searchOpen = state(LogQuery(text = "error"), items.filter { it.message.contains("Error") })
        .copy(searchOpen = true, search = "error")

    val emptyFiltered = state(LogQuery(minLevel = LfLevel.Critical, text = "zzz"), emptyList())
        .copy(searchOpen = true, search = "zzz")

    val empty = state(LogQuery(), emptyList())

    private const val LONG_NAME = "payments-gateway-eu-west-1-production-canary-blue-green-shadow-traffic-mirror-replica"
    private const val LONG_MESSAGE =
        "java.lang.IllegalStateException:Expected_BEGIN_OBJECT_but_was_STRING_at_line_1_column_1_path_\$.data.items[0].payload"

    /** Long message and project name in rows (no project filter, so rows name their project). */
    val longText = state(
        LogQuery(minLevel = LfLevel.Warning),
        listOf(log(21, 9, LONG_MESSAGE, LfLevel.Critical, at(TODAY, "15:40"), 1234)) + items.take(3),
    ).copy(projects = projects + project(9, LONG_NAME))

    /** Long project name in the active Project chip. */
    val longProjectChip = state(LogQuery(projectId = 9), items.take(2))
        .copy(projects = projects + project(9, LONG_NAME))

    val levelSheet = populated.copy(sheet = LogsSheet.Level)
}
