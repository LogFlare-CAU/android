package com.logflare.qa.script

object UiModeNightParser {
    private val nightYes = Regex("""(?i)Night mode:\s*yes\b""")
    private val nightNo = Regex("""(?i)Night mode:\s*no\b""")

    /** Returns true for night/dark, false for not-night/light, null if unparsable. */
    fun isNightEnabled(raw: String): Boolean? {
        val text = raw.trim()
        if (nightYes.containsMatchIn(text)) return true
        if (nightNo.containsMatchIn(text)) return false
        return when (text.lowercase()) {
            "yes" -> true
            "no" -> false
            else -> null
        }
    }

    fun matchesExpected(theme: String, raw: String): Boolean {
        val night = isNightEnabled(raw) ?: return false
        return when (theme.lowercase()) {
            "dark" -> night
            "light" -> !night
            else -> false
        }
    }
}
