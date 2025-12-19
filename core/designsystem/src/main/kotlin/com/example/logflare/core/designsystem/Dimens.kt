package com.example.logflare.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// -------------------------------------------------------------------
// Spacing, Radius (Shapes), Grid & Dimens System
// -------------------------------------------------------------------

@Immutable
data class AppSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s6: Dp = 24.dp,
    val s8: Dp = 32.dp
)

@Immutable
data class AppRadius(
    val small: RoundedCornerShape = RoundedCornerShape(2.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(8.dp),
    val large: RoundedCornerShape = RoundedCornerShape(12.dp),
    val xlarge: RoundedCornerShape = RoundedCornerShape(16.dp),
    val full: RoundedCornerShape = RoundedCornerShape(9999.dp)
)

@Immutable
data class AppDimens(
    val mobileMargin: Dp = 16.dp,
    val mobileGutter: Dp = 16.dp,
    val gridColumns: Int = 4
)

// CompositionLocals for new systems
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalAppRadius = staticCompositionLocalOf { AppRadius() }
val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
