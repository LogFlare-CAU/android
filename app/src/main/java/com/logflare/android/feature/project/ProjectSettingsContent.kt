package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.DestructiveTextButton
import com.example.logflare.core.designsystem.v2.components.EmptyState
import com.example.logflare.core.designsystem.v2.components.GroupedCard
import com.example.logflare.core.designsystem.v2.components.LfToastHost
import com.example.logflare.core.designsystem.v2.components.LfToastState
import com.example.logflare.core.designsystem.v2.components.SettingRow
import com.example.logflare.core.designsystem.v2.components.SkeletonCard
import com.example.logflare.core.designsystem.v2.components.SubpageTopBar
import com.example.logflare.core.designsystem.v2.components.TokenField
import com.example.logflare.core.designsystem.v2.components.rememberLfToastState
import com.logflare.android.R
import com.logflare.android.ui.VisualQaTags

@Composable
fun ProjectSettingsRoute(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberLfToastState()
    val clipboard = LocalClipboardManager.current
    val saved = stringResource(R.string.project_settings_saved)
    val saveFailed = stringResource(R.string.project_settings_save_failed)
    val copied = stringResource(R.string.project_create_token_copied)
    val unavailable = stringResource(R.string.project_settings_token_unavailable)
    val rotated = stringResource(R.string.project_create_token_rotated)
    val rotateFailed = stringResource(R.string.project_create_rotate_failed)
    val deleteFailed = stringResource(R.string.project_settings_delete_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProjectSettingsEvent.Saved -> toast.show(saved)
                ProjectSettingsEvent.SaveFailed -> toast.show(saveFailed)
                is ProjectSettingsEvent.CopyToken -> {
                    clipboard.setText(AnnotatedString(it.token))
                    toast.show(copied)
                }
                ProjectSettingsEvent.TokenUnavailable -> toast.show(unavailable)
                ProjectSettingsEvent.TokenRotated -> toast.show(rotated)
                ProjectSettingsEvent.RotateFailed -> toast.show(rotateFailed)
                ProjectSettingsEvent.Deleted -> onDeleted()
                ProjectSettingsEvent.DeleteFailed -> toast.show(deleteFailed)
            }
        }
    }
    val duplicate = stringResource(R.string.project_create_keyword_duplicate)
    ProjectSettingsContent(
        state = state,
        actions = ProjectSettingsActions(
            onBack = onBack,
            onOpenSheet = viewModel::openSheet,
            onDismissSheet = viewModel::dismissSheet,
            onNameDraft = viewModel::setNameDraft,
            onSaveName = viewModel::saveName,
            onSaveLevel = viewModel::saveAlertLevel,
            onSaveKeywords = viewModel::saveKeywords,
            onKeywordDuplicate = { toast.show(duplicate) },
            onSaveMembers = viewModel::saveMembers,
            onToggleReveal = viewModel::toggleReveal,
            onCopyToken = viewModel::copyToken,
            onConfirmRotate = viewModel::confirmRotate,
            onDeleteText = viewModel::setDeleteConfirmText,
            onConfirmDelete = viewModel::confirmDelete,
        ),
        toast = toast,
    )
}

/** Everything the settings screen and its sheets can do. */
class ProjectSettingsActions(
    val onBack: () -> Unit = {},
    val onOpenSheet: (SettingsSheet) -> Unit = {},
    val onDismissSheet: () -> Unit = {},
    val onNameDraft: (String) -> Unit = {},
    val onSaveName: () -> Unit = {},
    val onSaveLevel: (LfLevel) -> Unit = {},
    val onSaveKeywords: (List<String>) -> Unit = {},
    val onKeywordDuplicate: () -> Unit = {},
    val onSaveMembers: (Set<Int>) -> Unit = {},
    val onToggleReveal: () -> Unit = {},
    val onCopyToken: () -> Unit = {},
    val onConfirmRotate: () -> Unit = {},
    val onDeleteText: (String) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
)

@Composable
fun ProjectSettingsContent(
    state: ProjectSettingsUiState,
    actions: ProjectSettingsActions,
    modifier: Modifier = Modifier,
    toast: LfToastState? = null,
) {
    Box(modifier.fillMaxSize().testTag(VisualQaTags.ProjectSettings)) {
        Column(Modifier.fillMaxSize()) {
            SubpageTopBar(title = stringResource(R.string.project_settings_title), onNav = actions.onBack)
            val project = state.project
            when {
                project != null -> Settings(state, project.name, actions, Modifier.weight(1f))
                state.loading -> SkeletonCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), rows = 4)
                else -> EmptyState(
                    title = stringResource(R.string.project_detail_not_found),
                    body = null,
                    actionLabel = stringResource(R.string.project_detail_back_to_projects),
                    onAction = actions.onBack,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (toast != null) LfToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
    if (state.project != null) ProjectSettingsSheet(state, actions)
}

@Composable
private fun Settings(state: ProjectSettingsUiState, name: String, actions: ProjectSettingsActions, modifier: Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GroupedCard {
            if (state.isAdmin) {
                SettingRow(stringResource(R.string.project_settings_name), name, { actions.onOpenSheet(SettingsSheet.Name) })
            }
            SettingRow(
                stringResource(R.string.project_settings_alert_level),
                if (state.pushAlertsOff) stringResource(R.string.project_settings_push_off) else levelRange(state.alertLevel),
                { actions.onOpenSheet(SettingsSheet.AlertLevel) },
            )
            SettingRow(
                stringResource(R.string.project_settings_mute_keywords),
                state.mutedKeywords.size.takeIf { it > 0 }?.toString() ?: stringResource(R.string.project_settings_none),
                { actions.onOpenSheet(SettingsSheet.Keywords) },
            )
            if (state.isAdmin) {
                SettingRow(
                    stringResource(R.string.project_settings_members),
                    state.memberCount?.toString(),
                    { actions.onOpenSheet(SettingsSheet.Members) },
                )
            }
        }
        if (state.isAdmin) {
            GroupedCard(contentPadding = PaddingValues(20.dp)) {
                TokenField(
                    // Not fetched yet: an empty token shows as all bullets.
                    token = state.token.orEmpty(),
                    revealed = state.tokenRevealed,
                    onToggleReveal = actions.onToggleReveal,
                    onCopy = actions.onCopyToken,
                    onRotate = { actions.onOpenSheet(SettingsSheet.Rotate) },
                    description = stringResource(R.string.project_settings_token_body),
                )
            }
            GroupedCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                DestructiveTextButton(
                    text = stringResource(R.string.project_settings_delete),
                    onClick = { actions.onOpenSheet(SettingsSheet.Delete) },
                )
            }
        }
    }
}

/** "Warning and above" / "Critical only", as on the Alert level row. */
@Composable
internal fun levelRange(level: LfLevel): String =
    if (level == LfLevel.Critical) {
        stringResource(R.string.project_settings_critical_only)
    } else {
        stringResource(R.string.project_settings_level_and_above, level.name)
    }
