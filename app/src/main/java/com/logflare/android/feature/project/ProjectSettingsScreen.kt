package com.logflare.android.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.logflare.android.ui.VisualQaTags

@Composable
fun ProjectSettingsScreen(
    projectId: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    vm: ProjectSettingsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    LaunchedEffect(projectId) {
        vm.initWithProject(projectId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            ProjectSettingsScreenContent(
                uiState = ui,
                onAction = { action ->
                    when (action) {
                        is ProjectEditorAction.NameChanged -> vm.onNameChanged(action.value)
                        is ProjectEditorAction.KeywordChanged -> vm.onKeywordInputChanged(action.value)
                        ProjectEditorAction.AddKeyword -> vm.addKeyword()
                        is ProjectEditorAction.RemoveKeyword -> vm.removeKeyword(action.value)
                        is ProjectEditorAction.ToggleLevel -> vm.toggleAlertLevel(action.value)
                        is ProjectEditorAction.TogglePermission -> {
                            val index = ui.permissions.indexOfFirst { it.username == action.username }
                            if (index >= 0) {
                                vm.onPermissionToggle(index, !ui.permissions[index].active)
                            }
                        }
                        ProjectEditorAction.Submit -> {
                            vm.savePerms()
                            onDelete()
                        }
                        ProjectEditorAction.CopyToken -> Unit
                        ProjectEditorAction.Delete -> vm.deleteProject { onDelete() }
                    }
                },
                onNameSave = {
                    if (ui.saved) vm.editProject() else vm.saveProject()
                },
            )

            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp),
                )
            }
        }
    }
}

@Composable
fun ProjectSettingsScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
) {
    ProjectSettingsScreenContent(
        uiState = uiState,
        onAction = onAction,
        onNameSave = {},
    )
}

@Composable
private fun ProjectSettingsScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
    onNameSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.ProjectSettings),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item {
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = AppTheme.colors.red.default,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                ProjectNameSection(
                    name = uiState.name,
                    isValid = uiState.nameValid,
                    loading = uiState.loading,
                    saved = uiState.saved,
                    onChange = { value -> onAction(ProjectEditorAction.NameChanged(value)) },
                    onSave = onNameSave,
                )
            }

            item {
                KeywordSection(
                    value = uiState.keywordInput,
                    error = uiState.keywordError,
                    onValueChange = { value -> onAction(ProjectEditorAction.KeywordChanged(value)) },
                    onSave = { onAction(ProjectEditorAction.AddKeyword) },
                    enabled = uiState.saved,
                )
            }

            item {
                KeywordList(
                    keywords = uiState.keywords,
                    onRemove = { keyword -> onAction(ProjectEditorAction.RemoveKeyword(keyword)) },
                )
            }

            item {
                LogLevelSection(
                    selected = uiState.alertLevels,
                    onToggle = { level -> onAction(ProjectEditorAction.ToggleLevel(level)) },
                    enabled = uiState.saved,
                )
            }

            item {
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

            item {
                DeleteProject(
                    onClick = { onAction(ProjectEditorAction.Delete) },
                    enabled = !uiState.loading,
                )
            }
        }

        BottomActionBar(
            onDone = { onAction(ProjectEditorAction.Submit) },
            enabled = uiState.token != null,
        )
    }
}

@Composable
private fun DeleteProject(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        LogFlareButton(
            text = "Delete Project",
            onClick = onClick,
            type = ButtonType.Text,
            variant = ButtonVariant.Secondary,
            enabled = enabled,
        )
    }
}
