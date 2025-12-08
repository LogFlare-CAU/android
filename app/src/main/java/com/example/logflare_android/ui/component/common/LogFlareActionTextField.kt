package com.example.logflare_android.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

enum class LogFlareActionTextFieldState {
    Default,
    Validating,
    Success,
    Error,
    Saved
}

enum class LogFlareActionTextFieldHelperTone {
    Info,
    Error
}

/**
 * Validation status for member fields (username, password, etc.)
 */
enum class MemberFieldStatus {
    Idle,
    Validating,
    Valid,
    Error,
    Completed
}

/**
 * Extension to convert MemberFieldStatus to LogFlareActionTextFieldState
 */
fun MemberFieldStatus.toActionTextFieldState(): LogFlareActionTextFieldState = when (this) {
    MemberFieldStatus.Valid -> LogFlareActionTextFieldState.Success
    MemberFieldStatus.Validating -> LogFlareActionTextFieldState.Validating
    MemberFieldStatus.Error -> LogFlareActionTextFieldState.Error
    MemberFieldStatus.Completed -> LogFlareActionTextFieldState.Saved
    MemberFieldStatus.Idle -> LogFlareActionTextFieldState.Default
}

/**
 * Text input with inline action button (like ProjectCreateScreen's name input)
 * 
 * @param label Optional header text above the input field
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param placeholder Placeholder text
 * @param state Current validation/action state
 * @param helperText Optional helper/error text below the input
 * @param helperTone Style tone for helper text (Info/Error)
 * @param actionEnabled Whether the action button is clickable
 * @param onActionClick Callback when action button is clicked
 */
@Composable
fun LogFlareActionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    state: LogFlareActionTextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    helperText: String? = null,
    helperTone: LogFlareActionTextFieldHelperTone = LogFlareActionTextFieldHelperTone.Info,
    actionEnabled: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onActionClick: () -> Unit
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when (state) {
        LogFlareActionTextFieldState.Error -> colors.red.default
        LogFlareActionTextFieldState.Success, LogFlareActionTextFieldState.Validating -> colors.primary.default
        LogFlareActionTextFieldState.Saved -> colors.neutral.s50
        else -> if (isFocused) colors.primary.default else colors.neutral.s40
    }

    val inputBackground = when (state) {
        LogFlareActionTextFieldState.Saved -> colors.neutral.s10
        else -> colors.neutral.white
    }

    val isButtonEnabled = actionEnabled && state != LogFlareActionTextFieldState.Saved && state != LogFlareActionTextFieldState.Validating

    val buttonBackground = when {
        state == LogFlareActionTextFieldState.Validating -> colors.primary.default
        isButtonEnabled -> colors.primary.default
        else -> colors.neutral.s30
    }

    val buttonContentColor = when {
        state == LogFlareActionTextFieldState.Validating || isButtonEnabled -> colors.neutral.white
        else -> colors.neutral.s70
    }

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = AppTheme.typography.bodySmBold,
                color = colors.neutral.black
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = AppTheme.radius.medium,
                color = inputBackground,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.s3),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTheme.typography.bodyMdMedium,
                            color = colors.neutral.s60
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = state != LogFlareActionTextFieldState.Saved,
                        textStyle = AppTheme.typography.bodyMdMedium.copy(color = colors.neutral.s90),
                        cursorBrush = SolidColor(colors.primary.default),
                        keyboardOptions = keyboardOptions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Surface(
                onClick = { if (isButtonEnabled) onActionClick() },
                enabled = isButtonEnabled,
                shape = AppTheme.radius.medium,
                color = buttonBackground,
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state == LogFlareActionTextFieldState.Validating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = colors.neutral.white,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Check",
                            style = AppTheme.typography.bodySmMedium,
                            color = buttonContentColor
                        )
                    }
                }
            }
        }

        helperText?.takeIf { it.isNotBlank() }?.let { text ->
            Spacer(modifier = Modifier.height(AppTheme.spacing.s1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s1)
            ) {
                if (helperTone == LogFlareActionTextFieldHelperTone.Error) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = colors.red.default,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = text,
                    style = AppTheme.typography.captionSmMedium,
                    color = if (helperTone == LogFlareActionTextFieldHelperTone.Error) {
                        colors.red.default
                    } else {
                        colors.neutral.s70
                    }
                )
            }
        }
    }
}