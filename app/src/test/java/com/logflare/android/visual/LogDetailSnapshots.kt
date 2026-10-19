package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogEventDetail
import com.logflare.android.feature.log.LogDetailContent
import com.logflare.android.feature.log.LogDetailUiState
import com.logflare.android.visual.ds.DsSnapshotTest
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import org.robolectric.annotation.Config

class LogDetailSnapshots : DsSnapshotTest() {

    @Composable
    private fun Detail(state: LogDetailUiState) = LogDetailContent(
        state = state,
        zone = ZoneOffset.UTC,
        onBack = {},
        onToggleRaw = {},
        onOpenShare = {},
        onCloseShare = {},
        onRetry = {},
        onOpenProject = {},
        onOpenLog = {},
    )

    @Test fun log_detail_collapsed_light() = capture("log_detail_collapsed", false) { Detail(LogDetailFixtures.collapsed) }
    @Test fun log_detail_collapsed_dark() = capture("log_detail_collapsed", true) { Detail(LogDetailFixtures.collapsed) }
    @Test fun log_detail_expanded_light() = capture("log_detail_expanded", false) { Detail(LogDetailFixtures.expanded) }
    @Test fun log_detail_expanded_dark() = capture("log_detail_expanded", true) { Detail(LogDetailFixtures.expanded) }
    // Korean (app language 한국어): long labels must not truncate or overflow.
    @Config(qualifiers = "+ko") @Test fun log_detail_expanded_ko_light() = capture("log_detail_expanded_ko", false) { Detail(LogDetailFixtures.expanded) }
    @Test fun log_detail_no_raw_light() = capture("log_detail_no_raw", false) { Detail(LogDetailFixtures.noRaw) }
    @Test fun log_detail_no_raw_dark() = capture("log_detail_no_raw", true) { Detail(LogDetailFixtures.noRaw) }
    @Test fun log_detail_share_sheet_light() = capture("log_detail_share_sheet", false) { Detail(LogDetailFixtures.shareSheet) }
    @Test fun log_detail_share_sheet_dark() = capture("log_detail_share_sheet", true) { Detail(LogDetailFixtures.shareSheet) }
    @Test fun log_detail_not_found_light() = capture("log_detail_not_found", false) { Detail(LogDetailFixtures.notFound) }
    @Test fun log_detail_not_found_dark() = capture("log_detail_not_found", true) { Detail(LogDetailFixtures.notFound) }
    @Test fun log_detail_loading_light() = capture("log_detail_loading", false) { Detail(LogDetailUiState()) }
    @Test fun log_detail_loading_dark() = capture("log_detail_loading", true) { Detail(LogDetailUiState()) }
}

object LogDetailFixtures {
    private const val RAW =
        "java.lang.NullPointerException: Cannot invoke \"Card.getToken()\" because \"card\" is null\n" +
            "  at com.shop.payment.PaymentService.charge(PaymentService.kt:142)\n" +
            "  at com.shop.order.OrderController.checkout(OrderController.kt:88)\n" +
            "  at com.shop.order.OrderController\$routes\$1.invokeSuspend(OrderController.kt:41)\n" +
            "  at io.ktor.server.routing.Route.handle(Route.kt:120)\n" +
            "  at io.ktor.server.engine.BaseApplicationEngine.execute(BaseApplicationEngine.kt:66)\n" +
            "  at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)\n" +
            "  at java.base/java.lang.Thread.run(Thread.java:1583)\n\n" +
            "request_id=7f3a91c2 user_id=10482 amount=39000 currency=KRW"

    private val event = LogEvent(
        id = 12,
        projectId = 1,
        logFileName = "payment.log",
        type = "NullPointerException",
        message = "NullPointerException in PaymentService",
        level = LfLevel.Error,
        lastSeen = Instant.parse("2026-10-18T15:39:12Z"),
        firstSeen = Instant.parse("2026-10-18T15:31:04Z"),
        occurrences = 3,
    )

    private val detail = LogEventDetail(
        event = event,
        raw = RAW,
        recentIds = listOf(11, 7),
        occurredAt = Instant.parse("2026-10-18T15:39:12Z"),
        rowLevel = LfLevel.Error,
    )

    val collapsed = LogDetailUiState(detail = detail, projectName = "api-server", initialLoad = false)

    val expanded = collapsed.copy(rawExpanded = true)

    val noRaw = collapsed.copy(detail = detail.copy(raw = null, recentIds = emptyList()))

    val shareSheet = collapsed.copy(shareSheetOpen = true)

    val notFound = LogDetailUiState(initialLoad = false, error = com.logflare.android.feature.log.LogDetailError.NotFound)
}
