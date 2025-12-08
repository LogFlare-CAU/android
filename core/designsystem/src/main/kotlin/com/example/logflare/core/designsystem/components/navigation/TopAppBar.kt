package com.example.logflare.core.designsystem.components.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    modifier: Modifier = Modifier
) {
    if (titleType == TopAppBarTitleType.Title) {
        require(!titleText.isNullOrBlank()) { "titleText is required when titleType is Title" }
    }

    val iconTint = AppTheme.colors.neutral.s50
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppTheme.colors.neutral.white)
            .padding(horizontal = AppTheme.spacing.s4),
        contentAlignment = Alignment.Center
    ) {
        onBack?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.CenterStart)) {
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
                color = AppTheme.colors.neutral.black,
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
    Image(
        painter = painterResource(id = R.drawable.ic_logflare_wordmark),
        contentDescription = contentDescription,
        modifier = modifier.height(17.dp)
    )
}

