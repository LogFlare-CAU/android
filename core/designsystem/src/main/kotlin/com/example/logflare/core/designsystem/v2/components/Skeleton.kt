package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

@Composable
private fun Bar(fraction: Float, height: Int) {
    Box(Modifier.fillMaxWidth(fraction).height(height.dp).clip(RoundedCornerShape(6.dp)).background(Lf.colors.fill))
}

/** Static (shimmer-free) placeholder with the same metrics as a log/list row. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Lf.colors.fill))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bar(0.7f, 14)
            Bar(0.4f, 12)
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier, rows: Int = 3) {
    GroupedCard(modifier) { repeat(rows) { SkeletonRow() } }
}
