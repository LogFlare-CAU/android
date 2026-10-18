package com.logflare.android.data.store.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Parses a server timestamp. The backend stores naive UTC and usually sends it without an
 * offset (`2026-10-18T10:00:00` or with fractional seconds); such values are read as UTC.
 * Values with `Z` or an offset are converted to UTC. A space instead of `T` is accepted.
 *
 * @throws DateTimeParseException when [s] is not an ISO-8601 date-time.
 */
fun parseServerTime(s: String): Instant {
    val text = s.trim().replace(' ', 'T')
    return try {
        OffsetDateTime.parse(text).toInstant()
    } catch (_: DateTimeParseException) {
        LocalDateTime.parse(text).toInstant(ZoneOffset.UTC)
    }
}
