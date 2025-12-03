package com.example.logflare.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// -------------------------------------------------------------------
// 1. Font Family Setup (Pretendard)
// -------------------------------------------------------------------
// NOTE:
// - If you add actual font files under core/designsystem/src/main/res/font
//   as pretendard_bold.otf, pretendard_medium.otf, pretendard_light.otf,
//   you can replace the fallback with resource-based fonts.
// - For now, we fallback to the default sans-serif family to avoid build errors
//   when font resources are not present.

val Pretendard = FontFamily.SansSerif
// Example (uncomment after adding resources to this module):
// val Pretendard = FontFamily(
//     Font(R.font.pretendard_bold, FontWeight.Bold),
//     Font(R.font.pretendard_medium, FontWeight.Medium),
//     Font(R.font.pretendard_light, FontWeight.Light)
// )

// -------------------------------------------------------------------
// 2. Typography Definition (roleSizeWeight -> camelCase)
// -------------------------------------------------------------------

@Immutable
data class AppTypography(
    // Body - Large
    val bodyLgBold: TextStyle,

    // Body - Medium
    val bodyMdBold: TextStyle,
    val bodyMdMedium: TextStyle,
    val bodyMdLight: TextStyle,

    // Body - Small
    val bodySmBold: TextStyle,
    val bodySmMedium: TextStyle,
    val bodySmLight: TextStyle,

    // Caption
    val captionMdMedium: TextStyle,
    val captionSmMedium: TextStyle
)

// CompositionLocal 생성 (기본값은 비어있음)
val LocalAppTypography = staticCompositionLocalOf {
    AppTypography(
        bodyLgBold = TextStyle.Default,
        bodyMdBold = TextStyle.Default,
        bodyMdMedium = TextStyle.Default,
        bodyMdLight = TextStyle.Default,
        bodySmBold = TextStyle.Default,
        bodySmMedium = TextStyle.Default,
        bodySmLight = TextStyle.Default,
        captionMdMedium = TextStyle.Default,
        captionSmMedium = TextStyle.Default
    )
}
