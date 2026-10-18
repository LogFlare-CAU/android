package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

/** Dropdown-style filter chip (opens a picker). Active = a filter is applied. */
@Composable
fun LfFilterChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fg = if (active) Lf.colors.onPrimaryAction else Lf.colors.textPrimary
    Row(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) Lf.colors.primaryAction else Lf.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
        Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(20.dp), tint = fg)
    }
}
