package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            Box(
                Modifier
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .clickable(role = Role.Button) { shown = !shown }
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (shown) "Hide" else "Show",
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textSecondary,
                )
            }
        },
    )
}
