package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

@Composable
private fun LfButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    container: Color,
    content: Color,
    modifier: Modifier,
) {
    val active = enabled && !loading
    val bg = if (enabled) container else Lf.colors.disabled
    val fg = if (enabled) content else Lf.colors.onDisabled
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(Lf.shapes.button)
            .background(bg)
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .semantics { if (!enabled) disabled() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = fg, strokeWidth = 2.5.dp)
        } else {
            Text(text, style = Lf.type.sectionTitle, color = fg)
        }
    }
}

/** Filled dark action; 56dp tall, full width via [modifier]. Disabled uses the `disabled` token pair. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) = LfButton(text, onClick, enabled, loading, Lf.colors.primaryAction, Lf.colors.onPrimaryAction, modifier)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) = LfButton(text, onClick, enabled, loading, Lf.colors.secondaryAction, Lf.colors.textPrimary, modifier)

/** Left-aligned error-colored text row (e.g. "Delete project"). */
@Composable
fun DestructiveTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = Lf.type.bodyStrong, color = Lf.colors.error)
    }
}
