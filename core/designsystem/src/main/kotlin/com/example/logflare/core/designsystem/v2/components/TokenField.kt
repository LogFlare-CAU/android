package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

/** First 6 + 12 bullets + last 4; tokens too short to split are fully masked. */
fun maskToken(token: String): String =
    if (token.length <= 10) "•".repeat(12)
    else token.take(6) + "•".repeat(12) + token.takeLast(4)

@Composable
fun TokenField(
    token: String?,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onRotate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Project token", style = Lf.type.sectionTitle, color = Lf.colors.textPrimary, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable(role = Role.Button, onClick = onToggleReveal)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (revealed) "Hide" else "Show",
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textSecondary,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(Lf.shapes.input)
                .background(Lf.colors.background)
                .padding(14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = when {
                    token == null -> "—"
                    revealed -> token
                    else -> maskToken(token)
                },
                style = Lf.type.secondary.copy(fontFamily = FontFamily.Monospace),
                color = Lf.colors.textSecondary,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TokenAction("Copy", true, onCopy, Modifier.weight(1f))
            if (onRotate != null) TokenAction("Rotate", false, onRotate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TokenAction(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(Lf.shapes.input)
            .background(if (primary) Lf.colors.primaryAction else Lf.colors.secondaryAction)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
            color = if (primary) Lf.colors.onPrimaryAction else Lf.colors.textPrimary,
        )
    }
}
