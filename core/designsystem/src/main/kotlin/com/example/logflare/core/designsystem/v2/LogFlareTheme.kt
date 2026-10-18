package com.example.logflare.core.designsystem.v2

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalLfColors = staticCompositionLocalOf { lfLightColors() }
private val LocalLfTypography = staticCompositionLocalOf { lfTypography() }
private val LocalLfShapes = staticCompositionLocalOf { LfShapes() }

object Lf {
    val colors: LfColors
        @Composable @ReadOnlyComposable get() = LocalLfColors.current
    val type: LfTypography
        @Composable @ReadOnlyComposable get() = LocalLfTypography.current
    val shapes: LfShapes
        @Composable @ReadOnlyComposable get() = LocalLfShapes.current
}

@Composable
fun LogFlareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = remember(darkTheme) { if (darkTheme) lfDarkColors() else lfLightColors() }
    val typography = remember { lfTypography() }
    val shapes = remember { LfShapes() }
    val scheme = remember(darkTheme) {
        if (darkTheme) {
            darkColorScheme(
                background = colors.background,
                surface = colors.surface,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                onSurfaceVariant = colors.textMuted,
                outline = colors.divider,
                error = colors.error,
                onError = Color.White,
                primary = colors.primaryAction,
                onPrimary = colors.onPrimaryAction,
            )
        } else {
            lightColorScheme(
                background = colors.background,
                surface = colors.surface,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                onSurfaceVariant = colors.textMuted,
                outline = colors.divider,
                error = colors.error,
                onError = Color.White,
                primary = colors.primaryAction,
                onPrimary = colors.onPrimaryAction,
            )
        }
    }
    CompositionLocalProvider(
        LocalLfColors provides colors,
        LocalLfTypography provides typography,
        LocalLfShapes provides shapes,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
