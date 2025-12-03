package com.example.logflare_android.enums

import androidx.compose.ui.graphics.Color

enum class UserPermission(
    val code: Int,
    val label: String,
    val color: Color
) {
    USER(0, "Member", Color(0xFF616161)),
    MODERATOR(80, "Admin", Color(0xFF60B176)),
    SUPER_USER(100, "Super Admin", Color(0xFF30A14F));

    companion object {
        fun fromCode(code: Int): UserPermission =
            UserPermission.entries.find { it.code == code } ?: USER
    }
}
