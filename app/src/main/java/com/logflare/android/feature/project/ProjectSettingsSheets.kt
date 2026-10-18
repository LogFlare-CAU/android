package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.AppTextField
import com.example.logflare.core.designsystem.v2.components.ConfirmSheet
import com.example.logflare.core.designsystem.v2.components.KeywordChipInput
import com.example.logflare.core.designsystem.v2.components.LfSheet
import com.example.logflare.core.designsystem.v2.components.MultiSelectSheet
import com.example.logflare.core.designsystem.v2.components.PrimaryButton
import com.example.logflare.core.designsystem.v2.components.SheetOption
import com.example.logflare.core.designsystem.v2.components.ThresholdLevelSelector
import com.logflare.android.R

/** The open [ProjectSettingsUiState.sheet], if any. */
@Composable
internal fun ProjectSettingsSheet(state: ProjectSettingsUiState, actions: ProjectSettingsActions) {
    val project = state.project ?: return
    when (state.sheet) {
        SettingsSheet.Name -> NameSheet(state, actions)
        SettingsSheet.AlertLevel -> AlertLevelSheet(state, actions)
        SettingsSheet.Keywords -> KeywordsSheet(state, actions)
        SettingsSheet.Members -> MultiSelectSheet(
            title = stringResource(R.string.project_settings_members),
            options = state.members.map { SheetOption(it.member.idx, it.member.username, roleLabel(it)) },
            selected = state.members.filter { it.hasAccess }.mapTo(mutableSetOf()) { it.member.idx },
            locked = state.members.filter { it.locked }.mapTo(mutableSetOf()) { it.member.idx },
            onConfirm = actions.onSaveMembers,
            onDismiss = actions.onDismissSheet,
        )
        SettingsSheet.Rotate -> ConfirmSheet(
            title = stringResource(R.string.project_create_rotate_title),
            body = stringResource(R.string.project_create_rotate_body),
            confirmLabel = stringResource(R.string.project_create_rotate),
            destructive = false,
            onConfirm = actions.onConfirmRotate,
            onDismiss = actions.onDismissSheet,
        )
        SettingsSheet.Delete -> ConfirmSheet(
            title = stringResource(R.string.project_settings_delete_title),
            body = stringResource(R.string.project_settings_delete_body, project.name),
            confirmLabel = stringResource(R.string.project_settings_delete_confirm),
            destructive = true,
            confirmEnabled = state.canDelete,
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissSheet,
        ) {
            AppTextField(
                value = state.deleteConfirmText,
                onValueChange = actions.onDeleteText,
                label = stringResource(R.string.project_settings_delete_label),
                placeholder = project.name,
            )
        }
        null -> Unit
    }
}

@Composable
private fun NameSheet(state: ProjectSettingsUiState, actions: ProjectSettingsActions) {
    LfSheet(title = stringResource(R.string.project_settings_name), onDismiss = actions.onDismissSheet) {
        AppTextField(
            value = state.nameDraft,
            onValueChange = actions.onNameDraft,
            label = stringResource(R.string.project_settings_name),
            error = (state.nameCheck as? NameCheck.Invalid)?.let { nameProblemText(it.reason) },
            helper = stringResource(R.string.project_create_name_helper),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSaveName) actions.onSaveName() }),
        )
        SaveButton(enabled = state.canSaveName, loading = state.busy, onClick = actions.onSaveName)
    }
}

@Composable
private fun AlertLevelSheet(state: ProjectSettingsUiState, actions: ProjectSettingsActions) {
    var draft by rememberSaveable { mutableStateOf(state.alertLevel) }
    LfSheet(title = stringResource(R.string.project_settings_alert_level), onDismiss = actions.onDismissSheet) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.project_settings_device_only), style = Lf.type.secondary, color = Lf.colors.textMuted)
            if (state.pushAlertsOff) {
                Text(stringResource(R.string.project_settings_push_off_body), style = Lf.type.secondary, color = Lf.colors.error)
            }
        }
        ThresholdLevelSelector(
            selected = draft,
            onSelect = { draft = it },
            summary = if (draft == LfLevel.Critical) {
                stringResource(R.string.alerts_for_critical_only)
            } else {
                stringResource(R.string.alerts_for_level_and_above, draft.name)
            },
            modifier = Modifier.padding(top = 12.dp),
        )
        SaveButton(enabled = true, loading = false, onClick = { actions.onSaveLevel(draft) })
    }
}

@Composable
private fun KeywordsSheet(state: ProjectSettingsUiState, actions: ProjectSettingsActions) {
    var draft by rememberSaveable { mutableStateOf(state.mutedKeywords) }
    LfSheet(title = stringResource(R.string.project_settings_mute_keywords), onDismiss = actions.onDismissSheet) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.project_settings_keywords_body), style = Lf.type.secondary, color = Lf.colors.textMuted)
            Text(stringResource(R.string.project_settings_device_only), style = Lf.type.secondary, color = Lf.colors.textMuted)
        }
        KeywordChipInput(
            keywords = draft,
            onAdd = { draft = draft + it },
            onRemove = { draft = draft - it },
            placeholder = stringResource(R.string.project_create_keyword_placeholder),
            onDuplicate = actions.onKeywordDuplicate,
            modifier = Modifier.padding(top = 12.dp),
        )
        SaveButton(enabled = true, loading = false, onClick = { actions.onSaveKeywords(draft) })
    }
}

@Composable
private fun SaveButton(enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    PrimaryButton(
        text = stringResource(R.string.project_settings_save),
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun roleLabel(access: MemberAccess): String = stringResource(
    when {
        access.locked -> R.string.project_settings_role_super_admin
        access.member.permission >= 80 -> R.string.project_settings_role_admin
        else -> R.string.project_settings_role_member
    },
)
