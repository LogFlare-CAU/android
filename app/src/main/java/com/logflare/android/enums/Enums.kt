package com.logflare.android.enums

import androidx.compose.ui.graphics.Color
import com.example.logflare.core.designsystem.AppColors
import com.example.logflare.core.designsystem.logLevelColor

enum class UserPermission(
    val code: Int,
    val label: String,
) {
    USER(0, "Member"),
    MODERATOR(80, "Admin"),
    SUPER_USER(100, "Super Admin");

    companion object {
        fun fromCode(code: Int): UserPermission =
            UserPermission.entries.find { it.code == code } ?: USER
    }
}

fun UserPermission.color(colors: AppColors): Color = when (this) {
    UserPermission.USER -> colors.secondary.pressed
    UserPermission.MODERATOR -> colors.primary.default
    UserPermission.SUPER_USER -> colors.primary.pressed
}

enum class LogLevel(val code: Int, val label: String) {
    DEBUG(10, "Debug"),
    INFO(20, "Info"),
    WARNING(30, "Warning"),
    ERROR(40, "Error"),
    CRITICAL(50, "Critical");

    companion object {
        fun fromCode(code: Int): LogLevel =
            LogLevel.entries.find { it.code == code } ?: DEBUG

        fun fromLabel(label: String): LogLevel =
            LogLevel.entries.find { it.label.equals(label, ignoreCase = true) } ?: DEBUG

        fun getAll(): List<LogLevel> = LogLevel.entries.toList()

        fun getAllLabels(): List<String> = LogLevel.entries.map { it.label }

        fun getAboveLevel(level: String): List<LogLevel> =
            LogLevel.entries.filter { it.code >= fromLabel(level).code }
    }
}

fun LogLevel.color(colors: AppColors): Color = colors.logLevelColor(label)

enum class LogSort(val label: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    LEVEL_DESC("highest"),
    LEVEL_ASC("lowest")
}
