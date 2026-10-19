package com.logflare.android.data.store.model

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventDetailDTO
import com.example.logflare.core.model.RecentOccurrenceDTO
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogEventDetailMappingTest {
    private fun dto(recentIds: List<Int> = emptyList(), recent: List<RecentOccurrenceDTO> = emptyList()) = LogEventDetailDTO(
        id = 9, projectId = 1, message = "boom", level = "ERROR", timestamp = "2026-10-18T01:00:00",
        occurrences = 3, firstSeen = "2026-10-17T01:00:00", lastSeen = "2026-10-18T01:00:00",
        occurredAt = "2026-10-18T01:00:00", rowLevel = "ERROR", recentIds = recentIds, recent = recent,
    )

    @Test
    fun recentWithTimes_mapsToEarlierOccurrences() {
        val d = dto(
            recentIds = listOf(8, 7),
            recent = listOf(
                RecentOccurrenceDTO(8, "2026-10-18T00:30:00", "WARNING", "other"),
                RecentOccurrenceDTO(7, "2026-10-18T00:10:00Z", "ERROR"),
            ),
        ).toLogEventDetail()
        assertEquals(listOf(8, 7), d.recentIds)
        assertEquals(
            listOf(
                EarlierOccurrence(8, Instant.parse("2026-10-18T00:30:00Z"), LfLevel.Warning, "other"),
                EarlierOccurrence(7, Instant.parse("2026-10-18T00:10:00Z"), LfLevel.Error, null),
            ),
            d.earlier,
        )
    }

    @Test
    fun oldServer_onlyRecentIds_fallsBackToBareIds() {
        val d = dto(recentIds = listOf(8, 7)).toLogEventDetail()
        assertEquals(listOf(8, 7), d.earlier.map { it.id })
        assertNull(d.earlier[0].at)
        assertNull(d.earlier[0].level)
    }

    @Test
    fun noRecent_isEmpty() {
        assertEquals(emptyList<EarlierOccurrence>(), dto().toLogEventDetail().earlier)
    }
}
