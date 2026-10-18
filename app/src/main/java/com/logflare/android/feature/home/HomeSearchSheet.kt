package com.logflare.android.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.LevelBadge
import com.example.logflare.core.designsystem.v2.components.LfSheet
import com.example.logflare.core.designsystem.v2.components.RoleChip
import com.example.logflare.core.designsystem.v2.components.SearchField
import com.logflare.android.R
import com.logflare.android.ui.format.relativeTime
import java.time.Instant

/** Search over what Home loaded: recent errors first, then projects. Results follow [query] debounced (VM). */
@Composable
internal fun HomeSearchSheet(
    query: String,
    results: SearchResults,
    projectNames: Map<Int, String>,
    now: Instant,
    onQuery: (String) -> Unit,
    onOpenLog: (Int) -> Unit,
    onOpenProject: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    // A tapped row closes the sheet first (animated), then navigates once it is gone.
    var afterDismiss by remember { mutableStateOf<(() -> Unit)?>(null) }
    LfSheet(
        title = stringResource(R.string.home_search),
        onDismiss = {
            onDismiss()
            afterDismiss?.invoke()
        },
    ) { dismiss ->
        SearchField(
            value = query,
            onValueChange = onQuery,
            description = stringResource(R.string.home_search_field),
            placeholder = stringResource(R.string.home_search_placeholder),
            modifier = Modifier.focusRequester(focus),
        )
        Spacer(Modifier.height(8.dp))
        results.logs.forEach { log ->
            val ago = relativeTime(log.lastSeen, now)
            ResultRow(
                tag = { LevelBadge(log.level) },
                title = log.message,
                subtitle = projectNames[log.projectId]?.let { stringResource(R.string.home_dot_join, ago, it) } ?: ago,
                onClick = {
                    afterDismiss = { onOpenLog(log.id) }
                    dismiss()
                },
            )
        }
        results.projects.forEach { p ->
            ResultRow(
                tag = { RoleChip(stringResource(R.string.home_search_project_tag)) },
                title = p.name,
                subtitle = p.lastErrorAt?.let { stringResource(R.string.home_last_error, relativeTime(it, now)) }
                    ?: stringResource(R.string.home_no_errors),
                onClick = {
                    afterDismiss = { onOpenProject(p.id) }
                    dismiss()
                },
            )
        }
        if (results.logs.isEmpty() && results.projects.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.home_search_no_results),
                    style = Lf.type.sectionTitle,
                    color = Lf.colors.textPrimary,
                )
                if (query.isNotBlank()) {
                    Text(
                        stringResource(R.string.home_search_no_results_body, query.trim()),
                        style = Lf.type.body,
                        color = Lf.colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun ResultRow(tag: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Lf.shapes.input)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tag()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = Lf.type.bodyStrong,
                color = Lf.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = Lf.type.secondary,
                color = Lf.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
