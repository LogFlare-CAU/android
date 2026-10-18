package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfColors
import kotlinx.coroutines.launch

/** One row in a [SelectionSheet] or [MultiSelectSheet]. */
data class SheetOption<T>(
    val value: T,
    val label: String,
    val description: String? = null,
)

/**
 * Generic bottom sheet: surface container, 28dp top radius, drag handle, title, scrollable content.
 * [action] sits at the end of the title row (e.g. a "Mark all read" text button).
 */
@Composable
fun LfSheet(
    title: String,
    onDismiss: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = LfSheetFrame(title, onDismiss, action) { content() }

/**
 * Single choice. Tapping a row calls [onSelect], then animates the sheet away and calls
 * [onDismiss]. The selected row shows a check with content description "Selected".
 */
@Composable
fun <T> SelectionSheet(
    title: String,
    options: List<SheetOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) = LfSheetFrame(title, onDismiss) { dismiss ->
    options.forEach { option ->
        val isSelected = option.value == selected
        SheetRow(
            option = option,
            strong = isSelected,
            modifier = Modifier.selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = {
                    onSelect(option.value)
                    dismiss()
                },
            ),
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp),
                    tint = Lf.colors.textPrimary,
                )
            }
        }
    }
}

/**
 * Multiple choice with a local draft; "Save" calls [onConfirm] with the draft, then dismisses.
 * Options in [locked] keep their initial state and cannot be toggled (e.g. a Super Admin member).
 */
@Composable
fun <T> MultiSelectSheet(
    title: String,
    options: List<SheetOption<T>>,
    selected: Set<T>,
    locked: Set<T> = emptySet(),
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
) = LfSheetFrame(title, onDismiss) { dismiss ->
    var draft by remember(selected) { mutableStateOf(selected) }
    options.forEach { option ->
        val checked = option.value in draft
        val isLocked = option.value in locked
        SheetRow(
            option = option,
            strong = false,
            modifier = Modifier.toggleable(
                value = checked,
                enabled = !isLocked,
                role = Role.Checkbox,
                onValueChange = { on -> draft = if (on) draft + option.value else draft - option.value },
            ),
        ) {
            CheckBox(checked = checked, locked = isLocked)
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton(
        text = "Save",
        onClick = {
            onConfirm(draft)
            dismiss()
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Confirmation with two equal buttons: Cancel (secondary, dismisses) and [confirmLabel]
 * (error-colored when [destructive]). Confirm does not dismiss on its own, so callers can keep
 * the sheet up while work runs. [extra] renders between the body and the buttons (e.g. a
 * "type the project name" field driving [confirmEnabled]).
 */
@Composable
fun ConfirmSheet(
    title: String,
    body: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) = LfSheetFrame(title, onDismiss) { dismiss ->
    Text(
        body,
        style = Lf.type.body,
        color = Lf.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    if (extra != null) {
        Spacer(Modifier.height(12.dp))
        extra()
    }
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(text = "Cancel", onClick = dismiss, modifier = Modifier.weight(1f))
        if (destructive) {
            // onPrimaryAction is white in light and near-black in dark: readable on both error tones.
            LfButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = false,
                container = Lf.colors.error,
                content = Lf.colors.onPrimaryAction,
                modifier = Modifier.weight(1f),
            )
        } else {
            PrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Shared frame. [content] receives `dismiss`, which animates the sheet closed and then calls
 * [onDismiss]; swipe-down and scrim taps call [onDismiss] directly via Material3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LfSheetFrame(
    title: String,
    onDismiss: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { state.hide() }.invokeOnCompletion {
            if (!state.isVisible) onDismiss()
        }
    }
    // Cap the content, not the sheet: a heightIn on ModalBottomSheet's modifier shrinks its
    // anchoring box and lifts the sheet off the screen bottom. The handle takes 20dp.
    val maxContentHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f - DragHandleHeight
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = Lf.shapes.sheet,
        containerColor = Lf.colors.surface,
        contentColor = Lf.colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = Lf.colors.scrim,
        dragHandle = { DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = Lf.type.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
                    color = Lf.colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .semantics { heading() },
                )
                action?.invoke()
            }
            Spacer(Modifier.height(4.dp))
            content(dismiss)
        }
    }
}

private val DragHandleHeight = 20.dp // 12 top + 4 bar + 4 bottom

@Composable
private fun DragHandle() {
    Box(
        Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Lf.colors.disabled),
    )
}

@Composable
private fun <T> SheetRow(
    option: SheetOption<T>,
    strong: Boolean,
    modifier: Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Lf.shapes.input)
            .then(modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                option.label,
                style = if (strong) Lf.type.bodyStrong.copy(fontWeight = FontWeight.Bold) else Lf.type.bodyStrong,
                color = Lf.colors.textPrimary,
            )
            if (option.description != null) {
                Text(option.description, style = Lf.type.secondary, color = Lf.colors.textMuted)
            }
        }
        trailing()
    }
}

/** Border of an unchecked box: the box's only boundary, so it must reach 3:1 on the surface. */
internal fun uncheckedCheckBoxBorder(colors: LfColors): Color = colors.icon

@Composable
private fun CheckBox(checked: Boolean, locked: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    val box = Modifier
        .size(24.dp)
        .clip(shape)
    Box(
        if (checked) {
            box.background(if (locked) Lf.colors.icon else Lf.colors.primaryAction)
        } else {
            box.background(Lf.colors.surface).border(2.dp, uncheckedCheckBoxBorder(Lf.colors), shape)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (locked) Lf.colors.surface else Lf.colors.onPrimaryAction,
            )
        }
    }
}
