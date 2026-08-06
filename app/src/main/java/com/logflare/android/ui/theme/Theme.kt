package com.logflare.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.logflare.core.designsystem.AppTheme

/**
 * App-level theme wrapper.
 *
 * This wraps core/designsystem's AppTheme and maps its tokens to Material3's
 * ColorScheme and Typography so existing components using MaterialTheme continue to work.
 *
 * For new components, prefer using AppTheme directly:
 *   - AppTheme.colors.primary.default
 *   - AppTheme.typography.bodyMdBold
 *   - AppTheme.spacing.s4
 */
@Composable
fun LogflareandroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(darkTheme = darkTheme) {
        val colors = AppTheme.colors
        val colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = colors.primary.default,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primary.disabled,
                secondary = colors.secondary.default,
                onSecondary = colors.onPrimary,
                secondaryContainer = colors.secondary.disabled,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.muted,
                error = colors.red.default,
                onError = colors.onPrimary,
                errorContainer = colors.red.disabled,
                outline = colors.outline,
                outlineVariant = colors.divider,
                inverseOnSurface = colors.background,
                inverseSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant,
                tertiary = colors.primary.pressed
            )
        } else {
            lightColorScheme(
                primary = colors.primary.default,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primary.disabled,
                secondary = colors.secondary.default,
                onSecondary = colors.onPrimary,
                secondaryContainer = colors.secondary.disabled,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                onSurfaceVariant = colors.muted,
                error = colors.red.default,
                onError = colors.onPrimary,
                errorContainer = colors.red.disabled,
                outline = colors.outline,
                outlineVariant = colors.divider,
                inverseOnSurface = colors.background,
                inverseSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant,
                tertiary = colors.primary.pressed
            )
        }

        val materialTypography = Typography(
            bodyLarge = AppTheme.typography.bodyLgBold,
            bodyMedium = AppTheme.typography.bodyMdMedium,
            bodySmall = AppTheme.typography.bodySmMedium,
            titleMedium = AppTheme.typography.bodyLgBold,
            labelSmall = AppTheme.typography.captionSmMedium,
            labelMedium = AppTheme.typography.captionMdMedium
        )

        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insets = WindowCompat.getInsetsController(window, view)
                insets.isAppearanceLightStatusBars = !darkTheme
                insets.isAppearanceLightNavigationBars = !darkTheme
            }
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background,
                contentColor = colors.onBackground,
            ) {
                content()
            }
        }
    }
}
