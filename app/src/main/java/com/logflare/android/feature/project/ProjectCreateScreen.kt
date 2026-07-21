package com.logflare.android.feature.project

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Project Token", token)))
                                snackbarHostState.showSnackbar("Token copied")
                            }
                        }
                    }
                    ProjectEditorAction.Delete -> Unit
                }
            },
            onNameSave = {
                if (ui.saved) vm.editProject() else vm.saveProject()
            },
            modifier = Modifier.padding(paddingValues),
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
                TokenSection(
                    token = uiState.token,
                    onCopy = { onAction(ProjectEditorAction.CopyToken) },
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
        }

        BottomActionBar(
            onDone = { onAction(ProjectEditorAction.Submit) },
            enabled = uiState.token != null,
        )
    }
}

@Composable
private fun TokenSection(token: String?, onCopy: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Project Token",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AppTheme.colors.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = token != null) { onCopy() },
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .heightIn(min = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = token ?: "Token will be generated when you save",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (token != null) AppTheme.colors.onSurface else AppTheme.colors.muted,
                )
                Button(
                    onClick = onCopy,
                    enabled = token != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.secondary.pressed,
                        contentColor = AppTheme.colors.onPrimary,
                        disabledContainerColor = AppTheme.colors.secondary.disabled,
                        disabledContentColor = AppTheme.colors.onPrimary,
                    ),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text("Copy")
                }
            }
        }
    }
}
