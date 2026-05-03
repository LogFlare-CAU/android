package com.logflare.android.feature.project

import androidx.compose.ui.graphics.Color
import com.example.logflare.core.model.ProjectPermsDTO
import com.example.logflare.core.model.UserDTO
import com.logflare.android.enums.UserPermission

/**
 * Builds [PermissionToggleState] rows for the project settings permissions UI.
 */
object ProjectPermissionUiFactory {

    fun superUser(username: String = "{{username}}", active: Boolean = true) = PermissionToggleState(
        username = username,
        role = UserPermission.SUPER_USER.label,
        rolenum = UserPermission.SUPER_USER.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.SUPER_USER.color,
        inactiveColor = Color(0xFFCCCCCC),
        active = active,
    )

    fun adminUser(username: String = "{{username}}", active: Boolean = true) = PermissionToggleState(
        username = username,
        role = UserPermission.MODERATOR.label,
        rolenum = UserPermission.MODERATOR.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.MODERATOR.color,
        inactiveColor = Color(0xFFCCCCCC),
        active = active,
    )

    fun memberUser(username: String = "{{username}}", active: Boolean = false) = PermissionToggleState(
        username = username,
        role = UserPermission.USER.label,
        rolenum = UserPermission.USER.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.USER.color,
        inactiveColor = Color(0xFFC2C2C2),
        active = active,
    )

    fun hydratePlaceholder(): List<PermissionToggleState> = listOf(
        superUser(),
        adminUser(),
        memberUser(),
    )

    fun mapUsersToPermissionStates(
        users: List<UserDTO>,
        perms: List<ProjectPermsDTO>?,
    ): List<PermissionToggleState> = users.map { user ->
        when {
            user.permission >= UserPermission.SUPER_USER.code -> superUser(
                username = user.username,
                active = true,
            )

            user.permission >= UserPermission.MODERATOR.code -> adminUser(
                username = user.username,
                active = perms?.any { it.user_id == user.idx } ?: true,
            )

            else -> memberUser(
                username = user.username,
                active = perms?.any { it.user_id == user.idx } ?: false,
            )
        }
    }
}
