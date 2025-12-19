package com.example.logflare.core.designsystem.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonSize.Field
import com.example.logflare.core.designsystem.components.button.ButtonSize.Large
import com.example.logflare.core.designsystem.components.button.ButtonSize.Medium
import com.example.logflare.core.designsystem.components.button.ButtonSize.Small
import com.example.logflare.core.designsystem.components.button.ButtonType.Filled
import com.example.logflare.core.designsystem.components.button.ButtonType.Outline
import com.example.logflare.core.designsystem.components.button.ButtonType.Text
import com.example.logflare.core.designsystem.components.button.ButtonVariant.Primary
import com.example.logflare.core.designsystem.components.button.ButtonVariant.Secondary

/**
 * Standard primary CTA button anchored near the bottom.
 * Provides consistent height and padding so text doesn't clip.
 */
@Composable
fun BottomPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        type = Filled,
        size = Large,
        enabled = enabled
    )
}

/**
 * Standard outlined secondary CTA button for cancel or neutral actions.
 */
@Composable
fun BottomOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        type = Outline,
        size = Large,
        enabled = enabled
    )
}

/**
 * Danger-styled outlined button variant used for delete actions.
 */
@Composable
fun BottomDangerOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    SecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        type = Outline,
        size = Large,
        enabled = enabled
    )
}

/**
 * Single master button used across the app.
 *
 * - [variant]: Primary or Secondary (controls color palette)
 * - [type]: Filled / Outline / Text (controls container, border, and emphasis)
 * - [size]: Large / Medium / Small (controls height, padding, and typography)
 */
@Composable
fun LogFlareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = Primary,
    type: ButtonType = Filled,
    size: ButtonSize = Large,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val variantColors = when (variant) {
        Primary -> AppTheme.colors.primary
        Secondary -> AppTheme.colors.secondary
    }

    val colors = when (type) {
        Filled -> ButtonDefaults.buttonColors(
            containerColor = variantColors.default,
            contentColor = AppTheme.colors.neutral.white,
            disabledContainerColor = variantColors.disabled,
            disabledContentColor = AppTheme.colors.neutral.s40,
        )

        Outline -> ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = variantColors.default,
            disabledContentColor = variantColors.disabled,
        )

        Text -> ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = variantColors.default,
            disabledContentColor = variantColors.disabled,
        )
    }

    val border: BorderStroke? = when (type) {
        Filled, Text -> null
        Outline -> BorderStroke(
            width = 1.dp,
            color = if (enabled) variantColors.default else AppTheme.colors.neutral.s40,
        )
    }

    val (height, horizontalPadding, textStyle) = when (size) {
        Large -> Triple(48.dp, 24.dp, AppTheme.typography.bodyMdBold)
        Medium -> Triple(40.dp, 16.dp, AppTheme.typography.bodyMdBold)
        Small -> Triple(30.dp, 12.dp, AppTheme.typography.bodySmBold)
        Field -> Triple(50.dp, 16.dp, AppTheme.typography.bodySmMedium)
    }

    val contentPadding = PaddingValues(
        horizontal = horizontalPadding,
        vertical = 0.dp,
    )

    val shape = AppTheme.radius.medium

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.height(height),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                androidx.compose.material3.Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = AppTheme.spacing.s1),
                )
            }
            Text(
                text = text,
                style = textStyle,
            )
        }
    }

    when (type) {
        Filled -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            contentPadding = contentPadding,
            shape = shape,
        ) { content() }

        Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            border = border,
            contentPadding = contentPadding,
            shape = shape,
        ) { content() }

        Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            contentPadding = contentPadding,
            shape = shape,
        ) { content() }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = Filled,
    size: ButtonSize = Large,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    LogFlareButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = Primary,
        type = type,
        size = size,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = Filled,
    size: ButtonSize = Large,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    LogFlareButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = Secondary,
        type = type,
        size = size,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = Primary,
    size: ButtonSize = Large,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    LogFlareButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        type = Outline,
        size = size,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

