package com.logflare.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
                onSecondary = colors.neutral.white,
                secondaryContainer = colors.secondary.disabled,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                error = colors.red.default,
                onError = colors.neutral.white,
                errorContainer = colors.red.disabled,
                outline = colors.outline,
                inverseOnSurface = colors.neutral.white,
                inverseSurface = colors.neutral.s90,
                surfaceVariant = colors.surfaceVariant,
                tertiary = colors.primary.pressed
            )
        } else {
            lightColorScheme(
                primary = colors.primary.default,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primary.disabled,
                secondary = colors.secondary.default,
                onSecondary = colors.neutral.white,
                secondaryContainer = colors.secondary.disabled,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                error = colors.red.default,
                onError = colors.neutral.white,
                errorContainer = colors.red.disabled,
                outline = colors.outline,
                inverseOnSurface = colors.neutral.white,
                inverseSurface = colors.neutral.s90,
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

        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
            content = content
        )
    }
}
