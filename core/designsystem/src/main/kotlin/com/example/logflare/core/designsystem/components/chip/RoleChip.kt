package com.example.logflare.core.designsystem.components.chip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

enum class ChipSize {
    Large,
    Small
}

@Composable
fun RoleChip(
    text: String,
    size: ChipSize = ChipSize.Small,
    backgroundColor: Color,
    contentColor: Color = AppTheme.colors.neutral.s5,
    modifier: Modifier = Modifier
) {
    val (containerHeight, horizontalPadding, textStyle) = when (size) {
        ChipSize.Large -> Triple(28.dp, 12.dp, AppTheme.typography.bodySmMedium)
        ChipSize.Small -> Triple(20.dp, 8.dp, AppTheme.typography.captionSmMedium)
    }

    Surface(
        color = backgroundColor,
        shape = AppTheme.radius.medium,
        modifier = modifier.height(containerHeight)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        ) {
            Text(
                text = text,
                color = contentColor,
                style = textStyle
            )
        }
    }
}
