package com.example.logflare.core.designsystem.v2

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.example.logflare.core.designsystem.R

@Immutable
data class LfTypography(
    val largeTitle: TextStyle,
    val barTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val secondary: TextStyle,
    val caption: TextStyle,
    val statNumber: TextStyle,
)

private fun style(size: Int, weight: FontWeight, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = LfPretendard,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp,
)

fun lfTypography() = LfTypography(
    largeTitle = style(28, FontWeight.ExtraBold, -0.6f),
    barTitle = style(17, FontWeight.Bold),
    sectionTitle = style(16, FontWeight.Bold),
    cardTitle = style(18, FontWeight.Bold),
    body = style(15, FontWeight.Normal),
    bodyStrong = style(15, FontWeight.SemiBold),
    secondary = style(13, FontWeight.Normal),
    caption = style(12, FontWeight.Normal),
    statNumber = style(22, FontWeight.ExtraBold),
)

@OptIn(ExperimentalTextApi::class)
val LfPretendard = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.pretendardvariable,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)
