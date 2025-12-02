package com.example.logflare_android.enum

import androidx.compose.ui.graphics.Color

enum class UserPermission(
    val code: Int,
    val label: String,
    val color: Color
) {
    USER(0, "User", Color(0xFF616161)),
    MODERATOR(80, "Moderator", Color(0xFF1976D2)),
    SUPER_USER(100, "Super Admin", Color(0xFF9C27B0)); // 100번

    companion object {
        fun fromCode(code: Int): UserPermission =
            UserPermission.entries.find { it.code == code } ?: USER
    }
}