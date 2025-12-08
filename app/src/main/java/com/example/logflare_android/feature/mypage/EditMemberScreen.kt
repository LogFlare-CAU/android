package com.example.logflare_android.feature.mypage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonSize
import com.example.logflare.core.designsystem.components.button.ButtonType
import com.example.logflare.core.designsystem.components.button.ButtonVariant
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.dropdown.DropdownSize
import com.example.logflare.core.designsystem.components.dropdown.LogFlareDropdown
import com.example.logflare.core.designsystem.components.feedback.LogFlareSnackbar
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.example.logflare_android.ui.component.common.LogFlareActionTextField
import com.example.logflare_android.ui.component.common.LogFlareActionTextFieldHelperTone
import com.example.logflare_android.ui.component.common.LogFlareActionTextFieldState
import com.example.logflare_android.ui.component.common.MemberFieldStatus
import com.example.logflare_android.ui.component.common.toActionTextFieldState
import com.example.logflare_android.enums.UserPermission
import kotlinx.coroutines.delay

@Composable
fun EditMemberScreen(
    onBack: () -> Unit,
    onMemberDeleted: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    val usernameState = uiState.usernameValidation.status.toActionTextFieldState()
    val passwordState = uiState.passwordValidation.status.toActionTextFieldState()
    val usernameChanged = uiState.username != uiState.originalUsername &&
        uiState.usernameValidation.status == MemberFieldStatus.Valid
    val passwordReady = uiState.passwordValidation.status == MemberFieldStatus.Valid
    val roleChanged = uiState.selectedPermission != uiState.originalPermission
    val canSubmit = (usernameChanged || passwordReady || roleChanged) && !uiState.isLoading

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage != null) {
            delay(2500)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.neutral.white,
        topBar = {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "Edit Member",
                onBack = onBack
            )
        },
        bottomBar = {
            EditMemberBottomBar(
                snackbarMessage = uiState.snackbarMessage,
                isLoading = uiState.isLoading,
                canSubmit = canSubmit,
                onDismissSnackbar = viewModel::dismissSnackbar,
                onDeleteClick = viewModel::showDeleteDialog,
                onSaveClick = { viewModel.saveChanges() },
                disabled = uiState.disabled
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.username.isBlank() -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> EditMemberContent(
                uiState = uiState,
                usernameFieldState = usernameState,
                passwordFieldState = passwordState,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onRequestUsernameValidation = viewModel::retryUsernameValidation,
                onRequestPasswordValidation = viewModel::retryPasswordValidation,
                onPermissionSelect = viewModel::selectPermission,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }
    }

    if (uiState.showDeleteDialog) {
        DeleteMemberDialog(
            username = uiState.username,
            onConfirm = { viewModel.deleteMember(onMemberDeleted) },
            onDismiss = viewModel::hideDeleteDialog
        )
    }
}

@Composable
private fun EditMemberContent(
    uiState: EditMemberUiState,
    usernameFieldState: LogFlareActionTextFieldState,
    passwordFieldState: LogFlareActionTextFieldState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRequestUsernameValidation: () -> Unit,
    onRequestPasswordValidation: () -> Unit,
    onPermissionSelect: (UserPermission) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.s4)
            .padding(bottom = AppTheme.spacing.s6)
    ) {
        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))

        val canSaveUsername = uiState.usernameValidation.status == MemberFieldStatus.Valid && !uiState.isLoading

        LogFlareActionTextField(
            label = "Member name",
            value = uiState.username,
            onValueChange = onUsernameChange,
            placeholder = "Enter member name",
            state = usernameFieldState,
            helperText = uiState.usernameValidation.helperText,
            helperTone = if (usernameFieldState == LogFlareActionTextFieldState.Error) {
                LogFlareActionTextFieldHelperTone.Error
            } else {
                LogFlareActionTextFieldHelperTone.Info
            },
            actionText = if (uiState.usernameValidation.status == MemberFieldStatus.Completed) "Edit" else "Save",
            actionEnabled = canSaveUsername,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
            onActionClick = onRequestUsernameValidation,
            modifier = Modifier.fillMaxWidth(),
            disabled = uiState.disabled
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))

        val canSavePassword = uiState.passwordValidation.status == MemberFieldStatus.Valid && !uiState.isLoading

        LogFlareActionTextField(
            label = "Password",
            value = uiState.newPassword,
            onValueChange = onPasswordChange,
            placeholder = "Enter new password",
            state = passwordFieldState,
            helperText = uiState.passwordValidation.helperText,
            helperTone = if (passwordFieldState == LogFlareActionTextFieldState.Error) {
                LogFlareActionTextFieldHelperTone.Error
            } else {
                LogFlareActionTextFieldHelperTone.Info
            },
            actionText = if (uiState.passwordValidation.status == MemberFieldStatus.Completed) "Edit" else "Save",
            actionEnabled = canSavePassword,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password
            ),
            visualTransformation = PasswordVisualTransformation(),
            onActionClick = onRequestPasswordValidation,
            modifier = Modifier.fillMaxWidth(),
            disabled = uiState.disabled
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Role",
                style = AppTheme.typography.bodySmBold,
                color = AppTheme.colors.neutral.black
            )

            LogFlareDropdown(
                items = UserPermission.entries.filter { it != UserPermission.SUPER_USER },
                selectedItem = uiState.selectedPermission,
                onItemSelected = onPermissionSelect,
                itemLabelMapper = { it.label },
                size = DropdownSize.Large,
                modifier = Modifier.width(140.dp),
                disabled = uiState.disabled
            )
        }
    }
}

@Composable
private fun EditMemberBottomBar(
    snackbarMessage: String?,
    isLoading: Boolean,
    canSubmit: Boolean,
    onDismissSnackbar: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    disabled: Boolean = false
) {
    Surface(
        color = AppTheme.colors.neutral.white,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppTheme.spacing.s4)
                .padding(vertical = AppTheme.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3)
        ) {
            if (snackbarMessage != null) {
                LogFlareSnackbar(
                    message = snackbarMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissSnackbar() }
                )
            } else {
                LogFlareButton(
                    text = "Delete Member",
                    onClick = onDeleteClick,
                    variant = ButtonVariant.Secondary,
                    type = ButtonType.Text,
                    enabled = !isLoading && !disabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LogFlareButton(
                text = if (isLoading) "Saving..." else "Done",
                onClick = onSaveClick,
                enabled = canSubmit && !disabled,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeleteMemberDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Member",
                style = AppTheme.typography.bodyMdBold,
                color = AppTheme.colors.neutral.black
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$username\"? This action cannot be undone.",
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70
            )
        },
        confirmButton = {
            LogFlareButton(
                text = "Delete",
                onClick = onConfirm,
                variant = ButtonVariant.Secondary,
                type = ButtonType.Text
            )
        },
        dismissButton = {
            LogFlareButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ButtonVariant.Primary,
                type = ButtonType.Text
            )
        },
        containerColor = AppTheme.colors.neutral.white,
        tonalElevation = 2.dp
    )
}
