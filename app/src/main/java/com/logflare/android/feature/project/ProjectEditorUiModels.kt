package com.logflare.android.feature.project

import androidx.compose.ui.graphics.Color

data class ProjectCreateUiState(
    val id: Int = 0,
    val name: String = "",
    val nameValid: Boolean = false,
    val loading: Boolean = false,
    val token: String? = null,
    val error: String? = null,
    val keywords: List<String> = emptyList(),
    val keywordInput: String = "",
    val keywordError: String? = null,
    val alertLevels: Set<String> = emptySet(),
    val saved: Boolean = false,
    val snackbar: String? = null,
    val permissions: List<PermissionToggleState> = emptyList(),
)

data class PermissionToggleState(
    val username: String,
    val role: String,
    val rolenum: Int = 0,
    val roleColor: Color,
    val activeColor: Color,
    val inactiveColor: Color,
    val active: Boolean,
)
