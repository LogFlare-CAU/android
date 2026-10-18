package com.logflare.android.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.data.store.model.LogQuery
import java.time.Instant

@Composable
fun HomeRoute(
    onOpenLog: (id: Int) -> Unit,
    onOpenProject: (id: Int) -> Unit,
    onOpenLogs: (LogQuery) -> Unit,
    onOpenProjects: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val refreshFailed = stringResource(R.string.home_refresh_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == HomeEvent.RefreshFailed) toast.show(refreshFailed) }
    }
    // Relative times ("3 min ago") are re-anchored whenever the data changes.
    val now = remember(state) { Instant.now() }
    HomeContent(
        state = state,
        now = now,
        onRefresh = viewModel::refresh,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onSearch = viewModel::onSearch,
        onMarkRead = viewModel::markRead,
        onMarkAllRead = viewModel::markAllRead,
        onOpenLog = onOpenLog,
        onOpenProject = onOpenProject,
        onOpenLogs = onOpenLogs,
        onOpenProjects = onOpenProjects,
        toast = toast,
    )
}
