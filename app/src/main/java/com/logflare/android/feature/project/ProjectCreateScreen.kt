package com.logflare.android.feature.project

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.logflare.android.ui.VisualQaTags
import kotlinx.coroutines.launch

@Composable
fun ProjectCreateScreen(
    onCreated: () -> Unit = {},
    vm: ProjectCreateViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val clipboard: Clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmRotate by remember { mutableStateOf(false) }

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        contentColor = AppTheme.colors.onBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppTheme.colors.onSurface,
                    contentColor = AppTheme.colors.onPrimary,
                    shape = RoundedCornerShape(8.dp),
                )
            }
        },
    ) { paddingValues ->
        ProjectCreateScreenContent(
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
                        onCreated()
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
                    ProjectEditorAction.Delete -> Unit
                }
            },
            onNameSave = {
                if (ui.saved) vm.editProject() else vm.saveProject()
            },
            modifier = Modifier.padding(paddingValues),
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
fun ProjectCreateScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
) {
    ProjectCreateScreenContent(
        uiState = uiState,
        onAction = onAction,
        onNameSave = {},
    )
}

@Composable
private fun ProjectCreateScreenContent(
    uiState: ProjectCreateUiState,
    onAction: (ProjectEditorAction) -> Unit,
    onNameSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.ProjectCreate),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            projectEditorFormItems(
                uiState = uiState,
                onAction = onAction,
                onNameSave = onNameSave,
                showDelete = false,
            )
        }

        BottomActionBar(
            onDone = { onAction(ProjectEditorAction.Submit) },
            enabled = uiState.token != null,
            label = "Done",
        )
    }
}
