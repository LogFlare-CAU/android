package com.example.logflare.core.designsystem

import androidx.compose.runtime.Immutable
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
val GreenSubtle = Color(0xFFB8EEC7)

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
    val primarySubtle: Color,
    val secondary: BrandColorState,
    val red: BrandColorState,
    val neutral: NeutralColorStep
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        primary = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        primarySubtle = Color.Unspecified,
        secondary = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        red = BrandColorState(Color.Unspecified, Color.Unspecified, Color.Unspecified),
        neutral = NeutralColorStep(
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified,
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified,
            Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
        )
    )
}
