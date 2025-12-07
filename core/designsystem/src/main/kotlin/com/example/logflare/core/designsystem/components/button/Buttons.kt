package com.example.logflare.core.designsystem.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

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
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primary.default,
            disabledContainerColor = AppTheme.colors.neutral.s20
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = AppTheme.radius.medium
    ) {
        Text(
            text = text,
            color = AppTheme.colors.neutral.white,
            style = AppTheme.typography.bodyMdBold
        )
    }
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.neutral.s70
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) AppTheme.colors.neutral.s60 else AppTheme.colors.neutral.s40
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = AppTheme.radius.medium
    ) {
        Text(
            text = text,
            style = AppTheme.typography.bodyMdBold
        )
    }
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.red.default
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) AppTheme.colors.red.default else AppTheme.colors.neutral.s60
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = AppTheme.radius.medium
    ) {
        Text(
            text = text,
            style = AppTheme.typography.bodyMdBold
        )
    }
}
