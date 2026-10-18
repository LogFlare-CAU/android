package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.v2.Lf

@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 4.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = Lf.type.largeTitle,
            color = Lf.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** Home variant: 36dp logo tile plus the wordmark. [logo] is passed in (the drawable lives in :app). */
@Composable
fun HomeHeader(
    logo: Painter,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Lf.colors.primaryAction),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = logo,
                    contentDescription = null,
                    modifier = Modifier.size(width = 18.dp, height = 24.dp),
                    colorFilter = ColorFilter.tint(Lf.colors.onPrimaryAction),
                )
            }
            Text(
                "LogFlare",
                style = Lf.type.largeTitle.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
                color = Lf.colors.textPrimary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * 48dp header icon button (search, bell, add). [dot] adds the red unread marker; say so in
 * [contentDescription] too, since the dot itself is not announced.
 */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Boolean = false,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Lf.colors.textSecondary, modifier = Modifier.size(24.dp))
        if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 11.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Lf.colors.error)
                    .border(2.dp, Lf.colors.background, CircleShape),
            )
        }
    }
}

/**
 * Dark "+ New" pill for a tab header's actions (admin-only create). [contentDescription] defaults
 * to [text]; a null [icon] leaves the text alone.
 */
@Composable
fun HeaderPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    val description = contentDescription ?: text
    Row(
        modifier
            .padding(start = 4.dp)
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(Lf.colors.primaryAction)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = description }
            .padding(start = if (icon != null) 10.dp else 16.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Lf.colors.onPrimaryAction, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
            color = Lf.colors.onPrimaryAction,
            maxLines = 1,
        )
    }
}

/**
 * Line under a tab header: a muted [label] (e.g. "5 projects") and, at the end, a text action
 * with an optional [actionIcon] (e.g. the sort picker).
 */
@Composable
fun ListToolbar(
    label: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = Lf.type.caption.copy(fontWeight = FontWeight.Bold),
            color = Lf.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .heightIn(min = 44.dp)
                .clip(Lf.shapes.input)
                .clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (actionIcon != null) {
                Icon(actionIcon, contentDescription = null, tint = Lf.colors.textSecondary, modifier = Modifier.size(16.dp))
            }
            Text(
                actionText,
                style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                color = Lf.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}
