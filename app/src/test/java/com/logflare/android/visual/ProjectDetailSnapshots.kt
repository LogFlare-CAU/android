package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogFileRef
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.projectdetail.ProjectDetailContent
import com.logflare.android.feature.projectdetail.ProjectDetailSheet
import com.logflare.android.feature.projectdetail.ProjectDetailUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test

class ProjectDetailSnapshots : DsSnapshotTest() {

    @Composable
    private fun Detail(state: ProjectDetailUiState) = ProjectDetailContent(
        state = state,
        zone = ProjectDetailFixtures.ZONE,
        today = ProjectDetailFixtures.TODAY,
        onBack = {},
        onOpenSettings = {},
        onToggleErrors = {},
        onToggleWarnings = {},
        onOpenSheet = {},
        onCloseSheet = {},
        onSelectFile = {},
        onSelectLevel = {},
        onSelectSort = {},
        onClearFilters = {},
        onRefresh = {},
        onEndReached = {},
        onOpenLog = {},
    )

    @Test fun project_detail_populated_light() = capture("project_detail_populated", false) { Detail(ProjectDetailFixtures.populated) }
    @Test fun project_detail_populated_dark() = capture("project_detail_populated", true) { Detail(ProjectDetailFixtures.populated) }
    @Test fun project_detail_filtered_light() = capture("project_detail_filtered", false) { Detail(ProjectDetailFixtures.filtered) }
    @Test fun project_detail_filtered_dark() = capture("project_detail_filtered", true) { Detail(ProjectDetailFixtures.filtered) }
    @Test fun project_detail_empty_light() = capture("project_detail_empty", false) { Detail(ProjectDetailFixtures.emptyFiltered) }
    @Test fun project_detail_empty_dark() = capture("project_detail_empty", true) { Detail(ProjectDetailFixtures.emptyFiltered) }
    @Test fun project_detail_no_logs_light() = capture("project_detail_no_logs", false) { Detail(ProjectDetailFixtures.noLogs) }
    @Test fun project_detail_no_logs_dark() = capture("project_detail_no_logs", true) { Detail(ProjectDetailFixtures.noLogs) }
    @Test fun project_detail_not_found_light() = capture("project_detail_not_found", false) { Detail(ProjectDetailFixtures.notFound) }
    @Test fun project_detail_not_found_dark() = capture("project_detail_not_found", true) { Detail(ProjectDetailFixtures.notFound) }
    @Test fun project_detail_file_sheet_light() = capture("project_detail_file_sheet", false) { Detail(ProjectDetailFixtures.fileSheet) }
    @Test fun project_detail_file_sheet_dark() = capture("project_detail_file_sheet", true) { Detail(ProjectDetailFixtures.fileSheet) }
    @Test fun project_detail_initial_load_light() = capture("project_detail_initial_load", false) { Detail(ProjectDetailUiState()) }
    @Test fun project_detail_initial_load_dark() = capture("project_detail_initial_load", true) { Detail(ProjectDetailUiState()) }
    @Test fun project_detail_load_failed_light() = capture("project_detail_load_failed", false) { Detail(ProjectDetailFixtures.loadFailed) }
    @Test fun project_detail_load_failed_dark() = capture("project_detail_load_failed", true) { Detail(ProjectDetailFixtures.loadFailed) }
}

object ProjectDetailFixtures {
    val ZONE: ZoneId = ZoneOffset.UTC
    val TODAY: LocalDate = LocalDate.of(2026, 10, 18)

    private fun at(day: LocalDate, hhmm: String): Instant = Instant.parse("${day}T$hhmm:00Z")

    private fun log(id: Int, message: String, level: LfLevel, seen: Instant, count: Int = 1) = LogEvent(
        id = id,
        projectId = 1,
        logFileName = "app.log",
        type = "RuntimeError",
        message = message,
        level = level,
        lastSeen = seen,
        firstSeen = seen,
        occurrences = count,
    )

    private val project = ProjectItem(
        id = 1,
        name = "api-server",
        logFiles = listOf(LogFileRef(10, "payment.log"), LogFileRef(11, "db.log"), LogFileRef(12, "app.log")),
        errorCount24h = 4,
        warningCount24h = 2,
        lastErrorAt = null,
    )

    private val summary = HomeSummary(errors24h = 4, warnings24h = 2, projectCount = 1)

    private val items = listOf(
        log(1, "NullPointerException in PaymentService", LfLevel.Error, at(TODAY, "15:39"), 3),
        log(2, "Payment gateway unreachable for 60s", LfLevel.Critical, at(TODAY, "15:12")),
        log(3, "Slow query: SELECT * FROM logs (2.4s)", LfLevel.Warning, at(TODAY, "15:01"), 2),
        log(4, "Deployed build 1.4.2", LfLevel.Info, at(TODAY, "14:48")),
        log(5, "TimeoutError: upstream auth-service", LfLevel.Error, at(TODAY.minusDays(1), "23:20")),
        log(6, "Connection pool at 90% (18/20)", LfLevel.Warning, at(TODAY.minusDays(1), "19:55")),
        log(7, "Cache warmup finished in 412ms", LfLevel.Debug, at(TODAY.minusDays(1), "09:30"), 6),
    )

    private fun state(query: LogQuery = LogQuery(projectId = 1), logs: List<LogEvent> = items) = ProjectDetailUiState(
        project = project,
        summary = summary,
        query = query,
        items = logs,
        initialLoad = false,
    )

    val populated = state().copy(hasMore = true)

    val filtered = state(
        LogQuery(projectId = 1, minLevel = LfLevel.Error, logFileId = 10),
        items.filter { it.level >= LfLevel.Error },
    )

    val emptyFiltered = state(LogQuery(projectId = 1, minLevel = LfLevel.Critical, logFileId = 11), emptyList())

    val noLogs = state(logs = emptyList())

    val notFound = ProjectDetailUiState(initialLoad = false, notFound = true)

    val fileSheet = filtered.copy(sheet = ProjectDetailSheet.File)

    val loadFailed = ProjectDetailUiState(initialLoad = false, loadFailed = true)
}
