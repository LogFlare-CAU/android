package com.logflare.android.feature.home

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.visual.HomeFixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: HomeUiState) {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                HomeContent(
                    state = state,
                    now = HomeFixtures.NOW,
                    onRefresh = { calls += "refresh" },
                    onOpenSheet = { calls += "sheet:$it" },
                    onCloseSheet = { calls += "close" },
                    onSearch = { calls += "search:$it" },
                    onMarkRead = { calls += "read:$it" },
                    onMarkAllRead = { calls += "readAll" },
                    onOpenLog = { calls += "log:$it" },
                    onOpenProject = { calls += "project:$it" },
                    onOpenLogs = { calls += "logs:${it.minLevel}" },
                    onOpenProjects = { calls += "projects" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun notificationTap_marksReadAndOpensLog() {
        show(HomeFixtures.notificationsSheet)

        compose.onNodeWithText("Critical · worker").performClick()

        assertEquals(listOf("read:12", "close", "log:12"), calls)
    }

    @Test
    fun notificationTap_testPush_marksReadWithoutNavigating() {
        show(HomeFixtures.notificationsSheet)

        compose.onNodeWithText("Test notification", substring = true).performClick()

        assertEquals(listOf("read:0"), calls)
    }

    @Test
    fun statTaps_openLogsWithLevelOrProjects() {
        show(HomeFixtures.populated)

        compose.onNodeWithText("Errors · 24h").performClick()
        compose.onNodeWithText("Warnings").performClick()
        compose.onNode(hasText("Projects") and hasClickAction()).performClick()

        assertEquals(listOf("logs:${LfLevel.Error}", "logs:${LfLevel.Warning}", "projects"), calls)
    }

    @Test
    fun headerButtons_openSheets() {
        show(HomeFixtures.populated)

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithContentDescription("Notifications, unread").performClick()

        assertEquals(listOf("sheet:${HomeSheet.Search}", "sheet:${HomeSheet.Notifications}"), calls)
    }

    @Test
    fun emptyServer_showsProjectsAction() {
        show(HomeFixtures.empty)

        compose.onNodeWithText("No projects yet").assertExists()
        compose.onNodeWithText("Open Projects").performClick()

        assertEquals(listOf("projects"), calls)
    }

    @Test
    fun loadFailed_retryRefreshes() {
        show(HomeUiState(initialLoad = false, loadFailed = true))

        compose.onNodeWithText("Try again").performClick()

        assertEquals(listOf("refresh"), calls)
    }
}
