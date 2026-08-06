package com.logflare.android.feature.project

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.model.ProjectDTO
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.enums.color
import com.logflare.android.ui.theme.logflareOutlinedTextFieldColors

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
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.roles.layout.screenPadding,
            vertical = AppTheme.spacing.s2,
        ),
    ) {
        Text(text = "Project Name", style = AppTheme.typography.bodyMdBold)
        Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true,
                placeholder = { Text("Project name") },
                shape = AppTheme.radius.medium,
                isError = showError,
                colors = logflareOutlinedTextFieldColors(isError = showError)
            )
            Spacer(modifier = Modifier.width(AppTheme.roles.layout.contentGap))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSave()
                },
                enabled = buttonEnabled,
                shape = AppTheme.radius.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (buttonEnabled) AppTheme.colors.primary.default else AppTheme.colors.primary.disabled,
                    contentColor = AppTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .height(50.dp)
                    .width(88.dp)
            ) {
                Text(buttonLabel)
            }
        }
        Spacer(modifier = Modifier.height(AppTheme.spacing.s6 / 4))
        if (showError) {
            Text(
                text = "Use English, Korean, and symbols only",
                color = AppTheme.colors.red.default,
                style = AppTheme.typography.bodySmMedium
            )
        }
    }
}

@Composable
fun LogLevelSection(selected: Set<String>, onToggle: (String) -> Unit, enabled: Boolean = false) {
    val options = LogLevel.getAllLabels()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.roles.layout.screenPadding,
            vertical = AppTheme.spacing.s2,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Log Level",
                style = AppTheme.typography.bodyMdMedium.copy(fontWeight = FontWeight.Medium),
                color = AppTheme.colors.primary.default
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface),
                shape = AppTheme.radius.medium,
                modifier = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = AppTheme.roles.layout.screenPadding),
                enabled = enabled
            ) {
                Text(
                    text = if (selected.isEmpty()) "Select" else selected.joinToString(", "),
                    color = AppTheme.colors.onSurface
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
                                colors = CheckboxDefaults.colors(checkedColor = AppTheme.colors.primary.default)
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
            .padding(AppTheme.roles.layout.screenPadding)
            .background(AppTheme.colors.surfaceVariant, AppTheme.radius.large)
            .padding(AppTheme.roles.layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.screenPadding)
    ) {
        if (permissions.isEmpty()) {
            Text(
                text = "No members available yet",
                color = AppTheme.colors.muted,
                style = AppTheme.typography.bodyMdMedium,
            )
        } else {
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
}

@Composable
private fun PermissionRow(state: PermissionToggleState, onToggle: (Boolean) -> Unit, enabled: Boolean = false) {
    val permission = UserPermission.fromCode(state.rolenum)
    val roleColor = permission.color(AppTheme.colors)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = state.username, color = AppTheme.colors.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(AppTheme.spacing.s1))
            Surface(
                color = roleColor,
                shape = AppTheme.radius.medium
            ) {
                Text(
                    text = state.role,
                    color = AppTheme.colors.onPrimary,
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.s2,
                        vertical = AppTheme.spacing.s1 / 2,
                    ),
                    style = AppTheme.typography.captionSmMedium
                )
            }
        }
        Switch(
            checked = state.active,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppTheme.colors.primary.default,
                checkedThumbColor = AppTheme.colors.surface,
                uncheckedTrackColor = AppTheme.colors.divider,
                uncheckedThumbColor = AppTheme.colors.surface,
                disabledCheckedTrackColor = AppTheme.colors.primary.default.copy(alpha = 0.5f),
                disabledCheckedThumbColor = AppTheme.colors.surface.copy(alpha = 0.5f)
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
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.roles.layout.screenPadding,
            vertical = AppTheme.spacing.s2,
        ),
    ) {
        Text(
            text = "Exclusion Keywords",
            style = AppTheme.typography.bodyMdBold
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text("Enter keyword") },
                singleLine = true,
                shape = AppTheme.radius.medium,
                enabled = enabled,
                isError = error != null,
                colors = logflareOutlinedTextFieldColors(isError = error != null)
            )
            Spacer(modifier = Modifier.width(AppTheme.roles.layout.contentGap))
            Button(
                onClick = onSave,
                enabled = canSave,
                shape = AppTheme.radius.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSave) AppTheme.colors.primary.default else AppTheme.colors.primary.disabled,
                    contentColor = AppTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .height(50.dp)
                    .width(88.dp)
            ) {
                Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(AppTheme.spacing.s6 / 4))
        Text(
            text = error ?: "Use English, number, and symbols only",
            color = if (error != null) AppTheme.colors.red.default else AppTheme.colors.muted,
            style = AppTheme.typography.bodySmMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordList(keywords: List<String>, onRemove: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.roles.layout.screenPadding,
            vertical = AppTheme.spacing.s2,
        ),
    ) {
        Text(
            text = "Keywords",
            style = AppTheme.typography.captionMdMedium.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        if (keywords.isEmpty()) {
            Text(text = "No keywords added", color = AppTheme.colors.muted)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap),
                verticalArrangement = Arrangement.spacedBy(AppTheme.roles.layout.contentGap),
                modifier = Modifier.fillMaxWidth()
            ) {
                keywords.forEach { keyword ->
                    Surface(
                        shape = AppTheme.radius.full,
                        color = AppTheme.colors.chip
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = AppTheme.roles.layout.contentGap,
                                vertical = AppTheme.spacing.s6 / 4,
                            )
                        ) {
                            Text(keyword, color = AppTheme.colors.onChip)
                            Spacer(modifier = Modifier.width(AppTheme.spacing.s2))
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
    label: String = "Done",
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colors.surface,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(AppTheme.roles.layout.screenPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDone,
                enabled = enabled,
                shape = AppTheme.radius.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) AppTheme.colors.primary.default else AppTheme.colors.primary.disabled,
                    contentColor = AppTheme.colors.onPrimary,
                    disabledContainerColor = AppTheme.colors.primary.disabled,
                    disabledContentColor = AppTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = label,
                    style = AppTheme.typography.bodyLgBold
                )
            }
        }
    }
}

@Composable
fun DeleteProjectButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.roles.layout.screenPadding),
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

@Composable
fun ProjectTokenSection(
    token: String?,
    onCopy: () -> Unit,
    onRotate: (() -> Unit)? = null,
    rotateEnabled: Boolean = true,
    placeholder: String = "Token will be generated when you save",
) {
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.roles.layout.screenPadding,
            vertical = AppTheme.spacing.s2,
        ),
    ) {
        Text(
            text = "Project Token",
            style = AppTheme.typography.bodyMdBold,
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
        Surface(
            shape = AppTheme.radius.medium,
            color = AppTheme.colors.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = token != null) { onCopy() },
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.roles.layout.contentGap,
                        vertical = AppTheme.spacing.s2 + AppTheme.spacing.s1 / 2,
                    )
                    .heightIn(min = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = token ?: placeholder,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.bodyMdMedium,
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
                    contentPadding = PaddingValues(horizontal = AppTheme.roles.layout.contentGap),
                ) {
                    Text("Copy")
                }
            }
        }
        if (onRotate != null) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.s2))
            Button(
                onClick = onRotate,
                enabled = rotateEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.secondary.pressed,
                    contentColor = AppTheme.colors.onPrimary,
                    disabledContainerColor = AppTheme.colors.secondary.disabled,
                    disabledContentColor = AppTheme.colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentPadding = PaddingValues(horizontal = AppTheme.roles.layout.contentGap),
            ) {
                Text("Rotate Token")
            }
        }
    }
}


@Composable
fun ProjectCard(
    project: ProjectDTO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** When null, no live/connection dot is shown (API does not expose this yet). */
    connectionHealthy: Boolean? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = AppTheme.radius.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.roles.layout.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = project.name,
                    style = AppTheme.typography.bodyMdBold,
                    color = AppTheme.colors.onSurface,
                )

                project.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = AppTheme.typography.bodySmMedium,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.padding(top = AppTheme.spacing.s1),
                    )
                }
            }

            if (connectionHealthy != null) {
                Surface(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(start = AppTheme.spacing.s2),
                    shape = CircleShape,
                    color = if (connectionHealthy) AppTheme.colors.success else AppTheme.colors.red.default,
                ) {}
            } else {
                Text(
                    text = "›",
                    style = AppTheme.typography.bodyLgBold,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.padding(start = AppTheme.spacing.s2),
                )
            }
        }
    }
}
