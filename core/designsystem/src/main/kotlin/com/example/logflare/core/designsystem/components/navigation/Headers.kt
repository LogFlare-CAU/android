package com.example.logflare.core.designsystem.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

/**
 * Standard back header used across screens.
 * Places a left-aligned back button and a bold title.
 */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = AppTheme.colors.neutral.black,
    iconTint: Color = AppTheme.colors.secondary.default,
    horizontalPadding: Dp = AppTheme.spacing.s4,
    verticalPadding: Dp = AppTheme.spacing.s3
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
        Spacer(Modifier.width(AppTheme.spacing.s2))
        Text(
            text = title,
            style = AppTheme.typography.bodyLgBold,
            color = titleColor
        )
    }
}
