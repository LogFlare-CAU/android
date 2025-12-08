package com.example.logflare.core.designsystem.components.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.logflare.core.designsystem.AppTheme

@Composable
fun RowScope.LogFlareGnbItem(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    label: String,
    enabled: Boolean = true
) {
    val selectedColor = AppTheme.colors.primary.default
    val unselectedColor = AppTheme.colors.secondary.default
    val disabledColor = AppTheme.colors.neutral.s40

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        icon = {
            androidx.compose.material3.Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color.Unspecified
            )
        },
        label = {
            Text(
                text = label,
                style = AppTheme.typography.captionSmMedium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor,
            disabledIconColor = disabledColor,
            disabledTextColor = disabledColor
        )
    )
}
