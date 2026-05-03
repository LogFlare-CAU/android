package com.logflare.android.feature.log

import com.example.logflare.core.model.ErrorlogDTO
import com.logflare.android.enums.LogLevel

/**
 * Client-side log level filtering. The `/log/error` API has no level query param; if the backend
 * adds one later, prefer server-side filtering and keep this only as a fallback.
 */
fun List<ErrorlogDTO>.filterByLogLevels(levels: List<LogLevel>): List<ErrorlogDTO> {
    if (levels.isEmpty()) return this
    val allowed = levels.map { it.name.uppercase() }.toSet()
    return filter { row -> allowed.contains(row.level.uppercase()) }
}
