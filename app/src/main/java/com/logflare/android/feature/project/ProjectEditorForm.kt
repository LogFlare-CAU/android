package com.logflare.android.feature.project

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme

/**
 * Shared Create/Settings form body. Screens own chrome (scaffold, dialogs, bottom bar).
 */
fun LazyListScope.projectEditorFormItems(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
    onNameSave: () -> Unit,
    showDelete: Boolean,
) {
    if (uiState.error != null) {
        item(key = "editor_error") {
            Text(
                text = uiState.error.orEmpty(),
                color = AppTheme.colors.red.default,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    item(key = "basics_header") {
        EditorSectionHeader(
            title = "Basics",
            description = "Project identity",
            showDivider = false,
        )
    }
    item(key = "basics_name") {
        ProjectNameSection(
            name = uiState.name,
            isValid = uiState.nameValid,
            loading = uiState.loading,
            saved = uiState.saved,
            onChange = { value -> onAction(ProjectEditorAction.NameChanged(value)) },
            onSave = onNameSave,
        )
    }

    item(key = "access_header") {
        EditorSectionHeader(
            title = "Access",
            description = "Token used by clients to send logs",
        )
    }
    item(key = "access_token") {
        ProjectTokenSection(
            token = uiState.token,
            onCopy = { onAction(ProjectEditorAction.CopyToken) },
            onRotate = if (uiState.saved) {
                { onAction(ProjectEditorAction.RotateToken) }
            } else {
                null
            },
            rotateEnabled = uiState.saved && !uiState.loading,
            placeholder = if (showDelete) {
                "Rotate to issue a new project token"
            } else {
                "Token will be generated when you save"
            },
        )
    }

    item(key = "alerts_header") {
        EditorSectionHeader(
            title = "Alerts",
            description = "Exclusion keywords and levels that trigger notifications",
        )
    }
    item(key = "alerts_keywords_input") {
        KeywordSection(
            value = uiState.keywordInput,
            error = uiState.keywordError,
            onValueChange = { value -> onAction(ProjectEditorAction.KeywordChanged(value)) },
            onSave = { onAction(ProjectEditorAction.AddKeyword) },
            enabled = uiState.saved,
        )
    }
    item(key = "alerts_keywords_list") {
        KeywordList(
            keywords = uiState.keywords,
            onRemove = { keyword -> onAction(ProjectEditorAction.RemoveKeyword(keyword)) },
        )
    }
    item(key = "alerts_levels") {
        LogLevelSection(
            selected = uiState.alertLevels,
            onToggle = { level -> onAction(ProjectEditorAction.ToggleLevel(level)) },
            enabled = uiState.saved,
        )
    }

    item(key = "members_header") {
        EditorSectionHeader(
            title = "Members",
            description = "Who can access this project",
        )
    }
    item(key = "members_list") {
        PermissionsSection(
            permissions = uiState.permissions,
            onToggle = { index, _ ->
                uiState.permissions.getOrNull(index)?.username?.let { username ->
                    onAction(ProjectEditorAction.TogglePermission(username))
                }
            },
            enabled = uiState.saved,
        )
    }

    if (showDelete) {
        item(key = "danger_header") {
            EditorSectionHeader(
                title = "Danger zone",
                description = "Permanent actions — cannot be undone",
            )
        }
        item(key = "danger_delete") {
            DeleteProjectButton(
                onClick = { onAction(ProjectEditorAction.Delete) },
                enabled = !uiState.loading,
            )
        }
    }
}
