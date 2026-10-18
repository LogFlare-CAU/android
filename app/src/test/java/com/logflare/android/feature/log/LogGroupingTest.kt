package com.logflare.android.feature.log

import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.LogEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LogGroupingTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 18)

    private fun log(id: Int, lastSeen: String) = LogEvent(
        id = id,
        projectId = 1,
        logFileName = null,
        type = null,
        message = "m$id",
        level = LfLevel.Error,
        lastSeen = Instant.parse(lastSeen),
        firstSeen = Instant.parse(lastSeen),
        occurrences = 1,
    )

    @Test
    fun groupByDay_todayYesterdayDate() {
        val items = listOf(
            log(1, "2026-10-18T01:00:00Z"), // 10:00 KST, today
            log(2, "2026-10-17T15:00:00Z"), // 00:00 KST on the 18th: today in the zone, not in UTC
            log(3, "2026-10-17T14:59:00Z"), // 23:59 KST on the 17th
            log(4, "2026-10-16T03:00:00Z"),
            log(5, "2025-12-31T03:00:00Z"), // another year
        )

        val groups = groupByDay(items, today, zone)

        assertEquals(
            listOf(
                DayLabel.Today,
                DayLabel.Yesterday,
                DayLabel.Date(LocalDate.of(2026, 10, 16), showYear = false),
                DayLabel.Date(LocalDate.of(2025, 12, 31), showYear = true),
            ),
            groups.map { it.label },
        )
        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4), listOf(5)), groups.map { g -> g.items.map { it.id } })
    }

    @Test
    fun groupByDay_oldestKeepsAscending() {
        val items = listOf(
            log(1, "2026-10-16T03:00:00Z"),
            log(2, "2026-10-16T04:00:00Z"),
            log(3, "2026-10-17T03:00:00Z"),
            log(4, "2026-10-18T01:00:00Z"),
            log(5, "2026-10-18T02:00:00Z"),
        )

        val groups = groupByDay(items, today, zone)

        assertEquals(
            listOf(DayLabel.Date(LocalDate.of(2026, 10, 16), showYear = false), DayLabel.Yesterday, DayLabel.Today),
            groups.map { it.label },
        )
        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4, 5)), groups.map { g -> g.items.map { it.id } })
    }

    @Test
    fun groupByDay_levelSort_keepsServerOrderWithinAndAcrossDays() {
        // Highest level first: a day can only head the list where its first row does.
        val items = listOf(
            log(1, "2026-10-17T03:00:00Z"),
            log(2, "2026-10-18T01:00:00Z"),
            log(3, "2026-10-17T05:00:00Z"),
        )

        val groups = groupByDay(items, today, zone)

        assertEquals(listOf(DayLabel.Yesterday, DayLabel.Today), groups.map { it.label })
        assertEquals(listOf(listOf(1, 3), listOf(2)), groups.map { g -> g.items.map { it.id } })
    }

    @Test
    fun groupByDay_empty() {
        assertEquals(emptyList<DayGroup>(), groupByDay(emptyList(), today, zone))
    }
}
