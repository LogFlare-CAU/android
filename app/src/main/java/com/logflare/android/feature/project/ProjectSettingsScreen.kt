package com.logflare.android.feature.project

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.ui.VisualQaTags
import kotlinx.coroutines.launch

@Composable
fun ProjectSettingsScreen(
    projectId: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    vm: ProjectSettingsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRotate by remember { mutableStateOf(false) }

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    LaunchedEffect(projectId) {
        vm.initWithProject(projectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
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
                        ProjectEditorAction.CopyToken -> {
                            ui.token?.let { token ->
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("Project Token", token)),
                                    )
                                    snackbarHostState.showSnackbar("Token copied")
                                }
                            }
                        }
                        ProjectEditorAction.RotateToken -> confirmRotate = true
                        ProjectEditorAction.Delete -> confirmDelete = true
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
                        .padding(top = AppTheme.roles.layout.sectionGap + AppTheme.roles.layout.statePadding * 2),
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete project?") },
            text = { Text("This permanently removes the project and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.deleteProject { onDelete() }
                    },
                ) {
                    Text("Delete", color = AppTheme.colors.red.default)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (confirmRotate) {
        AlertDialog(
            onDismissRequest = { confirmRotate = false },
            title = { Text("Rotate token?") },
            text = {
                Text("Clients using the current token will stop working until they get the new one.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRotate = false
                        vm.rotateToken()
                    },
                ) {
                    Text("Rotate")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRotate = false }) {
                    Text("Cancel")
                }
            },
        )
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
            contentPadding = PaddingValues(vertical = AppTheme.roles.layout.contentGap),
        ) {
            projectEditorFormItems(
                uiState = uiState,
                onAction = onAction,
                onNameSave = onNameSave,
                showDelete = true,
            )
        }

        BottomActionBar(
            onDone = { onAction(ProjectEditorAction.Submit) },
            enabled = uiState.saved && !uiState.loading,
            label = "Save & Close",
        )
    }
}
