package com.example.logflare.core.designsystem.v2

enum class LfLevel {
    Debug, Info, Warning, Error, Critical;

    companion object {
        /** Case-insensitive; "WARN" maps to [Warning]; null/unknown maps to [Debug]. */
        fun parse(raw: String?): LfLevel = when (raw?.trim()?.lowercase()) {
            "info" -> Info
            "warn", "warning" -> Warning
            "error" -> Error
            "critical", "fatal" -> Critical
            else -> Debug
        }
    }
}
