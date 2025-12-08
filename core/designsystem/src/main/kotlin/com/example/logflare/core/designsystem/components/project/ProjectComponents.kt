package com.example.logflare.core.designsystem.components.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

@Composable
fun ProjectCard(
    name: String,
    lastLogTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = AppTheme.colors.neutral.s20,
        shape = AppTheme.radius.large,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = name,
                    style = AppTheme.typography.bodyMdBold,
                    color = AppTheme.colors.neutral.s90
                )
                Spacer(modifier = Modifier.weight(1f, fill = true))
                LogTimeRow(lastLogTime = lastLogTime)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppTheme.colors.neutral.s50,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun LogTimeRow(lastLogTime: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last Log",
            style = AppTheme.typography.bodySmLight,
            color = AppTheme.colors.neutral.s60
        )
        Spacer(modifier = Modifier.width(AppTheme.spacing.s4))
        Text(
            text = lastLogTime,
            style = AppTheme.typography.bodySmLight,
            color = AppTheme.colors.neutral.s60
        )
    }
}
