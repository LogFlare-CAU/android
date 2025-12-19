package com.example.logflare.core.designsystem.components.dropdown

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

/** Visual presets for dropdown height/text sizing. */
enum class DropdownSize { Large, Small }

/**
 * Generic design-system dropdown that renders a compact header and menu matching
 * the LogFlare design tokens. Works with any value type via [itemLabelMapper].
 */
@Composable
fun <T> LogFlareDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabelMapper: (T) -> String = { it.toString() },
    placeholder: String = "Select",
    size: DropdownSize = DropdownSize.Small,
    modifier: Modifier = Modifier,
    showCheckboxInMenu: Boolean = true,
    disabled: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val metrics = dropdownMetrics(size)
    val colors = AppTheme.colors

    val currentLabel = selectedItem?.let(itemLabelMapper) ?: placeholder
    val isPlaceholder = selectedItem == null
    val textColor = when {
        isPlaceholder -> colors.neutral.s50
        expanded -> colors.primary.default
        else -> colors.neutral.s70
    }

    Box(modifier = modifier) {
        Surface(
            shape = AppTheme.radius.medium,
            color = colors.neutral.white,
            border = BorderStroke(
                width = 1.dp,
                color = if (expanded) colors.primary.default else colors.neutral.s40
            ),
            onClick = { if (items.isNotEmpty()) expanded = !expanded },
            enabled = items.isNotEmpty() && !disabled
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.height)
                    .padding(horizontal = AppTheme.spacing.s3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    color = textColor,
                    style = metrics.textStyle,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.s2))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(color = colors.neutral.white, shape = AppTheme.radius.medium)
                .border(1.dp, colors.neutral.s40, AppTheme.radius.medium),
        ) {
            items.forEach { item ->
                val isSelected = selectedItem == item
                DropdownItem(
                    text = itemLabelMapper(item),
                    selected = isSelected,
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    size = size,
                    showCheckbox = showCheckboxInMenu,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Menu row used inside [LogFlareDropdown] (or standalone menus) with optional checkbox visuals.
 */
@Composable
fun DropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    size: DropdownSize,
    showCheckbox: Boolean = false,
    modifier: Modifier = Modifier
) {
    val metrics = dropdownMetrics(size)
    val colors = AppTheme.colors
    val textColor = if (selected) colors.primary.default else colors.neutral.s70

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCheckbox) {
                    DropdownCheckbox(selected = selected)
                    Spacer(modifier = Modifier.width(AppTheme.spacing.s2))
                }
                Text(
                    text = text,
                    color = textColor,
                    style = metrics.textStyle
                )
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = AppTheme.spacing.s3),
        modifier = modifier.height(metrics.height),
        colors = MenuDefaults.itemColors(
            textColor = Color.Unspecified,
            leadingIconColor = Color.Unspecified,
            trailingIconColor = Color.Unspecified
        )
    )
}

@Composable
private fun DropdownCheckbox(selected: Boolean) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(4.dp)
    val size = 16.dp

    if (selected) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(colors.primary.default),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.neutral.white,
                modifier = Modifier.size(12.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .border(1.dp, colors.neutral.s40, shape)
        )
    }
}

private data class DropdownMetrics(
    val height: Dp,
    val textStyle: TextStyle
)

@Composable
private fun dropdownMetrics(size: DropdownSize): DropdownMetrics = when (size) {
    DropdownSize.Large -> DropdownMetrics(
        height = 28.dp,
        textStyle = AppTheme.typography.bodySmMedium
    )
    DropdownSize.Small -> DropdownMetrics(
        height = 24.dp,
        textStyle = AppTheme.typography.captionSmMedium
    )
}
