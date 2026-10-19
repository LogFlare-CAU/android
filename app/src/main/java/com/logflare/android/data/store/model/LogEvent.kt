package com.logflare.android.data.store.model

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogEventDTO
import com.example.logflare.core.model.LogEventDetailDTO
import com.example.logflare.core.model.LogEventPageDTO
import java.time.Instant

/**
 * One group of the event list: rows with the same project, type and message. [id] is the
 * latest row, [level] the highest level in the group and [occurrences] the matching rows.
 */
data class LogEvent(
    val id: Int,
    val projectId: Int,
    val logFileName: String?,
    val type: String?,
    val message: String,
    val level: LfLevel,
    val lastSeen: Instant,
    val firstSeen: Instant,
    val occurrences: Int,
)

/**
 * One row ([occurredAt], [rowLevel], [raw]) with its group in [event]. Here
 * [LogEvent.occurrences] counts the whole group, all time. [recentIds] are up to 10 other rows
 * of the same group, [earlier] the same rows with their times when the server provides them.
 */
data class LogEventDetail(
    val event: LogEvent,
    val raw: String?,
    val recentIds: List<Int>,
    val occurredAt: Instant,
    val rowLevel: LfLevel,
    val earlier: List<EarlierOccurrence> = emptyList(),
)

/** One of a group's other rows; [at], [level] and [message] are null when the server only sent ids. */
data class EarlierOccurrence(val id: Int, val at: Instant?, val level: LfLevel?, val message: String?)

data class LogPage(val items: List<LogEvent>, val hasMore: Boolean)

internal fun LogEventDTO.toLogEvent() = LogEvent(
    id = id,
    projectId = projectId,
    logFileName = logfileName,
    type = errortype,
    message = message,
    level = LfLevel.parse(level),
    lastSeen = parseServerTime(lastSeen),
    firstSeen = parseServerTime(firstSeen),
    occurrences = occurrences,
)

internal fun LogEventPageDTO.toLogPage() = LogPage(items.map { it.toLogEvent() }, hasMore = nextCursor != null)

internal fun LogEventDetailDTO.toLogEventDetail() = LogEventDetail(
    event = LogEvent(
        id = id,
        projectId = projectId,
        logFileName = logfileName,
        type = errortype,
        message = message,
        level = LfLevel.parse(level),
        lastSeen = parseServerTime(lastSeen),
        firstSeen = parseServerTime(firstSeen),
        occurrences = occurrences,
    ),
    raw = raw,
    recentIds = recentIds,
    occurredAt = parseServerTime(occurredAt),
    rowLevel = LfLevel.parse(rowLevel),
    earlier = if (recent.isNotEmpty()) {
        recent.map { EarlierOccurrence(it.id, parseServerTime(it.timestamp), LfLevel.parse(it.level), it.message) }
    } else {
        recentIds.map { EarlierOccurrence(it, null, null, null) }
    },
)
