package com.example.logflare_android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare_android.ui.navigation.Route

/**
 * Metadata describing a bottom navigation destination.
 */
data class GnbItem(
    val route: Route,
    @DrawableRes val iconRes: Int,
    val label: String
)

/**
 * LogFlare-styled bottom navigation item.
 */
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
