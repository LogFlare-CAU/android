package com.example.logflare_android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.logflare.core.designsystem.AppTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun LogflareandroidTheme(
    content: @Composable () -> Unit
) {
    // Build a Material ColorScheme from the design system tokens.
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
        tertiary = AppTheme.colors.primary.pressed // fallback
    )

    // Map our AppTypography into Material Typography so existing code using
    // MaterialTheme.typography continues to work during incremental migration.
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


//@Composable
//fun LogflareandroidTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}