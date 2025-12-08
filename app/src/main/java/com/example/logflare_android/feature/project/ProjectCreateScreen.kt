package com.example.logflare_android.feature.project

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.ui.common.TopTitle
import kotlinx.coroutines.launch


@Composable
fun ProjectCreateScreen(
    onCreated: () -> Unit = {},
    vm: ProjectCommonViewModel = hiltViewModel()
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
    Column(modifier = Modifier.fillMaxSize()) {
        TopTitle(title = "Create Project")
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
                    TokenSection(
                        token = ui.token,
                        onCopy = {
                            ui.token?.let { token ->
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(ui.token, token)))
                                    snackbarHostState.showSnackbar("Token copied")
                                }
                            }
                        }
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
                        onCreated()
                    },
                    enabled = ui.token != null
                )
            }
        }
    }
}

@Composable
private fun TokenSection(token: String?, onCopy: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Project Token", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardGray,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = token != null) { onCopy() }
                .padding(end = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = token ?: "Token will be generated when you save",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (token != null) Color(0xFF4C4C4C) else Color(0xFF9E9E9E)
                )
                Button(
                    onClick = onCopy,
                    enabled = token != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7B7B7B),
                        contentColor = Color.White,
                        disabledContainerColor = DisabledGray,
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Copy")
                }
            }
        }
    }
}
