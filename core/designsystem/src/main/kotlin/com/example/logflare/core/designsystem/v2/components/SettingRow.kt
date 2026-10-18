package com.example.logflare.core.designsystem.v2.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.logflare.core.designsystem.v2.Lf

/** Label on the left, current value and chevron on the right. */
@Composable
fun SettingRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = label,
        modifier = modifier,
        trailing = value?.let {
            { Text(it, style = Lf.type.body, color = Lf.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        showChevron = true,
        onClick = onClick,
    )
}
