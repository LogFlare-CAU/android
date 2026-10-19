package com.logflare.android.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.Avatar
import com.example.logflare.core.designsystem.v2.components.CountBadge
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.HeaderIconButton
import com.example.logflare.core.designsystem.v2.components.HomeHeader
import com.example.logflare.core.designsystem.v2.components.LevelIcon
import com.example.logflare.core.designsystem.v2.components.LfPullToRefresh
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.ListRow
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.StatItem
import com.example.logflare.core.designsystem.v2.components.StatStrip
import com.example.logflare.core.designsystem.v2.components.StatTone
import com.logflare.android.R
import com.logflare.android.data.store.model.LogEvent
import com.logflare.android.data.store.model.LogQuery
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.format.logTitle
import com.logflare.android.ui.format.relativeTime
import java.time.Instant

/**
 * Home: summary strip, recent errors and the most recently failing projects, with the search
 * and notification sheets. [now] anchors the relative times ("3 min ago").
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    now: Instant,
    onRefresh: () -> Unit,
    onOpenSheet: (HomeSheet) -> Unit,
    onCloseSheet: () -> Unit,
    onSearch: (String) -> Unit,
    onMarkRead: (errorId: Int) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenLog: (id: Int) -> Unit,
    onOpenProject: (id: Int) -> Unit,
    onOpenLogs: (LogQuery) -> Unit,
    onOpenProjects: () -> Unit,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.Home)) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(logo = painterResource(R.drawable.logo)) {
                HeaderIconButton(
                    Icons.Outlined.Search,
                    stringResource(R.string.home_search),
                    onClick = { onOpenSheet(HomeSheet.Search) },
                )
                HeaderIconButton(
                    Icons.Outlined.Notifications,
                    stringResource(if (state.unreadAlerts > 0) R.string.home_notifications_unread else R.string.home_notifications),
                    onClick = { onOpenSheet(HomeSheet.Notifications) },
                    dot = state.unreadAlerts > 0,
                )
            }
            LfPullToRefresh(refreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        state.initialLoad -> {
                            item { Stats(state, onOpenLogs, onOpenProjects) }
                            item { SkeletonCard(rows = HOME_RECENT_COUNT) }
                            item { SkeletonCard(rows = HOME_PROJECT_COUNT) }
                        }
                        state.loadFailed -> item {
                            EmptyState(
                                title = stringResource(R.string.home_load_failed_title),
                                body = stringResource(R.string.home_load_failed_body),
                                actionLabel = stringResource(R.string.home_retry),
                                onAction = onRefresh,
                            )
                        }
                        else -> {
                            item { Stats(state, onOpenLogs, onOpenProjects) }
                            if (state.projects.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = stringResource(R.string.home_empty_title),
                                        body = stringResource(R.string.home_empty_body),
                                        actionLabel = stringResource(R.string.home_empty_action),
                                        onAction = onOpenProjects,
                                    )
                                }
                            } else {
                                item { RecentErrors(state, now, onOpenLog, onOpenLogs) }
                                item { Projects(state.projects, now, onOpenProject, onOpenProjects) }
                            }
                        }
                    }
                }
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter))
    }

    when (state.sheet) {
        HomeSheet.Search -> HomeSearchSheet(
            query = state.search,
            results = state.searchResults,
            projectNames = state.projectNames,
            now = now,
            onQuery = onSearch,
            onOpenLog = onOpenLog,
            onOpenProject = onOpenProject,
            onDismiss = onCloseSheet,
        )
        HomeSheet.Notifications -> NotificationsSheet(
            alerts = state.alerts,
            projectNames = state.projectNames,
            now = now,
            onMarkRead = onMarkRead,
            onMarkAllRead = onMarkAllRead,
            onOpenLog = onOpenLog,
            onDismiss = onCloseSheet,
        )
        null -> Unit
    }
}

@Composable
private fun Stats(state: HomeUiState, onOpenLogs: (LogQuery) -> Unit, onOpenProjects: () -> Unit) {
    val s = state.summary
    fun count(n: Int?) = n?.toString() ?: "–"
    StatStrip(
        listOf(
            StatItem(
                count(s?.errors24h),
                stringResource(R.string.home_stat_errors),
                StatTone.Error,
                onClick = { onOpenLogs(LogQuery(minLevel = LfLevel.Error)) },
            ),
            StatItem(
                count(s?.warnings24h),
                stringResource(R.string.home_stat_warnings),
                StatTone.Warning,
                onClick = { onOpenLogs(LogQuery(minLevel = LfLevel.Warning)) },
            ),
            StatItem(count(s?.projectCount), stringResource(R.string.home_stat_projects), onClick = onOpenProjects),
        ),
    )
}

@Composable
private fun RecentErrors(
    state: HomeUiState,
    now: Instant,
    onOpenLog: (Int) -> Unit,
    onOpenLogs: (LogQuery) -> Unit,
) {
    GroupedCard(contentPadding = PaddingValues(vertical = 8.dp)) {
        // Recent errors are ERROR and up, so "See all" opens Logs with that filter.
        CardHeader(stringResource(R.string.home_recent_errors)) { onOpenLogs(LogQuery(minLevel = LfLevel.Error)) }
        if (state.recent.isEmpty()) {
            Text(
                stringResource(R.string.home_recent_empty),
                style = Lf.type.body,
                color = Lf.colors.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        state.recent.forEach { log -> RecentRow(log, state.projectNames[log.projectId], now) { onOpenLog(log.id) } }
    }
}

@Composable
private fun RecentRow(log: LogEvent, projectName: String?, now: Instant, onClick: () -> Unit) {
    val ago = relativeTime(log.lastSeen, now)
    ListRow(
        title = logTitle(log.message),
        // Time first: a long project name ellipsizes instead of hiding it.
        subtitle = if (projectName != null) stringResource(R.string.home_dot_join, ago, projectName) else ago,
        leading = { LevelIcon(log.level) },
        onClick = onClick,
    )
}

@Composable
private fun Projects(projects: List<ProjectItem>, now: Instant, onOpenProject: (Int) -> Unit, onOpenProjects: () -> Unit) {
    GroupedCard(contentPadding = PaddingValues(vertical = 8.dp)) {
        CardHeader(stringResource(R.string.home_projects), onOpenProjects)
        projects.forEach { p ->
            ListRow(
                title = p.name,
                subtitle = p.lastErrorAt?.let { stringResource(R.string.home_last_error, relativeTime(it, now)) }
                    ?: stringResource(R.string.home_no_errors),
                leading = { Avatar(p.name) },
                trailing = if (p.errorCount24h > 0) {
                    { CountBadge(p.errorCount24h, StatTone.Error) }
                } else {
                    null
                },
                showChevron = true,
                onClick = { onOpenProject(p.id) },
            )
        }
    }
}

@Composable
private fun CardHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = Lf.type.cardTitle,
            color = Lf.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            stringResource(R.string.home_see_all),
            style = Lf.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = Lf.colors.textMuted,
            modifier = Modifier
                .clip(Lf.shapes.input)
                .clickable(role = Role.Button, onClick = onSeeAll)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
