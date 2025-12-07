package com.example.logflare_android.enums

import androidx.compose.ui.graphics.Color

enum class UserPermission(
    val code: Int,
    val label: String,
    val color: Color
) {
    USER(0, "User", Color(0xFF28BB00)),
    MODERATOR(80, "Moderator", Color(0xFF1976D2)),
    SUPER_USER(100, "Super Admin", Color(0xFF9C27B0)); // 100번

    companion object {
        fun fromCode(code: Int): UserPermission =
            UserPermission.entries.find { it.code == code } ?: USER
    }
}

enum class LogLevel(val code: Int, val label: String, val color: Color) {
    DEBUG(10, "Debug", Color(0xFF90CAF9)),
    INFO(20, "Info", Color(0xFF4CAF50)),
    WARNING(30, "Warning", Color(0xFFFFC107)),
    ERROR(40, "Error", Color(0xFFF44336)),
    CRITICAL(50, "Critical", Color(0xFFD32F2F));

    companion object {
        fun fromCode(code: Int): LogLevel =
            LogLevel.entries.find { it.code == code } ?: DEBUG

        fun fromCodeByLabel(label: String): LogLevel =
            LogLevel.entries.find { it.label.equals(label, ignoreCase = true) } ?: DEBUG

        fun getAll(): List<LogLevel> = LogLevel.entries.toList()

        fun getAllLabels(): List<String> = LogLevel.entries.map { it.label }

        fun getAboveLevel(level: String): List<LogLevel> =
            LogLevel.entries.filter { it.code >= fromCodeByLabel(level).code }
    }
}

enum class LogSort(val label: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    LEVEL_DESC("highest"),
    LEVEL_ASC("lowest")
}
