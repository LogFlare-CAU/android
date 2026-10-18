package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logflare.core.designsystem.v2.Lf

@Composable
fun Avatar(initial: String, size: Dp = 40.dp, strong: Boolean = false, modifier: Modifier = Modifier) {
    val bg = if (strong) Lf.colors.primaryAction else Lf.colors.fill
    val fg = if (strong) Lf.colors.onPrimaryAction else Lf.colors.textSecondary
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(
            initial.take(1).uppercase(),
            style = Lf.type.bodyStrong.copy(fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp),
            color = fg,
        )
    }
}
