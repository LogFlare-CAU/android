package com.example.logflare.core.designsystem.v2

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LfColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val icon: Color,
    val divider: Color,
    val fill: Color,
    val error: Color,
    val errorContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val critical: Color,
    val onCritical: Color,
    val primaryAction: Color,
    val onPrimaryAction: Color,
    val secondaryAction: Color,
    val disabled: Color,
    val onDisabled: Color,
    val scrim: Color,
)

fun lfLightColors() = LfColors(
    background = Color(0xFFF2F4F6),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF191F28),
    textSecondary = Color(0xFF4E5968),
    textMuted = Color(0xFF657080),
    icon = Color(0xFF8B95A1),
    divider = Color(0xFFE5E8EB),
    fill = Color(0xFFE5E8EB),
    error = Color(0xFFD22030),
    errorContainer = Color(0xFFFFEBEE),
    /** Darkened from the prototype's #B35900 to meet 4.5:1 on warningContainer. */
    warning = Color(0xFFA65100),
    warningContainer = Color(0xFFFFF3E0),
    critical = Color(0xFF2B0A0E),
    onCritical = Color(0xFFFFFFFF),
    primaryAction = Color(0xFF191F28),
    onPrimaryAction = Color(0xFFFFFFFF),
    secondaryAction = Color(0xFFE5E8EB),
    disabled = Color(0xFFD1D6DB),
    onDisabled = Color(0xFF6B7684),
    scrim = Color(0x66000000),
)

/**
 * Dark: cards lift off a near-black page, greys are brighter for readability, and Critical is a
 * solid saturated red tile (white glyph/text, >= 4.5:1) so it is louder than Error's dark tile.
 */
fun lfDarkColors() = LfColors(
    background = Color(0xFF0A0B0E),
    surface = Color(0xFF1E2228),
    textPrimary = Color(0xFFF2F4F6),
    textSecondary = Color(0xFFC2C9D1),
    textMuted = Color(0xFFA7B0BB),
    icon = Color(0xFF9AA4B0),
    divider = Color(0xFF363C44),
    fill = Color(0xFF2E333B),
    error = Color(0xFFFF8590),
    errorContainer = Color(0xFF4D1B22),
    warning = Color(0xFFFFB454),
    warningContainer = Color(0xFF3A2610),
    critical = Color(0xFFD8363D),
    onCritical = Color(0xFFFFFFFF),
    primaryAction = Color(0xFFF2F4F6),
    onPrimaryAction = Color(0xFF191F28),
    secondaryAction = Color(0xFF2E333B),
    disabled = Color(0xFF2E333B),
    onDisabled = Color(0xFF9AA4B0),
    scrim = Color(0x99000000),
)
