package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.logflare.core.designsystem.v2.Lf

/**
 * Screen root: background plus safe-drawing insets applied exactly once, to the content only (top and
 * sides, plus the bottom when there is no [bottomBar]). The [BottomNavBar] pads the navigation-bar
 * inset itself so its surface extends under the gesture area. Insets are consumed on the content box,
 * so nested bars that also request status-bar insets (e.g. SubpageTopBar) do not pad twice.
 * [toastHost] overlays the content bottom, above the bottom bar. [contentInsets] is overridable for tests.
 */
@Composable
fun LfScaffold(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    toastHost: LfToastState? = null,
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sides = if (bottomBar == null) {
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    } else {
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Lf.colors.background),
    ) {
        Box(
            Modifier
                .weight(1f)
                .windowInsetsPadding(contentInsets.only(sides)),
        ) {
            content(PaddingValues())
            // Overlay: never changes the content size; sits above the bottom bar (the content box ends
            // there) and above the navigation inset when there is no bottom bar.
            if (toastHost != null) LfToastHost(toastHost, Modifier.align(Alignment.BottomCenter))
        }
        bottomBar?.invoke()
    }
}
