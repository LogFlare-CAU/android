package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.ui.components.BottomPrimaryButton

private val ColorNeutralWhite = Color(0xFFFFFFFF)
private val ColorNeutralBlack = Color(0xFF1A1A1A)
private val ColorNeutral20 = Color(0xFFEEEEEE)
private val ColorNeutral40 = Color(0xFFBDBDBD)
private val ColorNeutral60 = Color(0xFF757575)
private val ColorNeutral70 = Color(0xFF616161)
private val ColorPrimaryDefault = Color(0xFF60B176)
private val ColorDanger = Color(0xFFB12B38)

@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ColorNeutralWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            AddMemberContent(
                uiState = uiState,
                onUsernameChange = viewModel::updateUsername,
                onPermissionSelect = viewModel::selectPermission,
                onAddMember = { viewModel.addMember(onBack) },
                onClearError = viewModel::clearError
            )
        }
    }
}


@Composable
private fun AddMemberContent(
    uiState: AddMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPermissionSelect: (UserPermission) -> Unit,
    onAddMember: () -> Unit,
    onClearError: () -> Unit
) {
    var showPermissionDropdown by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Username",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorNeutral70,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter username") },
            enabled = !uiState.isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorPrimaryDefault,
                unfocusedBorderColor = ColorNeutral40
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permission Level",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorNeutral70,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PermissionDropdown(
            selectedPermission = uiState.selectedPermission,
            expanded = showPermissionDropdown,
            onExpandedChange = { showPermissionDropdown = it },
            onPermissionSelected = {
                onPermissionSelect(it)
                showPermissionDropdown = false
            },
            enabled = !uiState.isLoading
        )

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            ErrorBanner(
                message = message,
                onDismiss = onClearError
            )
        }

        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            SuccessBanner(message = message)
        }

        Spacer(modifier = Modifier.weight(1f))

        BottomPrimaryButton(
            text = if (uiState.isLoading) "Adding..." else "Add Member",
            onClick = onAddMember,
            enabled = !uiState.isLoading && uiState.username.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun PermissionDropdown(
    selectedPermission: UserPermission,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPermissionSelected: (UserPermission) -> Unit,
    enabled: Boolean
) {
    val permissions = listOf(
        UserPermission.USER,
        UserPermission.MODERATOR,
        UserPermission.SUPER_USER
    )

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (enabled) ColorNeutralWhite else ColorNeutral20,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onExpandedChange(!expanded) },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) ColorPrimaryDefault else ColorNeutral40
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPermission.label,
                    fontSize = 16.sp,
                    color = if (enabled) ColorNeutralBlack else ColorNeutral60
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (enabled) ColorNeutral70 else ColorNeutral60
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            permissions.forEach { permission ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = permission.label,
                            fontSize = 16.sp
                        )
                    },
                    onClick = { onPermissionSelected(permission) }
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() },
        color = ColorDanger.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            color = ColorDanger,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SuccessBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorPrimaryDefault.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            color = ColorPrimaryDefault,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
