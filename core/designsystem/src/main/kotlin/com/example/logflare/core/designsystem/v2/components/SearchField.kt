package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.R
import com.example.logflare.core.designsystem.v2.Lf

/**
 * Unlabeled search input: 48dp, `background` fill, leading search icon. [description] names the
 * field for accessibility (there is no visible label); [placeholder] shows while empty.
 * [containerColor] defaults to the page background (for use on a surface, e.g. in a sheet); pass
 * `surface` on a page. With [onClear], a non-empty field shows a clear button named [clearDescription].
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSearch: (() -> Unit)? = null,
    containerColor: Color = Lf.colors.background,
    onClear: (() -> Unit)? = null,
    clearDescription: String = stringResource(R.string.lf_clear),
) {
    val c = Lf.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        textStyle = Lf.type.body.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.textPrimary),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(Lf.shapes.input)
                    .background(containerColor)
                    .padding(start = 14.dp, end = if (onClear != null && value.isNotEmpty()) 2.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = c.icon, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = Lf.type.body, color = c.textMuted, maxLines = 1)
                    }
                    inner()
                }
                if (onClear != null && value.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(Lf.shapes.input)
                            .clickable(role = Role.Button, onClick = onClear)
                            .semantics { contentDescription = clearDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    )
}
