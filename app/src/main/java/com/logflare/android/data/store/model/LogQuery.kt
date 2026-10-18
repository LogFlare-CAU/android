package com.logflare.android.data.store.model

import com.example.logflare.core.designsystem.v2.LfLevel

enum class LogSortOrder(val api: String) { Newest("newest"), Oldest("oldest"), Highest("highest"), Lowest("lowest") }

/** Filters of the grouped event list (`GET /log/events`). */
data class LogQuery(
    val projectId: Int? = null,
    val minLevel: LfLevel? = null,
    val logFileId: Int? = null,
    val text: String? = null,
    val sort: LogSortOrder = LogSortOrder.Newest,
) {
    /** Search text as sent to the server: blank means no search. */
    val apiText: String? get() = text?.trim()?.takeIf { it.isNotEmpty() }

    /** `min_level` as the server expects it (uppercase). */
    val apiMinLevel: String? get() = minLevel?.name?.uppercase()

    /**
     * Stable identity of this query for cache keys: equal for queries the server answers the
     * same way, and different whenever any field differs. The free text goes last and is
     * length-prefixed, so it can never be mistaken for another field.
     */
    fun hashKey(): String {
        val q = apiText?.let { "${it.length}:$it" } ?: "-"
        return "p=${projectId ?: "-"}|l=${apiMinLevel ?: "-"}|f=${logFileId ?: "-"}|s=${sort.api}|q=$q"
    }
}
