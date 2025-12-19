package com.example.logflare_android.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.Black
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare_android.enums.LogLevel

val AccentGreen = Color(0xFF61B175)
val DisabledGray = Color(0xFFC2C2C2)
val BorderGray = Color(0xFFBDBDBD)
val CardGray = Color(0xFFF5F5F5)
val ErrorRed = Color(0xFFE53935)


@Composable
fun ProjectNameSection(
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
                    errorBorderColor = ErrorRed,
                    cursorColor = Black
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSave()
                },
                enabled = buttonEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (buttonEnabled) AccentGreen else DisabledGray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(50.dp)
                    .width(88.dp)
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
fun LogLevelSection(selected: Set<String>, onToggle: (String) -> Unit, enabled: Boolean = false) {
    val options = LogLevel.getAllLabels()
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                enabled = enabled
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

@Composable
fun PermissionsSection(
    permissions: List<PermissionToggleState>,
    onToggle: (index: Int, checked: Boolean) -> Unit,
    enabled: Boolean = false
) {
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
                    onToggle(index, checked)
                },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun PermissionRow(state: PermissionToggleState, onToggle: (Boolean) -> Unit, enabled: Boolean = false) {
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
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentGreen,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedThumbColor = Color.White,
                disabledCheckedTrackColor = AccentGreen.copy(alpha = 0.5f),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordSection(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean = false,
) {
    val canSave = value.isNotBlank()
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Exclusion Keywords",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
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
                enabled = enabled,
                isError = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = BorderGray,
                    errorBorderColor = ErrorRed,
                    cursorColor = Black
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
                    .width(88.dp)
            ) {
                Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = error ?: "Use English, number, and symbols only",
            color = if (error != null) ErrorRed else Color(0xFF6D6D6D),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordList(keywords: List<String>, onRemove: (String) -> Unit) {
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
fun BottomActionBar(
    onDone: () -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDone,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) AccentGreen else DisabledGray,
                    contentColor = Color.White,
                    disabledContainerColor = DisabledGray,
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}


@Composable
fun ProjectCard(
    project: ProjectDTO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnected: Boolean = true // TODO: Determine connection status from actual data
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )

                project.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Connection status indicator (green/red dot)
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .padding(start = 8.dp),
                shape = CircleShape,
                color = if (isConnected) Color.Green else Color.Red
            ) {}
        }
    }
}
