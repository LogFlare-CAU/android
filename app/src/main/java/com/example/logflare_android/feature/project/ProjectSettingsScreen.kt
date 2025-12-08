package com.example.logflare_android.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.ui.common.TopTitle


@Composable
fun ProjectSettingsScreen(
    projectId: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    vm: ProjectCommonViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()

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
        TopTitle(title = "Project Settings", onBack = onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    if (ui.error != null) {
                        Text(
                            text = ui.error ?: "",
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item {
                    ProjectNameSection(
                        name = ui.name,
                        isValid = ui.nameValid,
                        loading = ui.loading,
                        saved = ui.saved,
                        onChange = vm::onNameChanged,
                        onSave = { if (ui.saved) vm.editProject() else vm.saveProject() }
                    )
                }


                item {
                    KeywordSection(
                        value = ui.keywordInput,
                        error = ui.keywordError,
                        onValueChange = vm::onKeywordInputChanged,
                        onSave = vm::addKeyword,
                        enabled = ui.saved
                    )
                }

                item {
                    KeywordList(keywords = ui.keywords, onRemove = vm::removeKeyword)
                }

                item {
                    LogLevelSection(
                        selected = ui.alertLevels,
                        onToggle = vm::toggleAlertLevel,
                        enabled = ui.saved
                    )
                }

                item {
                    PermissionsSection(ui.permissions, onToggle = vm::onPermissionToggle, enabled = ui.saved)
                }

                item {
                    DeleteProject {
                        vm.deleteProject()
                        onDelete()
                    }
                }

            }

            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp)
                )

                BottomActionBar(
                    onDone = {
                        vm.savePerms()
                        onDelete()
                    },
                    enabled = ui.token != null
                )
            }
        }
    }
}

@Composable
private fun DeleteProject(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed,
                contentColor = Color.White
            ),
        ) {
            Text("Delete project")
        }
    }

}