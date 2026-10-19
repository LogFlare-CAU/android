package com.logflare.android.feature.home

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.components.LfSheet
import com.logflare.android.R
import com.logflare.android.push.HistoryEntry
import com.logflare.android.ui.format.levelLabel
import com.logflare.android.ui.format.logTitle
import com.logflare.android.ui.format.shortAge
import java.time.Instant

/**
 * Local push history, newest first. Tapping an entry marks it read and opens its error, except
 * test pushes (`errorId <= 0`), which have no error to open.
 */
@Composable
internal fun NotificationsSheet(
    alerts: List<HistoryEntry>,
    projectNames: Map<Int, String>,
    now: Instant,
    onMarkRead: (errorId: Int) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenLog: (errorId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val anyUnread = alerts.any { !it.read }
    // An opened alert closes the sheet first (animated), then navigates once it is gone.
    var afterDismiss by remember { mutableStateOf<(() -> Unit)?>(null) }
    LfSheet(
        title = stringResource(R.string.home_notifications),
        onDismiss = {
            onDismiss()
            afterDismiss?.invoke()
        },
        action = {
            Text(
                stringResource(R.string.home_mark_all_read),
                style = Lf.type.body.copy(fontWeight = FontWeight.Bold),
                color = if (anyUnread) Lf.colors.textPrimary else Lf.colors.textMuted,
                modifier = Modifier
                    .clip(Lf.shapes.input)
                    .clickable(enabled = anyUnread, role = Role.Button, onClick = onMarkAllRead)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        },
    ) { dismiss ->
        if (alerts.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.home_notifications_empty_title),
                    style = Lf.type.sectionTitle,
                    color = Lf.colors.textPrimary,
                )
                Text(
                    stringResource(R.string.home_notifications_empty_body),
                    style = Lf.type.body,
                    color = Lf.colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        alerts.forEach { entry ->
            AlertRow(entry, entry.projectId?.let(projectNames::get), now) {
                onMarkRead(entry.errorId)
                if (entry.opensLog) {
                    afterDismiss = { onOpenLog(entry.errorId) }
                    dismiss()
                }
            }
        }
    }
}

/** Test pushes carry no real error id. */
internal val HistoryEntry.opensLog: Boolean get() = errorId > 0

@Composable
private fun AlertRow(entry: HistoryEntry, projectName: String?, now: Instant, onClick: () -> Unit) {
    val unread = !entry.read
    val level = levelLabel(entry.level)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Lf.shapes.input)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (unread) Lf.colors.error else Color.Transparent),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (projectName != null) stringResource(R.string.home_notification_title, level, projectName) else level,
                style = Lf.type.bodyStrong.copy(fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium),
                color = Lf.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.home_dot_join, logTitle(entry.message), shortAge(Instant.ofEpochMilli(entry.receivedAt), now)),
                style = Lf.type.secondary,
                color = Lf.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
