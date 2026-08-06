package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

/**
 * Group header for project Create/Settings forms.
 * Child field sections keep their own field labels and padding.
 */
@Composable
fun EditorSectionHeader(
    title: String,
    description: String? = null,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = if (showDivider) 8.dp else 4.dp),
    ) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                color = AppTheme.colors.outline.copy(alpha = 0.35f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
