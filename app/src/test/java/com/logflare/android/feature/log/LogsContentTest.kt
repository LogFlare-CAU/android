package com.logflare.android.feature.log

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.ui.VisualQaTags
import java.time.Instant
import java.time.LocalDate
import com.logflare.android.visual.LogsFixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class LogsContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: LogsUiState) = show(mutableStateOf(state))

    private fun show(holder: MutableState<LogsUiState>) {
        compose.setContent {
            val state by holder
            LogFlareTheme(darkTheme = false) {
                LogsContent(
                    state = state,
                    zone = LogsFixtures.ZONE,
                    onToggleSearch = { calls += "toggleSearch" },
                    onSearch = { calls += "search:$it" },
                    onOpenSheet = { calls += "sheet:$it" },
                    onCloseSheet = { calls += "close" },
                    onSelectProject = { calls += "project:$it" },
                    onSelectLevel = { calls += "level:$it" },
                    onSelectSort = { calls += "sort:$it" },
                    onClearFilters = { calls += "clear" },
                    onRefresh = { calls += "refresh" },
                    onEndReached = { calls += "end" },
                    onOpenLog = { calls += "log:$it" },
                )
            }
        }
    }

    @Test
    fun rowsAndChips_reachCallbacks() {
        show(LogsFixtures.populated)
        compose.onNodeWithTag(VisualQaTags.Logs).assertExists()
        compose.onNodeWithText("9+ logs").assertExists()
        compose.onNodeWithText("Today").assertExists()

        compose.onNodeWithTag(VisualQaTags.logCard(2)).performClick()
        compose.onNodeWithTag(VisualQaTags.FilterProjects).performClick()
        compose.onNodeWithTag(VisualQaTags.FilterLogLevel).performClick()
        compose.onNodeWithTag(VisualQaTags.FilterSort).performClick()
        compose.onNodeWithContentDescription("Search logs").performClick()

        assertEquals(
            listOf("log:2", "sheet:Project", "sheet:Level", "sheet:Sort", "toggleSearch"),
            calls.filter { it != "end" },
        )
    }

    private fun manyRows(n: Int): List<LogEvent> = (1..n).map { i ->
        val seen = Instant.parse("2026-10-18T12:00:00Z").minusSeconds(i * 60L)
        LogEvent(i, 1, null, null, "message $i", LfLevel.Error, seen, seen, 1)
    }

    private fun page(rows: Int) = LogsUiState(
        groups = groupByDay(manyRows(rows), LocalDate.of(2026, 10, 18), LogsFixtures.ZONE),
        count = rows,
        hasMore = true,
        initialLoad = false,
    )

    @Test
    fun endReached_onceAtEnd_notAgainWhenAPageLandsBelow() {
        val holder = mutableStateOf(page(3))
        show(holder)
        compose.waitForIdle()
        // Three rows fill nothing: the end is in view.
        assertEquals(1, calls.count { it == "end" })

        // The next page lands; the user is still at the top, far from the new end.
        holder.value = page(60)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(1, calls.count { it == "end" })

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(62)
        compose.waitForIdle()
        assertEquals(2, calls.count { it == "end" })
    }

    @Test
    fun activeChips_showSelection() {
        show(LogsFixtures.filtered)
        compose.onNodeWithText("api-server").assertExists()
        compose.onNodeWithText("Error+").assertExists()
        compose.onNodeWithText("Oldest").assertExists()
        compose.onNodeWithText("2 logs").assertExists()
    }

    @Test
    fun emptyFiltered_clearFilters() {
        show(LogsFixtures.emptyFiltered)
        compose.onNodeWithText("No logs match").assertExists()
        compose.onNodeWithText("Clear filters").performClick()
        assertEquals("clear", calls.last())
    }

    @Test
    fun levelSheet_selects() {
        show(LogsFixtures.levelSheet)
        compose.onNodeWithText("Error and above").performClick()
        compose.waitForIdle()
        assertEquals("level:${LfLevel.Error}", calls.first { it.startsWith("level:") })
    }
}
