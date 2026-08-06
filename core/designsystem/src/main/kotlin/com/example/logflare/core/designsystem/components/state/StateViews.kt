package com.example.logflare.core.designsystem.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton

@Composable
fun LogFlareLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppTheme.colors.primary.default)
    }
}

@Composable
fun LogFlareErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.roles.layout.statePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap),
        ) {
            Text(
                text = message,
                color = AppTheme.colors.red.default,
                style = AppTheme.typography.bodyMdMedium,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                LogFlareButton(
                    text = "Retry",
                    onClick = onRetry,
                    type = ButtonType.Outline,
                    variant = ButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
fun LogFlareEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.roles.layout.statePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.bodyLgBold,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodyMdMedium,
                    color = AppTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
                LogFlareButton(
                    text = actionLabel,
                    onClick = onAction,
                    type = ButtonType.Filled,
                    variant = ButtonVariant.Primary,
                    modifier = actionModifier,
                )
            }
        }
    }
}
