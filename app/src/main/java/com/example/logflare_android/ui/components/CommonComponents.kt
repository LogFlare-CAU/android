package com.example.logflare_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

// Local color tokens aligned with existing screens
private val ColorNeutralBlack = Color(0xFF1A1A1A)
private val ColorNeutral20 = Color(0xFFEEEEEE)
private val ColorNeutral60 = Color(0xFF757575)
private val ColorNeutral70 = Color(0xFF616161)
private val ColorPrimaryDefault = Color(0xFF60B176)
private val ColorDanger = Color(0xFFB12B38)
private val ColorSecondaryDefault = Color(0xFF9E9E9E)

/**
 * Standard back header used across screens.
 * Places a left-aligned back button and a bold title.
 */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = ColorNeutralBlack,
    iconTint: Color = ColorSecondaryDefault,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 12.dp
) {
    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = iconTint
            )
        }
    Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor
        )
    }
}

/**
 * Standard primary CTA button anchored near the bottom.
 * Provides consistent height and padding so text doesn't clip.
 */
@Composable
fun BottomPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    containerColor: Color = ColorPrimaryDefault,
    disabledContainerColor: Color = ColorNeutral20
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/**
 * Standard outlined secondary CTA button for cancel or destructive confirmations.
 */
@Composable
fun BottomOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    contentColor: Color = ColorNeutral70,
    borderColor: Color = ColorNeutral60
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(vertical = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

/**
 * Danger-styled outlined button variant used for delete actions.
 */
@Composable
fun BottomDangerOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BottomOutlinedButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentColor = ColorDanger,
        borderColor = if (enabled) ColorDanger else ColorNeutral60
    )
}
