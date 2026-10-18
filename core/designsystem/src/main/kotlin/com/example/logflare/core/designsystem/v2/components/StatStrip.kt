package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

enum class StatTone { Neutral, Error, Warning }

data class StatItem(
    val value: String,
    val label: String,
    val tone: StatTone = StatTone.Neutral,
    val onClick: (() -> Unit)? = null,
    val selected: Boolean = false,
)

@Composable
fun StatStrip(items: List<StatItem>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Lf.colors.surface)
            .padding(6.dp),
    ) {
        items.forEach { item ->
            val valueColor = when (item.tone) {
                StatTone.Neutral -> Lf.colors.textPrimary
                StatTone.Error -> Lf.colors.error
                StatTone.Warning -> Lf.colors.warning
            }
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (item.selected) Lf.colors.background else Color.Transparent)
                    .semantics(mergeDescendants = true) { selected = item.selected }
                    .then(
                        if (item.onClick != null) Modifier.clickable(role = Role.Button, onClick = item.onClick)
                        else Modifier,
                    )
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(item.value, style = Lf.type.statNumber, color = valueColor)
                Text(
                    item.label,
                    style = Lf.type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Lf.colors.textMuted,
                )
            }
        }
    }
}
