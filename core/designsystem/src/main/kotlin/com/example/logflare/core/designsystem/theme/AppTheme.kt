package com.example.logflare.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.logflare.core.designsystem.AppColors
import com.example.logflare.core.designsystem.AppDimens
import com.example.logflare.core.designsystem.AppRadius
import com.example.logflare.core.designsystem.AppSpacing
import com.example.logflare.core.designsystem.AppTheme as CoreAppTheme
import com.example.logflare.core.designsystem.AppTypography

/**
 * Thin wrapper that exposes the canonical [CoreAppTheme] object under the
 * `com.example.logflare.core.designsystem.theme` package. This allows feature
 * modules to import from a stable `theme` namespace while the underlying
 * implementation remains in `core.designsystem`.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = CoreAppTheme.colors

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = CoreAppTheme.typography

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = CoreAppTheme.spacing

    val radius: AppRadius
        @Composable
        @ReadOnlyComposable
        get() = CoreAppTheme.radius

    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = CoreAppTheme.dimens
}
