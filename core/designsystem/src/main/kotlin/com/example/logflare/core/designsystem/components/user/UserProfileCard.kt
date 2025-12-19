package com.example.logflare.core.designsystem.components.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.chip.ChipSize
import com.example.logflare.core.designsystem.components.chip.RoleChip
import com.example.logflare.core.designsystem.components.userlist.RoleBadgeType
import com.example.logflare.core.designsystem.components.userlist.roleChipStyle

@Composable
fun UserProfileCard(
    username: String,
    roleLabel: String,
    roleType: RoleBadgeType = RoleBadgeType.Member,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    val roleChipStyle = roleChipStyle(roleType)

    Surface(
        color = AppTheme.colors.neutral.s20,
        shape = AppTheme.radius.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = username,
                    style = AppTheme.typography.bodyLgBold,
                    color = AppTheme.colors.neutral.black
                )
            }

            RoleChip(
                text = roleLabel,
                size = ChipSize.Large,
                backgroundColor = roleChipStyle.backgroundColor,
                contentColor = roleChipStyle.contentColor
            )

            trailingContent?.invoke()
        }
    }
}
