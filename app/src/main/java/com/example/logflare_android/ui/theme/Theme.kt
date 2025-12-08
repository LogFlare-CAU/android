package com.example.logflare_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
    content: @Composable () -> Unit
) {
    // Map design system tokens to Material3 ColorScheme
    val colorScheme = lightColorScheme(
        primary = AppTheme.colors.primary.default,
        onPrimary = AppTheme.colors.neutral.white,
        primaryContainer = AppTheme.colors.primary.disabled,
        secondary = AppTheme.colors.secondary.default,
        onSecondary = AppTheme.colors.neutral.white,
        secondaryContainer = AppTheme.colors.secondary.disabled,
        background = AppTheme.colors.neutral.s5,
        onBackground = AppTheme.colors.neutral.s90,
        surface = AppTheme.colors.neutral.white,
        onSurface = AppTheme.colors.neutral.s90,
        error = AppTheme.colors.red.default,
        onError = AppTheme.colors.neutral.white,
        errorContainer = AppTheme.colors.red.disabled,
        outline = AppTheme.colors.neutral.s40,
        inverseOnSurface = AppTheme.colors.neutral.white,
        inverseSurface = AppTheme.colors.neutral.s90,
        surfaceVariant = AppTheme.colors.neutral.s10,
        tertiary = AppTheme.colors.primary.pressed
    )

    // Map design system typography to Material Typography
    val materialTypography = Typography(
        bodyLarge = AppTheme.typography.bodyLgBold,
        bodyMedium = AppTheme.typography.bodyMdMedium,
        bodySmall = AppTheme.typography.bodySmMedium,
        titleMedium = AppTheme.typography.bodyLgBold,
        labelSmall = AppTheme.typography.captionSmMedium,
        labelMedium = AppTheme.typography.captionMdMedium
    )

    AppTheme {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
            content = content
        )
    }
}