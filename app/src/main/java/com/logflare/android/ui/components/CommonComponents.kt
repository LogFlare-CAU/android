package com.logflare.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.logflare.core.designsystem.AppTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

/**
 * Standard back header used across screens.
 * Places a left-aligned back button and a bold title.
 */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = AppTheme.colors.onSurface,
    iconTint: Color = AppTheme.colors.secondary.default,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 12.dp
) {
    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = iconTint
            )
        }
    Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor
        )
    }
}

/**
 * Standard primary CTA button anchored near the bottom.
 * Provides consistent height and padding so text doesn't clip.
 */
@Composable
fun BottomPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.primary.default,
    disabledContainerColor: Color = AppTheme.colors.primary.disabled
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor,
            contentColor = AppTheme.colors.onPrimary,
            disabledContentColor = AppTheme.colors.onPrimary
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, color = AppTheme.colors.onPrimary, fontWeight = FontWeight.Bold)
    }
}

/**
 * Standard outlined secondary CTA button for cancel or destructive confirmations.
 */
@Composable
fun BottomOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    contentColor: Color = AppTheme.colors.muted,
    borderColor: Color = AppTheme.colors.outline
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
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
    BottomOutlinedButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentColor = AppTheme.colors.red.default,
        borderColor = if (enabled) AppTheme.colors.red.default else AppTheme.colors.outline
    )
}
