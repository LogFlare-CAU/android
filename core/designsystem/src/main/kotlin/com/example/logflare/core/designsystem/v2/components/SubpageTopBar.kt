package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.R
import com.example.logflare.core.designsystem.v2.Lf

enum class NavIcon { Back, Close }

/**
 * Sub-page bar. The status-bar inset is padded outside a fixed 56dp row, so the row never shrinks
 * under the status bar. [windowInsets] is overridable for tests.
 */
@Composable
fun SubpageTopBar(
    title: String,
    onNav: () -> Unit,
    modifier: Modifier = Modifier,
    navIcon: NavIcon = NavIcon.Back,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().windowInsetsPadding(windowInsets)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("SubpageTopBar.row")
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onNav) {
                Icon(
                    imageVector = if (navIcon == NavIcon.Back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = stringResource(if (navIcon == NavIcon.Back) R.string.lf_back else R.string.lf_close),
                    tint = Lf.colors.textPrimary,
                )
            }
            Text(
                title,
                style = Lf.type.barTitle,
                color = Lf.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}
