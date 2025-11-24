package com.example.logflare_android.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

/**
 * Project creation UI shell matching provided wireframe (no real functionality yet).
 * Sections:
 *  - Header (title)
 *  - Project Name input
 *  - Project Token placeholder (read-only)
 *  - Alert Level stub
 *  - Exclusion Keywords input placeholder
 *  - Permissions list (static placeholders)
 *  - Bottom action bar (Done button)
 */
@Composable
fun ProjectCreateScreen(
    onCreated: () -> Unit = {},
    vm: ProjectCreateViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // state handled by ViewModel
    // Static placeholder permissions & roles
    val permissionRows = remember {
        listOf(
            PermissionDisplay("{{username}}", "Super Admin", Color(0xFF2FA14F)),
            PermissionDisplay("{{username}}", "Admin", Color(0xFF2FA14F)),
            PermissionDisplay("{{username}}", "Admin", Color(0xFF2FA14F)),
            PermissionDisplay("{{username}}", "Member", Color(0xFF616161)),
            PermissionDisplay("{{username}}", "Member", Color(0xFF616161))
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp) // leave space for bottom bar
        ) {
            item { ScreenHeader(title = "Create Project") }
            if (ui.error != null) {
                item {
                    Text(
                        text = ui.error ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item { SectionLabel(text = "Project Name") }
            item { NameInputField(value = ui.name, onChange = { vm.onNameChanged(it) }, isError = !ui.nameValid && ui.name.isNotEmpty(), errorText = if (!ui.nameValid && ui.name.isNotEmpty()) "Invalid name" else null) }
            item { SectionLabel(text = "Project Token") }
            item { TokenPlaceholder(token = ui.token, onCopy = {
                ui.token?.let { t ->
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(t))
                    scope.launch { snackbarHostState.showSnackbar("Token copied") }
                }
            }) }
            item { SectionLabel(text = "Alert Level") }
            item {
                AlertLevelSelector(
                    selected = ui.alertLevels,
                    onToggle = { level -> vm.toggleAlertLevel(level) }
                )
            }
            item { SectionLabel(text = "Exclusion Keywords") }
            item {
                ExclusionKeywordField(value = ui.keywordInput, onChange = { vm.onKeywordInputChanged(it) }, onAdd = { vm.addKeyword() }, keywordError = ui.keywordError)
            }
            item { KeywordChips(keywords = ui.keywords, onRemove = vm::removeKeyword) }
            item { SectionLabel(text = "Permissions") }
            item { PermissionsList(permissions = permissionRows) }
        }
        // Snackbar host + show VM snackbars
        Box(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(ui.snackbar) {
                val msg = ui.snackbar
                if (msg != null) {
                    snackbarHostState.showSnackbar(msg)
                    vm.clearSnackbar()
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        }

        BottomActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onSave = { if (!ui.saved) vm.saveProject() else vm.editProject() },
            onDone = {
                onCreated()
            },
            saveEnabled = ui.nameValid && !ui.loading,
            doneEnabled = ui.token != null
        )
    }
}

// --- Composables ---

@Composable
private fun ScreenHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp)
    )
}

@Composable
private fun NameInputField(
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Enter a project name", color = Color(0xFF616161)) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            isError = isError
        )
        if (isError && errorText != null) {
            Text(
                text = errorText,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun TokenPlaceholder(token: String?, onCopy: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        color = Color(0xFFF5F5F5),
        border = null
    ) {
        Row(modifier = Modifier
            .height(50.dp)
            .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = token ?: "Token will be generated automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF616161),
                modifier = Modifier.weight(1f)
            )
            if (token != null) {
                TextButton(onClick = onCopy) {
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun AlertLevelStub() {
    // Simple stub card with a placeholder label
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .width(140.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Log Level",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun AlertLevelSelector(selected: Set<String>, onToggle: (String) -> Unit) {
    val levels = listOf("trace", "debug", "info", "warn", "error", "fatal")
    Column(modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.forEach { level ->
                FilterChip(
                    selected = selected.contains(level),
                    onClick = { onToggle(level) },
                    label = { Text(level) }
                )
            }
        }
    }
}

@Composable
private fun ExclusionKeywordField(value: String, onChange: (String) -> Unit, onAdd: () -> Unit, keywordError: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Enter a Keyword", color = Color(0xFF616161)) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        )
        Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            Button(onClick = onAdd) { Text("Add") }
            if (keywordError != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = keywordError, color = Color.Red)
            }
        }
    }
}

@Composable
private fun KeywordChips(keywords: List<String>, onRemove: (String) -> Unit) {
    if (keywords.isEmpty()) return
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keywords.forEach { k ->
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE0E0E0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    TextButton(onClick = { onRemove(k) }) { Text("x") }
                }
            }
        }
    }
}

private data class PermissionDisplay(val username: String, val role: String, val roleColor: Color)

@Composable
private fun PermissionsList(permissions: List<PermissionDisplay>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEEEEE),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            permissions.forEach { p -> PermissionRow(p) }
        }
    }
}

@Composable
private fun PermissionRow(p: PermissionDisplay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left placeholder toggle pill
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(36.dp)
                .background(Color(0xFF62B175), RoundedCornerShape(50))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = p.username,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = p.roleColor
            ) {
                Text(
                    text = p.role,
                    color = Color(0xFFF9F9F9),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
    onDone: () -> Unit,
    saveEnabled: Boolean,
    doneEnabled: Boolean
) {
    Surface(
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = saveEnabled) {
                Text("Save")
            }
            Button(onClick = onDone, enabled = doneEnabled, modifier = Modifier.weight(1f)) {
                Text("Done")
            }
        }
    }
}
