package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

/**
 * Labeled text field. Idle = `background` fill, no border; focused = 2dp `textPrimary` border on `surface`;
 * error = 2dp `error` border and error text under the field.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    success: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var focused by remember { mutableStateOf(false) }
    val c = Lf.colors
    val borderColor = when {
        error != null -> c.error
        focused -> c.textPrimary
        else -> null
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = Lf.type.secondary.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            textStyle = Lf.type.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.textPrimary),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(Lf.shapes.input)
                        .background(if (focused || error != null) c.surface else c.background)
                        .let { if (borderColor != null) it.border(2.dp, borderColor, Lf.shapes.input) else it }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = Lf.type.body, color = c.textMuted)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        val message = error ?: success ?: helper
        if (message != null) {
            Text(
                message,
                style = Lf.type.caption,
                color = when {
                    error != null -> c.error
                    success != null -> c.textPrimary
                    else -> c.textMuted
                },
            )
        }
    }
}
