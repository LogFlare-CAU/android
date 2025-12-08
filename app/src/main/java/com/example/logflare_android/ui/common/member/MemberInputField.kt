package com.example.logflare_android.ui.common.member

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonSize
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.input.LogFlareTextField

/**
 * Visual contract for the inline input + status button rows used in Add/Edit member screens.
 */
enum class MemberFieldStatus {
    Idle,
    Validating,
    Valid,
    Error,
    Completed
}

@Composable
fun MemberInputField(
    title: String,
    value: String,
    placeholder: String,
    status: MemberFieldStatus,
    helperText: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppTheme.colors.neutral.white
    ) {
        Column {
            Text(
                text = title,
                style = AppTheme.typography.bodySmBold,
                color = AppTheme.colors.neutral.black,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.s4)
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.s4))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogFlareTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = null,
                    placeholder = placeholder,
                    helperText = null,
                    isError = status == MemberFieldStatus.Error,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )

                MemberFieldStatusButton(
                    status = status,
                    minWidth = 74.dp
                )
            }

            helperText?.takeIf { it.isNotBlank() }?.let { text ->
                Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
                Row(
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s1)
                ) {
                    if (status == MemberFieldStatus.Error) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = AppTheme.colors.red.default,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = text,
                        style = AppTheme.typography.captionSmMedium,
                        color = if (status == MemberFieldStatus.Error) {
                            AppTheme.colors.red.default
                        } else {
                            AppTheme.colors.neutral.s70
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberFieldStatusButton(
    status: MemberFieldStatus,
    minWidth: Dp,
    modifier: Modifier = Modifier
) {
    val (text, variant, enabled) = when (status) {
        MemberFieldStatus.Idle -> Triple("Auto", ButtonVariant.Secondary, false)
        MemberFieldStatus.Error -> Triple("Check", ButtonVariant.Secondary, false)
        MemberFieldStatus.Validating -> Triple("Checking", ButtonVariant.Primary, true)
        MemberFieldStatus.Valid -> Triple("Ready", ButtonVariant.Primary, true)
        MemberFieldStatus.Completed -> Triple("Saved", ButtonVariant.Secondary, true)
    }

    LogFlareButton(
        text = text,
        onClick = {},
        variant = variant,
        size = ButtonSize.Field,
        enabled = enabled,
        modifier = modifier
            .width(minWidth)
    )
}
