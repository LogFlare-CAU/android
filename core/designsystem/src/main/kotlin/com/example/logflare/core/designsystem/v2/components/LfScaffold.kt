package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.logflare.core.designsystem.v2.Lf

/**
 * Screen root: background plus WindowInsets.safeDrawing applied exactly once. Insets are consumed here,
 * so nested bars that also request status-bar insets (e.g. SubpageTopBar) do not pad twice.
 * The bottom bar sits below the content; content receives zero padding.
 * (The toast host parameter from the plan is added with LfToastState in a later task.)
 */
@Composable
fun LfScaffold(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Lf.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Box(Modifier.weight(1f)) { content(PaddingValues()) }
        bottomBar?.invoke()
    }
}
