package com.logflare.android.data.store.model

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

/**
 * `date` + `T` or space + `time` (fractional seconds optional) + an optional offset (`Z`,
 * `+09:00`, `+0900`). A missing offset means UTC, which is how the backend stores times.
 */
private val SERVER_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .optionalStart().appendLiteral('T').optionalEnd()
    .optionalStart().appendLiteral(' ').optionalEnd()
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
    .optionalStart().appendOffset("+HHMMss", "Z").optionalEnd()
    .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
    .toFormatter()

/**
 * Parses a server timestamp as an [Instant]; see [SERVER_TIME] for the accepted forms.
 *
 * @throws DateTimeParseException when [s] is not such a date-time.
 */
fun parseServerTime(s: String): Instant = OffsetDateTime.parse(s.trim(), SERVER_TIME).toInstant()
