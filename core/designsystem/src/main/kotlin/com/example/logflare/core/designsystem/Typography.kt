package com.example.logflare.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.R

// -------------------------------------------------------------------
// 1. Font Family Setup (Pretendard)
// -------------------------------------------------------------------
// NOTE:
// Pretendard variable font assets live in core/designsystem/src/main/res/font.
// We expose common weights (Light/Medium/Bold) by referencing the same
// variable font resource so text styles can lean on custom typography.

val Pretendard = FontFamily(
    Font(resId = R.font.pretendardvariable, weight = FontWeight.Light),
    Font(resId = R.font.pretendardvariable, weight = FontWeight.Medium),
    Font(resId = R.font.pretendardvariable, weight = FontWeight.Bold)
)

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
