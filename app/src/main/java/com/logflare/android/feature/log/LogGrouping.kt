package com.logflare.android.feature.log

import com.logflare.android.data.store.model.LogEvent
import java.time.LocalDate
import java.time.ZoneId

/** A day heading of the Logs list; the screen turns it into "Today", "Yesterday" or "Oct 16". */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel

    /** [showYear] when [date] is not in the current year ("Dec 31, 2025"). */
    data class Date(val date: LocalDate, val showYear: Boolean) : DayLabel
}

data class DayGroup(val label: DayLabel, val items: List<LogEvent>)

/**
 * Splits [items] by the local day (in [zone]) of their `lastSeen`. The list's order is kept, within
 * and across groups: days come in the order their first row appears, so newest-first lists start
 * with Today and an Oldest sort keeps ascending days.
 */
fun groupByDay(items: List<LogEvent>, today: LocalDate, zone: ZoneId): List<DayGroup> =
    items
        .groupBy { it.lastSeen.atZone(zone).toLocalDate() } // LinkedHashMap: first-appearance order.
        .map { (day, rows) -> DayGroup(labelOf(day, today), rows) }

private fun labelOf(day: LocalDate, today: LocalDate): DayLabel = when (day) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.Date(day, showYear = day.year != today.year)
}
