package com.example.logflare.core.designsystem.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf

sealed interface KeywordResult {
    data class Added(val value: String) : KeywordResult
    data object Blank : KeywordResult
    data object Duplicate : KeywordResult
}

/** Trims [raw]; blank -> Blank; case-insensitive match against [existing] -> Duplicate. */
fun normalizeKeyword(raw: String, existing: Collection<String>): KeywordResult {
    val v = raw.trim()
    if (v.isEmpty()) return KeywordResult.Blank
    if (existing.any { it.equals(v, ignoreCase = true) }) return KeywordResult.Duplicate
    return KeywordResult.Added(v)
}

@Composable
fun KeywordChipInput(
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    placeholder: String,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() {
        when (val r = normalizeKeyword(text, keywords)) {
            is KeywordResult.Added -> { onAdd(r.value); text = "" }
            KeywordResult.Duplicate -> onDuplicate()
            KeywordResult.Blank -> Unit
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = "Keyword",
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Box(
                Modifier
                    .padding(top = 28.dp)
                    .heightIn(min = 52.dp)
                    .width(64.dp)
                    .clip(Lf.shapes.input)
                    .background(if (text.isBlank()) Lf.colors.disabled else Lf.colors.primaryAction)
                    .clickable(enabled = text.isNotBlank(), role = Role.Button) { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Add",
                    style = Lf.type.bodyStrong,
                    color = if (text.isBlank()) Lf.colors.onDisabled else Lf.colors.onPrimaryAction,
                )
            }
        }
        WrapLayout(Modifier.fillMaxWidth(), spacing = 8.dp) {
            keywords.forEach { kw ->
                Row(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Lf.colors.background)
                        .padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(kw, style = Lf.type.secondary.copy(fontWeight = FontWeight.SemiBold), color = Lf.colors.textPrimary)
                    Box(
                        Modifier
                            .size(44.dp)
                            .clickable(onClickLabel = "Remove $kw", role = Role.Button) { onRemove(kw) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "Remove $kw", Modifier.size(14.dp), tint = Lf.colors.textMuted)
                    }
                }
            }
        }
    }
}

/** Minimal wrapping layout (avoids FlowRow, whose ABI differs across Compose foundation versions). */
@Composable
private fun WrapLayout(modifier: Modifier, spacing: Dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(loose) }
        val positions = ArrayList<Pair<Int, Int>>()
        var x = 0
        var y = 0
        var rowH = 0
        placeables.forEach { p ->
            if (x > 0 && x + p.width > constraints.maxWidth) {
                x = 0
                y += rowH + gap
                rowH = 0
            }
            positions += x to y
            x += p.width + gap
            rowH = maxOf(rowH, p.height)
        }
        layout(constraints.maxWidth, if (placeables.isEmpty()) 0 else y + rowH) {
            placeables.forEachIndexed { i, p -> p.placeRelative(positions[i].first, positions[i].second) }
        }
    }
}
