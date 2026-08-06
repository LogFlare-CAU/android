package com.logflare.android.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.AppTheme

/**
 * Semantic OutlinedTextField colors so typed text stays readable in light and dark.
 */
@Composable
fun logflareOutlinedTextFieldColors(
    isError: Boolean = false,
): TextFieldColors {
    val colors = AppTheme.colors
    val borderFocused = if (isError) colors.red.default else colors.primary.default
    val borderUnfocused = if (isError) colors.red.default else colors.outline
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.onInput,
        unfocusedTextColor = colors.onInput,
        disabledTextColor = colors.muted,
        errorTextColor = colors.onInput,
        focusedContainerColor = colors.input,
        unfocusedContainerColor = colors.input,
        disabledContainerColor = colors.inputDisabled,
        errorContainerColor = colors.input,
        cursorColor = colors.onInput,
        errorCursorColor = colors.red.default,
        focusedBorderColor = borderFocused,
        unfocusedBorderColor = borderUnfocused,
        disabledBorderColor = colors.outline,
        errorBorderColor = colors.red.default,
        focusedLabelColor = colors.muted,
        unfocusedLabelColor = colors.muted,
        disabledLabelColor = colors.muted,
        errorLabelColor = colors.red.default,
        focusedPlaceholderColor = colors.muted,
        unfocusedPlaceholderColor = colors.muted,
        disabledPlaceholderColor = colors.muted,
        errorPlaceholderColor = colors.muted,
    )
}
