package com.example.logflare.core.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.R

enum class TopAppBarTitleType { Default, Title }

@Composable
fun LogFlareTopAppBar(
    titleType: TopAppBarTitleType = TopAppBarTitleType.Default,
    titleText: String? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backTestTag: String? = null,
) {
    if (titleType == TopAppBarTitleType.Title) {
        require(!titleText.isNullOrBlank()) { "titleText is required when titleType is Title" }
    }

    val iconTint = AppTheme.colors.muted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            // surface, not neutral.white: an absolute here left the bar a white slab in dark mode.
            .background(AppTheme.colors.surface)
            .statusBarsPadding()
            .padding(horizontal = AppTheme.spacing.s4),
        contentAlignment = Alignment.Center
    ) {
        onBack?.let {
            val backModifier = Modifier
                .align(Alignment.CenterStart)
                .then(if (backTestTag != null) Modifier.testTag(backTestTag) else Modifier)
            IconButton(onClick = it, modifier = backModifier) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = iconTint
                )
            }
        }

        when (titleType) {
            TopAppBarTitleType.Default -> LogFlareWordmark(modifier = Modifier.align(Alignment.Center))
            TopAppBarTitleType.Title -> Text(
                text = titleText.orEmpty(),
                style = AppTheme.typography.bodyMdBold,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when {
            onClose != null -> IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = iconTint)
            }

            actionIcon != null && onActionClick != null -> IconButton(
                onClick = onActionClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(imageVector = actionIcon, contentDescription = "Action", tint = iconTint)
            }
        }
    }
}

@Composable
fun LogFlareWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String = "LogFlare logo"
) {
    // The drawable is single-ink and baked at #212121, which disappears on a dark bar. Icon paints
    // it as a mask, so the same asset serves both themes; onSurface is #212121 in light, leaving
    // the light rendering byte-identical to the untinted Image this replaced.
    Icon(
        painter = painterResource(id = R.drawable.ic_logflare_wordmark),
        contentDescription = contentDescription,
        tint = AppTheme.colors.onSurface,
        modifier = modifier.height(17.dp)
    )
}

