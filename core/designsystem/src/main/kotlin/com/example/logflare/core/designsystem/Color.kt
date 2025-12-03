package com.example.logflare.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 1. Primitive Colors (Raw Hex Values)

val Black = Color(0xFF1A1A1A)
val White = Color(0xFFFFFFFF)

val Neutral5 = Color(0xFFFAFAFA)
val Neutral10 = Color(0xFFF5F5F5)
val Neutral20 = Color(0xFFEEEEEE)
val Neutral30 = Color(0xFFE0E0E0)
val Neutral40 = Color(0xFFBDBDBD)
val Neutral50 = Color(0xFF9E9E9E)
val Neutral60 = Color(0xFF757575)
val Neutral70 = Color(0xFF616161)
val Neutral80 = Color(0xFF424242)
val Neutral90 = Color(0xFF212121)

val GreenDefault = Color(0xFF60B176)
val GreenPressed = Color(0xFF30A14F)
val GreenDisabled = Color(0xFF9ED4AD)

val GreyDefault = Color(0xFF9E9E9E)
val GreyPressed = Color(0xFF4C4C4C)
val GreyDisabled = Color(0xFFC1C1C1)

val RedDefault = Color(0xFFE63946)
val RedPressed = Color(0xFFB02C38)
val RedDisabled = Color(0xFFF3B3B8)

// 2. Semantic Structure (Naming Convention Implementation)

@Immutable
data class BrandColorState(
    val default: Color,
    val pressed: Color,
    val disabled: Color
)

@Immutable
data class NeutralColorStep(
    val s5: Color,
    val s10: Color,
    val s20: Color,
    val s30: Color,
    val s40: Color,
    val s50: Color,
    val s60: Color,
    val s70: Color,
    val s80: Color,
    val s90: Color,
    val black: Color,
    val white: Color
)

@Immutable
data class AppColors(
    val primary: BrandColorState,
    val secondary: BrandColorState,
    val red: BrandColorState,
    val neutral: NeutralColorStep
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        primary = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        secondary = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        red = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        neutral = NeutralColorStep(
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified,
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified,
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
        )
    )
}

// 3. Theme Object & Provider
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val appColors = AppColors(
        primary = BrandColorState(
            default = GreenDefault,
            pressed = GreenPressed,
            disabled = GreenDisabled
        ),
        secondary = BrandColorState(
            default = GreyDefault,
            pressed = GreyPressed,
            disabled = GreyDisabled
        ),
        red = BrandColorState(
            default = RedDefault,
            pressed = RedPressed,
            disabled = RedDisabled
        ),
        neutral = NeutralColorStep(
            s5 = Neutral5,
            s10 = Neutral10,
            s20 = Neutral20,
            s30 = Neutral30,
            s40 = Neutral40,
            s50 = Neutral50,
            s60 = Neutral60,
            s70 = Neutral70,
            s80 = Neutral80,
            s90 = Neutral90,
            black = Black,
            white = White
        )
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        content = content
    )
}
