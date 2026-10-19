package com.logflare.android.feature.log

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.logflare.android.ui.copyText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfIcons
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.HeaderIconButton
import com.example.logflare.core.designsystem.v2.components.LevelBadge
import com.example.logflare.core.designsystem.v2.components.LfSheet
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.LogEventDetail
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.format.appLocale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogDetailRoute(
    onBack: () -> Unit,
    onOpenProject: (id: Int) -> Unit,
    onOpenLog: (id: Int) -> Unit,
    viewModel: LogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LogDetailContent(
        state = state,
        onBack = onBack,
        onToggleRaw = viewModel::toggleRaw,
        onOpenShare = viewModel::openShare,
        onCloseShare = viewModel::closeShare,
        onRetry = viewModel::retry,
        onOpenProject = onOpenProject,
        onOpenLog = onOpenLog,
        toast = rememberLfToastState(),
    )
}

/** Raw data shows this many lines until expanded. */
private const val RAW_COLLAPSED_LINES = 6

@Composable
fun LogDetailContent(
    state: LogDetailUiState,
    onBack: () -> Unit,
    onToggleRaw: () -> Unit,
    onOpenShare: () -> Unit,
    onCloseShare: () -> Unit,
    onRetry: () -> Unit,
    onOpenProject: (id: Int) -> Unit,
    onOpenLog: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    toast: LfToastState? = null,
) {
    val appContext = LocalContext.current
    val copiedLabel = stringResource(R.string.log_copied)
    val copy: (String) -> Unit = { text ->
        appContext.copyText(text)
        toast?.show(copiedLabel)
    }
    val detail = state.detail

    Box(modifier.fillMaxSize().testTag(VisualQaTags.LogDetail)) {
        Column(Modifier.fillMaxSize()) {
            SubpageTopBar(title = stringResource(R.string.log_title), onNav = onBack) {
                if (detail != null) {
                    HeaderIconButton(LfIcons.Share, stringResource(R.string.log_share), onClick = onOpenShare)
                }
            }
            when {
                detail != null -> Body(state, detail, zone, onToggleRaw, onOpenProject, onOpenLog, copy)
                state.error == LogDetailError.NotFound -> Message {
                    EmptyState(
                        title = stringResource(R.string.log_not_found),
                        body = null,
                        actionLabel = stringResource(R.string.log_back),
                        onAction = onBack,
                    )
                }
                state.error == LogDetailError.LoadFailed -> Message {
                    EmptyState(
                        title = stringResource(R.string.log_load_failed_title),
                        body = stringResource(R.string.log_load_failed_body),
                        actionLabel = stringResource(R.string.logs_retry),
                        onAction = onRetry,
                    )
                }
                else -> Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonCard(rows = 1)
                    SkeletonCard(rows = 4)
                }
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }

    if (state.shareSheetOpen && detail != null) {
        LfSheet(title = stringResource(R.string.log_share_title), onDismiss = onCloseShare) { dismiss ->
            ShareOption(LfIcons.ContentCopy, stringResource(R.string.log_copy_message)) {
                copy(detail.event.message)
                dismiss()
            }
        }
    }
}

@Composable
private fun Message(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
}

@Composable
private fun Body(
    state: LogDetailUiState,
    detail: LogEventDetail,
    zone: ZoneId,
    onToggleRaw: () -> Unit,
    onOpenProject: (Int) -> Unit,
    onOpenLog: (Int) -> Unit,
    copy: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Hero(detail, state.projectName, zone)
        InfoCard(detail, state.projectName, zone, onOpenProject)
        if (!detail.raw.isNullOrEmpty()) RawCard(detail.raw, state.rawExpanded, onToggleRaw, copy)
        if (detail.recentIds.isNotEmpty()) EarlierCard(detail.recentIds, onOpenLog)
    }
}

@Composable
private fun Hero(detail: LogEventDetail, projectName: String?, zone: ZoneId) {
    GroupedCard(contentPadding = PaddingValues(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LevelBadge(detail.rowLevel)
            Text(
                detail.event.message,
                style = Lf.type.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
                color = Lf.colors.textPrimary,
            )
            val parts = listOfNotNull(
                projectName,
                dateTime(detail.occurredAt, zone),
                if (detail.event.occurrences > 1) stringResource(R.string.log_times, detail.event.occurrences) else null,
            )
            Text(
                parts.joinToString(" · "),
                style = Lf.type.secondary,
                color = Lf.colors.textMuted,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Lf.type.sectionTitle,
        color = Lf.colors.textPrimary,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun InfoCard(detail: LogEventDetail, projectName: String?, zone: ZoneId, onOpenProject: (Int) -> Unit) {
    val event = detail.event
    GroupedCard {
        SectionTitle(
            stringResource(R.string.log_info),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
        )
        InfoRow(
            label = stringResource(R.string.log_info_project),
            value = projectName ?: stringResource(R.string.logs_project_fallback),
            onClick = { onOpenProject(event.projectId) },
        )
        event.type?.let { InfoRow(stringResource(R.string.log_info_error_type), it) }
        event.logFileName?.let { InfoRow(stringResource(R.string.log_info_log_file), it) }
        InfoRow(stringResource(R.string.log_info_first_seen), dateTime(event.firstSeen, zone))
        InfoRow(stringResource(R.string.log_info_last_seen), dateTime(event.lastSeen, zone))
        InfoRow(stringResource(R.string.log_info_occurrences), event.occurrences.toString())
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (onClick != null) 48.dp else 44.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 24.dp, end = if (onClick != null) 16.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, style = Lf.type.secondary, color = Lf.colors.textMuted)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text(
                value,
                style = Lf.type.secondary.copy(fontWeight = if (onClick != null) FontWeight.Bold else FontWeight.SemiBold),
                color = Lf.colors.textPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    tint = Lf.colors.icon,
                )
            }
        }
    }
}

@Composable
private fun RawCard(raw: String, expanded: Boolean, onToggle: () -> Unit, copy: (String) -> Unit) {
    // Learned from the collapsed layout; an expanded text always has the toggle.
    var overflows by remember(raw) { mutableStateOf(false) }
    GroupedCard(contentPadding = PaddingValues(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.log_raw_data), Modifier.weight(1f))
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(Lf.shapes.input)
                    .clickable(role = Role.Button) { copy(raw) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    LfIcons.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Lf.colors.textSecondary,
                )
                Text(
                    stringResource(R.string.log_raw_copy),
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textSecondary,
                )
            }
        }
        Text(
            raw,
            style = Lf.type.caption.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
            color = Lf.colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else RAW_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded && it.hasVisualOverflow) overflows = true },
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp)
                .fillMaxWidth()
                .testTag("log_raw")
                .clip(Lf.shapes.input)
                .background(Lf.colors.background)
                .padding(14.dp),
        )
        if (expanded || overflows) {
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(Lf.shapes.input)
                    .clickable(role = Role.Button, onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(if (expanded) R.string.log_raw_show_less else R.string.log_raw_show_all),
                    style = Lf.type.secondary.copy(fontWeight = FontWeight.Bold),
                    color = Lf.colors.textPrimary,
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Lf.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun EarlierCard(ids: List<Int>, onOpenLog: (Int) -> Unit) {
    GroupedCard {
        SectionTitle(
            stringResource(R.string.log_earlier),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp),
        )
        ids.forEach { id ->
            ListRow(
                title = stringResource(R.string.log_occurrence, id),
                showChevron = true,
                onClick = { onOpenLog(id) },
                modifier = Modifier.testTag(VisualQaTags.logCard(id)),
            )
        }
    }
}

@Composable
private fun ShareOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Lf.shapes.input)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Lf.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Lf.colors.textSecondary)
        }
        Text(label, style = Lf.type.bodyStrong, color = Lf.colors.textPrimary)
    }
}

/** "Oct 18, 15:39:12" in the app's locale and [zone]. */
@Composable
private fun dateTime(at: Instant, zone: ZoneId): String {
    val locale = appLocale()
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "MMMdHHmmss") }
    return at.atZone(zone).format(DateTimeFormatter.ofPattern(pattern, locale))
}
