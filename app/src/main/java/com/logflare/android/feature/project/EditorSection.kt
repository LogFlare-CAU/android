package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = AppTheme.roles.layout.screenPadding)
            .padding(top = if (showDivider) AppTheme.spacing.s2 else AppTheme.spacing.s1),
    ) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = AppTheme.roles.layout.screenPadding),
                color = AppTheme.colors.outline.copy(alpha = 0.35f),
            )
        }
        Text(
            text = title,
            style = AppTheme.typography.titleSection.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(vertical = AppTheme.spacing.s1),
            )
        }
    }
}
