package com.example.logflare.core.designsystem.components.input

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
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

@Composable
fun LogFlareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        !enabled -> AppTheme.colors.neutral.s70
        isError -> AppTheme.colors.red.default
        isFocused -> AppTheme.colors.primary.default
        else -> AppTheme.colors.neutral.s70
    }
    val backgroundColor = if (enabled) AppTheme.colors.neutral.white else AppTheme.colors.neutral.s10
    val textColor = if (enabled) AppTheme.colors.neutral.s90 else AppTheme.colors.neutral.s60
    val placeholderColor = AppTheme.colors.neutral.s60
    val helperColor = if (isError) AppTheme.colors.red.default else AppTheme.colors.neutral.s70

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = AppTheme.typography.bodyMdMedium,
                color = AppTheme.colors.neutral.s70
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = AppTheme.typography.bodyMdMedium.copy(color = textColor),
            interactionSource = interactionSource,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(AppTheme.colors.primary.default),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Surface(
                    shape = AppTheme.radius.medium,
                    border = BorderStroke(1.dp, borderColor),
                    color = backgroundColor,
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTheme.spacing.s3),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = AppTheme.typography.bodyMdMedium,
                                color = placeholderColor
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )

        helperText?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(AppTheme.spacing.s1))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s1)
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = AppTheme.colors.red.default,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = it,
                    style = AppTheme.typography.captionSmMedium,
                    color = helperColor
                )
            }
        }
    }
}
