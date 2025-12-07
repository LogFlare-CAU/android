package com.example.logflare_android.ui.components

/**
 * DEPRECATED: This file is kept for backward compatibility.
 * All components have been moved to core/designsystem.
 * 
 * Please update your imports to use:
 * - com.example.logflare.core.designsystem.components.button.*
 * - com.example.logflare.core.designsystem.components.navigation.*
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// Re-export wrappers for backward compatibility

@Composable
fun BottomPrimaryButton(
	text: String,
	onClick: () -> Unit,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	com.example.logflare.core.designsystem.components.button.BottomPrimaryButton(
		text = text,
		onClick = onClick,
		enabled = enabled,
		modifier = modifier
	)
}

@Composable
fun BottomOutlinedButton(
	text: String,
	onClick: () -> Unit,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	com.example.logflare.core.designsystem.components.button.BottomOutlinedButton(
		text = text,
		onClick = onClick,
		enabled = enabled,
		modifier = modifier
	)
}

@Composable
fun BottomDangerOutlinedButton(
	text: String,
	onClick: () -> Unit,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	com.example.logflare.core.designsystem.components.button.BottomDangerOutlinedButton(
		text = text,
		onClick = onClick,
		enabled = enabled,
		modifier = modifier
	)
}

@Composable
fun BackHeader(
	title: String,
	onBack: () -> Unit,
	modifier: Modifier = Modifier,
	titleColor: Color = com.example.logflare.core.designsystem.AppTheme.colors.neutral.black,
	iconTint: Color = com.example.logflare.core.designsystem.AppTheme.colors.secondary.default,
	horizontalPadding: Dp = com.example.logflare.core.designsystem.AppTheme.spacing.s4,
	verticalPadding: Dp = com.example.logflare.core.designsystem.AppTheme.spacing.s3
) {
	com.example.logflare.core.designsystem.components.navigation.BackHeader(
		title = title,
		onBack = onBack,
		modifier = modifier,
		titleColor = titleColor,
		iconTint = iconTint,
		horizontalPadding = horizontalPadding,
		verticalPadding = verticalPadding
	)
}
