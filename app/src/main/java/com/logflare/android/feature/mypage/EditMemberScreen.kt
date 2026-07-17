package com.logflare.android.feature.mypage

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.logflare.android.enums.UserPermission
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.component.common.LogFlareActionTextField
import com.logflare.android.ui.component.common.LogFlareActionTextFieldHelperTone
import com.logflare.android.ui.component.common.LogFlareActionTextFieldState
import com.logflare.android.ui.component.common.MemberFieldStatus
import com.logflare.android.ui.component.common.toActionTextFieldState
import kotlinx.coroutines.delay

@Composable
fun EditMemberScreen(
    onBack: () -> Unit,
    onMemberDeleted: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: EditMemberViewModel = hiltViewModel(),
) {
    val uiState by viewModel.ui.collectAsState()

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage != null) {
            delay(2500)
            viewModel.dismissSnackbar()
        }
    }

    EditMemberScreenContent(
        uiState = uiState,
        onBack = onBack,
        onUsernameChange = viewModel::updateUsername,
        onPasswordChange = viewModel::updatePassword,
        onValidateUsername = viewModel::retryUsernameValidation,
        onValidatePassword = viewModel::retryPasswordValidation,
        onPermissionChange = viewModel::selectPermission,
        onSave = viewModel::saveChanges,
        onDeleteRequest = viewModel::showDeleteDialog,
        onDeleteConfirm = { viewModel.deleteMember(onMemberDeleted) },
        onDeleteDismiss = viewModel::hideDeleteDialog,
        onDismissMessage = viewModel::dismissSnackbar,
        modifier = modifier,
    )
}

@Composable
fun EditMemberScreenContent(
    uiState: EditMemberUiState,
    onBack: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onValidateUsername: () -> Unit,
    onValidatePassword: () -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val usernameState = uiState.usernameValidation.status.toActionTextFieldState()
    val passwordState = uiState.passwordValidation.status.toActionTextFieldState()
    val usernameChanged = uiState.username != uiState.originalUsername &&
        uiState.usernameValidation.status == MemberFieldStatus.Valid
    val passwordReady = uiState.passwordValidation.status == MemberFieldStatus.Valid
    val roleChanged = uiState.selectedPermission != uiState.originalPermission
    val canSubmit = (usernameChanged || passwordReady || roleChanged) && !uiState.isLoading

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.EditMember),
        containerColor = AppTheme.colors.surface,
        topBar = {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "Edit Member",
                onBack = onBack,
                backTestTag = VisualQaTags.NavigateBack,
            )
        },
        bottomBar = {
            if (!(uiState.isLoading && uiState.username.isBlank())) {
                EditMemberBottomBar(
                    snackbarMessage = uiState.snackbarMessage,
                    snackbarIsError = uiState.snackbarIsError,
                    isLoading = uiState.isLoading,
                    canSubmit = canSubmit,
                    onDismissSnackbar = onDismissMessage,
                    onDeleteClick = onDeleteRequest,
                    onSaveClick = onSave,
                    disabled = uiState.disabled,
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.username.isBlank() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .testTag(VisualQaTags.Loading),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                EditMemberForm(
                    uiState = uiState,
                    usernameFieldState = usernameState,
                    passwordFieldState = passwordState,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onValidateUsername = onValidateUsername,
                    onValidatePassword = onValidatePassword,
                    onPermissionChange = onPermissionChange,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
        }
    }

    if (uiState.showDeleteDialog) {
        DeleteMemberDialog(
            username = uiState.username,
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
        )
    }
}

@Composable
private fun EditMemberForm(
    uiState: EditMemberUiState,
    usernameFieldState: LogFlareActionTextFieldState,
    passwordFieldState: LogFlareActionTextFieldState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onValidateUsername: () -> Unit,
    onValidatePassword: () -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.spacing.s4)
            .padding(bottom = AppTheme.spacing.s6),
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
                autoCorrectEnabled = false,
            ),
            onActionClick = onValidateUsername,
            modifier = Modifier.fillMaxWidth(),
            disabled = uiState.disabled,
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
                keyboardType = KeyboardType.Password,
            ),
            visualTransformation = PasswordVisualTransformation(),
            onActionClick = onValidatePassword,
            modifier = Modifier.fillMaxWidth(),
            disabled = uiState.disabled,
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Role",
                style = AppTheme.typography.bodySmBold,
                color = AppTheme.colors.onSurface,
            )

            LogFlareDropdown(
                items = UserPermission.entries.filter { it != UserPermission.SUPER_USER },
                selectedItem = uiState.selectedPermission,
                onItemSelected = onPermissionChange,
                itemLabelMapper = { it.label },
                size = DropdownSize.Large,
                modifier = Modifier.width(140.dp),
                disabled = uiState.disabled,
            )
        }
    }
}

@Composable
private fun EditMemberBottomBar(
    snackbarMessage: String?,
    snackbarIsError: Boolean,
    isLoading: Boolean,
    canSubmit: Boolean,
    onDismissSnackbar: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    disabled: Boolean = false,
) {
    Surface(
        color = AppTheme.colors.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppTheme.spacing.s4)
                .padding(vertical = AppTheme.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s3),
        ) {
            if (snackbarMessage != null) {
                LogFlareSnackbar(
                    message = snackbarMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissSnackbar() }
                        .then(
                            if (snackbarIsError) Modifier.testTag(VisualQaTags.Error)
                            else Modifier,
                        ),
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DeleteMemberDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Member",
                style = AppTheme.typography.bodyMdBold,
                color = AppTheme.colors.onSurface,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$username\"? This action cannot be undone.",
                style = AppTheme.typography.bodySmMedium,
                color = AppTheme.colors.neutral.s70,
            )
        },
        confirmButton = {
            LogFlareButton(
                text = "Delete",
                onClick = onConfirm,
                variant = ButtonVariant.Secondary,
                type = ButtonType.Text,
            )
        },
        dismissButton = {
            LogFlareButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ButtonVariant.Primary,
                type = ButtonType.Text,
            )
        },
        containerColor = AppTheme.colors.surface,
        tonalElevation = 2.dp,
    )
}
