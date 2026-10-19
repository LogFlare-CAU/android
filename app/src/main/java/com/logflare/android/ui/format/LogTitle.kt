package com.logflare.android.ui.format

/** Longest title [logTitle] returns, ellipsis included. */
const val LOG_TITLE_MAX = 200

private const val LEVELS = "DEBUG|INFO|NOTICE|WARN|WARNING|ERROR|CRITICAL|FATAL|TRACE"
private const val TRACEBACK = "Traceback (most recent call last)"

private val TIMESTAMP = Regex(
    """^\[?\d{4}[-/]\d{2}[-/]\d{2}(?:[T ]\d{2}:\d{2}:\d{2}(?:[.,]\d+)?(?:Z|[+-]\d{2}:?\d{2})?)?\]?\s*(?:[-|]\s+)?""",
)

/** Python logging: "logger.name - LEVEL - message". */
private val LOGGER_LEVEL = Regex("""^[A-Za-z_][\w.]*\s+-\s+(?:$LEVELS)\s+-\s+""")
private val BRACKET_LEVEL = Regex("""^\[(?:$LEVELS)]\s*(?:[:-]\s*)?""", RegexOption.IGNORE_CASE)
private val BARE_LEVEL = Regex("""^(?:$LEVELS)(?::\s*|\s+[-|:]\s+|\s+)""")

/** Go / glog style "path/file.go:123 " (or ":12:5: ") that follows a level. */
private val FILE_LINE = Regex("""^[\w./\-]+\.\w+:\d+\S*\s+""")

/**
 * A one-line title for a log [message]: the last exception line of a Python traceback, else the
 * first non-empty line without its timestamp, logger name, level and file:line prefix. Never empty
 * unless [message] is blank, and at most [LOG_TITLE_MAX] characters. The full message stays
 * available to callers for detail and search.
 */
fun logTitle(message: String): String {
    val lines = message.lineSequence().map { it.trimEnd() }.toList()
    val firstLine = lines.firstOrNull { it.isNotBlank() }?.trim() ?: return ""
    val traceback = lines.indexOfLast { it.trimStart().startsWith(TRACEBACK) }
    if (traceback >= 0) {
        val exception = lines.drop(traceback + 1).lastOrNull { it.isNotBlank() && !it[0].isWhitespace() }
        if (exception != null) return cap(exception.trim())
    }
    return cap(stripPrefixes(firstLine.take(LOG_TITLE_MAX * 4)).ifBlank { firstLine })
}

private fun stripPrefixes(line: String): String {
    var s = line.replace(TIMESTAMP, "")
    val loggerLevel = LOGGER_LEVEL.find(s)
    if (loggerLevel != null) return s.removeRange(loggerLevel.range).trim()
    val level = BRACKET_LEVEL.find(s) ?: BARE_LEVEL.find(s) ?: return s.trim()
    s = s.removeRange(level.range)
    return s.replace(FILE_LINE, "").trim()
}

private fun cap(s: String) = if (s.length <= LOG_TITLE_MAX) s else s.take(LOG_TITLE_MAX - 1) + "…"
