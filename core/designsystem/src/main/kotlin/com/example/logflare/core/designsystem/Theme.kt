package com.example.logflare.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// -------------------------------------------------------------------
// Theme Object & Provider
// -------------------------------------------------------------------

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val radius: AppRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalAppRadius.current

    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
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
        primarySubtle = GreenSubtle,
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

    val appTypography = AppTypography(
        // Body Large
        bodyLgBold = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 28.sp
        ),

        // Body Medium
        bodyMdBold = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMdMedium = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMdLight = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Light,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),

        // Body Small
        bodySmBold = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmMedium = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmLight = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Light,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),

        // Caption Medium
        captionMdMedium = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),

        // Caption Small
        captionSmMedium = TextStyle(
            fontFamily = Pretendard,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 16.sp
        )
    )

    // New systems instances
    val appSpacing = AppSpacing()
    val appRadius = AppRadius()
    val appDimens = AppDimens()

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
        LocalAppSpacing provides appSpacing,
        LocalAppRadius provides appRadius,
        LocalAppDimens provides appDimens,
        content = content
    )
}
