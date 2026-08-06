package com.logflare.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.logflare.core.designsystem.components.state.LogFlareEmptyState
import com.example.logflare.core.designsystem.components.state.LogFlareErrorState
import com.example.logflare.core.designsystem.components.state.LogFlareLoadingState
import com.logflare.android.ui.VisualQaTags

@Composable
fun ListLoadingState(modifier: Modifier = Modifier) {
    LogFlareLoadingState(modifier = modifier.testTag(VisualQaTags.Loading))
}

@Composable
fun ListErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    LogFlareErrorState(
        message = message,
        onRetry = onRetry,
        modifier = modifier.testTag(VisualQaTags.Error),
    )
}

@Composable
fun ListEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onAction: (() -> Unit)? = null,
) {
    LogFlareEmptyState(
        title = title,
        subtitle = subtitle,
        onAction = onAction,
        actionLabel = actionLabel,
        actionModifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier,
        modifier = modifier.testTag(VisualQaTags.Empty),
    )
}
