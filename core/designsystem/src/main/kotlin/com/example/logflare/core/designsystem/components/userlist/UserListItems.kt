package com.example.logflare.core.designsystem.components.userlist

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.chip.ChipSize
import com.example.logflare.core.designsystem.components.chip.RoleChip

enum class UserItemSize { Large, Small }
enum class RoleBadgeType { SuperUser, Moderator, Member }

@Composable
fun UserListItem(
    username: String,
    roleLabel: String,
    roleType: RoleBadgeType = RoleBadgeType.Member,
    size: UserItemSize = UserItemSize.Small,
    showRadio: Boolean = false,
    isSelected: Boolean = false,
    onSelectChange: ((Boolean) -> Unit)? = null,
    showDeleteIcon: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val metrics = when (size) {
        UserItemSize.Large -> UserListItemMetrics(
            textStyle = AppTheme.typography.bodyLgBold,
            nameChipSpacing = 8.dp,
            verticalPadding = 12.dp,
            chipSize = ChipSize.Large
        )
        UserItemSize.Small -> UserListItemMetrics(
            textStyle = AppTheme.typography.bodySmMedium,
            nameChipSpacing = 4.dp,
            verticalPadding = 8.dp,
            chipSize = ChipSize.Small
        )
    }

    val roleChipStyle = roleChipStyle(roleType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = metrics.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = username,
            style = metrics.textStyle,
            color = AppTheme.colors.neutral.black
        )

        Spacer(modifier = Modifier.width(metrics.nameChipSpacing))
        RoleChip(
            text = roleLabel,
            size = metrics.chipSize,
            backgroundColor = roleChipStyle.backgroundColor,
            contentColor = roleChipStyle.contentColor
        )

        Spacer(modifier = Modifier.weight(1f))

        if (showRadio) {
            Switch(
                checked = isSelected,
                onCheckedChange = { checked -> onSelectChange?.invoke(checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.neutral.white,
                    uncheckedThumbColor = AppTheme.colors.neutral.white,
                    checkedTrackColor = AppTheme.colors.primary.default,
                    uncheckedTrackColor = AppTheme.colors.neutral.s30
                )
            )
        }

        if (showRadio && (showDeleteIcon || trailingContent != null)) {
            Spacer(modifier = Modifier.width(AppTheme.spacing.s2))
        }

        if (showDeleteIcon) {
            IconButton(onClick = { onDeleteClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete user",
                    tint = AppTheme.colors.neutral.s50
                )
            }
        }

        if (showDeleteIcon && trailingContent != null) {
            Spacer(modifier = Modifier.width(AppTheme.spacing.s1))
        }

        trailingContent?.invoke()
    }
}

data class RoleChipStyle(
    val backgroundColor: Color,
    val contentColor: Color = Color.White
)

@Composable
fun roleChipStyle(roleType: RoleBadgeType): RoleChipStyle {
    val colors = AppTheme.colors
    return when (roleType) {
        RoleBadgeType.SuperUser -> RoleChipStyle(
            backgroundColor = colors.primary.pressed,
            contentColor = colors.neutral.white
        )
        RoleBadgeType.Moderator -> RoleChipStyle(
            backgroundColor = colors.primary.default,
            contentColor = colors.neutral.white
        )
        RoleBadgeType.Member -> RoleChipStyle(
            backgroundColor = colors.neutral.s70,
            contentColor = colors.neutral.white
        )
    }
}

private data class UserListItemMetrics(
    val textStyle: TextStyle,
    val nameChipSpacing: Dp,
    val verticalPadding: Dp,
    val chipSize: ChipSize
)
