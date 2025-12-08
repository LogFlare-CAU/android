package com.example.logflare.core.designsystem.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

@Composable
fun LogFlareSnackbar(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppTheme.colors.neutral.s80,
        shape = AppTheme.radius.large,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.s4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.white,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LogFlareListItem(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AppTheme.radius.large,
        color = AppTheme.colors.neutral.s10,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s90,
                modifier = Modifier.weight(1f)
            )
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppTheme.colors.neutral.s60
                )
            }
        }
    }
}

@Composable
fun LogFlareRadioButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AppTheme.colors.neutral.white,
            uncheckedThumbColor = AppTheme.colors.neutral.white,
            checkedTrackColor = AppTheme.colors.primary.default,
            uncheckedTrackColor = AppTheme.colors.neutral.s40
        )
    )
}

@Composable
fun LogFlareCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.size(20.dp),
        colors = CheckboxDefaults.colors(
            checkedColor = AppTheme.colors.primary.default,
            uncheckedColor = AppTheme.colors.neutral.s40,
            checkmarkColor = AppTheme.colors.neutral.white
        )
    )
}
