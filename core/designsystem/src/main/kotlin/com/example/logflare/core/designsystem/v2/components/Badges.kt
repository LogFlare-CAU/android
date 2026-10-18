package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

/** Neutral renders "x N" on the page-background chip; Error is a red pill; Warning uses the warning container. */
@Composable
fun CountBadge(count: Int, tone: StatTone, modifier: Modifier = Modifier) {
    val c = Lf.colors
    val (bg, fg) = when (tone) {
        StatTone.Neutral -> c.background to c.textSecondary
        StatTone.Error -> c.error to c.surface
        StatTone.Warning -> c.warningContainer to c.warning
    }
    Text(
        if (tone == StatTone.Neutral) "×$count" else "$count",
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = Lf.type.caption.copy(fontWeight = FontWeight.Bold),
        color = fg,
    )
}

@Composable
fun RoleChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Lf.colors.fill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = Lf.type.caption.copy(fontWeight = FontWeight.Bold),
        color = Lf.colors.textSecondary,
    )
}
