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
 * Splits [items] into runs of rows from the same local day (in [zone]) of their `lastSeen`,
 * keeping the list's order: a row joins the group before it only when it is from that group's
 * day. Newest-first lists start with Today, an Oldest sort keeps ascending days, and a level sort
 * keeps its order (a day can then appear more than once); rows of a later page never move up
 * into a group above them.
 */
fun groupByDay(items: List<LogEvent>, today: LocalDate, zone: ZoneId): List<DayGroup> {
    val groups = mutableListOf<Pair<LocalDate, MutableList<LogEvent>>>()
    for (item in items) {
        val day = item.lastSeen.atZone(zone).toLocalDate()
        val last = groups.lastOrNull()
        if (last != null && last.first == day) last.second += item else groups += day to mutableListOf(item)
    }
    return groups.map { (day, rows) -> DayGroup(labelOf(day, today), rows) }
}

private fun labelOf(day: LocalDate, today: LocalDate): DayLabel = when (day) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.Date(day, showYear = day.year != today.year)
}
