package com.example.logflare_android.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val AccentGreen = Color(0xFF61B175)
private val DisabledGray = Color(0xFFC2C2C2)
private val BorderGray = Color(0xFFBDBDBD)
private val CardGray = Color(0xFFF5F5F5)
private val ErrorRed = Color(0xFFE53935)

@Composable
fun ProjectCreateScreen(
    onCreated: () -> Unit = {},
    vm: ProjectCreateViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item { ScreenHeader("Create Project") }

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
                            clipboard.setText(AnnotatedString(token))
                            scope.launch { snackbarHostState.showSnackbar("Token copied") }
                        }
                    }
                )
            }

            item {
                KeywordSection(
                    value = ui.keywordInput,
                    error = ui.keywordError,
                    onValueChange = vm::onKeywordInputChanged,
                    onSave = vm::addKeyword
                )
            }

            item {
                KeywordList(keywords = ui.keywords, onRemove = vm::removeKeyword)
            }

            item {
                LogLevelSection(
                    selected = ui.alertLevels,
                    onToggle = vm::toggleAlertLevel
                )
            }

            item {
                PermissionsSection()
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 72.dp)
            )

            BottomActionBar(
                onDone = onCreated,
                enabled = ui.token != null
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectNameSection(
    name: String,
    isValid: Boolean,
    loading: Boolean,
    saved: Boolean,
    onChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val showError = !isValid && name.isNotEmpty()
    val buttonEnabled = isValid && !loading
    val buttonLabel = if (saved) "Edit" else "Save"

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Project Name", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true,
                placeholder = { Text("Project name") },
                shape = RoundedCornerShape(8.dp),
                isError = showError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BorderGray,
                    errorBorderColor = ErrorRed
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSave,
                enabled = buttonEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (buttonEnabled) AccentGreen else DisabledGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(50.dp)
                    .width(72.dp)
            ) {
                Text(buttonLabel)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (showError) {
            Text(
                text = "Use English, Korean, and symbols only",
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeywordSection(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val canSave = value.isNotBlank()
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Exclusion Keywords", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text("Enter keyword") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BorderGray,
                    errorBorderColor = ErrorRed
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSave,
                enabled = canSave,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSave) AccentGreen else DisabledGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(50.dp)
                    .width(72.dp)
            ) {
                Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = error ?: "Use English Only",
            color = if (error != null) ErrorRed else Color(0xFF6D6D6D),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordList(keywords: List<String>, onRemove: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Keywords", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
        Spacer(modifier = Modifier.height(8.dp))
        if (keywords.isEmpty()) {
            Text(text = "No keywords added", color = Color(0xFF8A8A8A))
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                keywords.forEach { keyword ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF0F0F0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(keyword, color = Color(0xFF101010))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { onRemove(keyword) }) {
                                Text("🗑")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogLevelSection(selected: Set<String>, onToggle: (String) -> Unit) {
    val options = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Log Level",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = AccentGreen
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (selected.isEmpty()) "Select" else selected.joinToString(", "),
                    color = Color(0xFF333333)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { level ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selected.contains(level),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                            )
                            Text(level)
                        }
                    },
                    onClick = {
                        onToggle(level)
                    }
                )
            }
        }
    }
}

private data class PermissionToggleState(
    val username: String,
    val role: String,
    val roleColor: Color,
    val activeColor: Color,
    val inactiveColor: Color,
    val active: Boolean
)

@Composable
private fun PermissionsSection() {
    val permissions = remember {
        mutableStateListOf(
            PermissionToggleState("{{username}}", "Super Admin", Color(0xFF1A1A1A), Color(0xFF2FA14F), Color(0xFFCCCCCC), true),
            PermissionToggleState("{{username}}", "Admin", Color(0xFF1A1A1A), Color(0xFF2FA14F), Color(0xFFCCCCCC), true),
            PermissionToggleState("{{username}}", "Member", Color(0xFF1A1A1A), Color(0xFF616161), Color(0xFFC2C2C2), false)
        )
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(Color(0xFFEDEDED), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Permissions", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        permissions.forEachIndexed { index, state ->
            PermissionRow(
                state = state,
                onToggle = { checked ->
                    permissions[index] = state.copy(active = checked)
                }
            )
        }
        Text(text = "TODO: Replace with API driven permissions", color = Color(0xFF6F6F6F), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PermissionRow(state: PermissionToggleState, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = state.username, color = state.roleColor, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = state.activeColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = state.role,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Switch(
            checked = state.active,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentGreen)
        )
    }
}

@Composable
private fun BottomActionBar(onDone: () -> Unit, enabled: Boolean) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDone,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) AccentGreen else DisabledGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Done")
            }
        }
    }
}

