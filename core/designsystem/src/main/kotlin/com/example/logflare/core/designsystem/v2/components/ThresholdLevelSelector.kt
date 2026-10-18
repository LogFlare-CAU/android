package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel

/** The selected level and every level above it ("this level and above"). */
fun thresholdHighlighted(selected: LfLevel): Set<LfLevel> =
    LfLevel.entries.filter { it.ordinal >= selected.ordinal }.toSet()

/** One segmented row; [selected] and everything to its right are highlighted. [summary] is caller-provided. */
@Composable
fun ThresholdLevelSelector(
    selected: LfLevel,
    onSelect: (LfLevel) -> Unit,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val highlighted = thresholdHighlighted(selected)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Lf.shapes.input)
                .background(Lf.colors.background)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            LfLevel.entries.forEach { level ->
                val on = level in highlighted
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Lf.colors.primaryAction else Lf.colors.fill)
                        .selectable(selected = level == selected, role = Role.RadioButton, onClick = { onSelect(level) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        level.name,
                        style = Lf.type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (on) Lf.colors.onPrimaryAction else Lf.colors.textMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(summary, style = Lf.type.caption.copy(fontWeight = FontWeight.SemiBold), color = Lf.colors.textSecondary)
    }
}
