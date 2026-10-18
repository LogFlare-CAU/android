package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

/** Row with a trailing 48x28 switch. The whole row (>= 56dp) toggles; semantics are Role.Switch. */
@Composable
fun SwitchRow(
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = label,
        subtitle = description,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { LfSwitchVisual(checked) },
    )
}

@Composable
private fun LfSwitchVisual(checked: Boolean) {
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Lf.colors.primaryAction else Lf.colors.disabled)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .background(if (checked) Lf.colors.onPrimaryAction else Color.White, CircleShape),
        )
    }
}
