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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.dropdown.DropdownSize
import com.example.logflare.core.designsystem.components.dropdown.LogFlareDropdown
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.logflare.android.enums.UserPermission
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.component.common.LogFlareActionTextField
import com.logflare.android.ui.component.common.LogFlareActionTextFieldHelperTone
import com.logflare.android.ui.component.common.LogFlareActionTextFieldState
import com.logflare.android.ui.component.common.MemberFieldStatus
import com.logflare.android.ui.component.common.toActionTextFieldState

@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMemberViewModel = hiltViewModel(),
) {
    val uiState by viewModel.ui.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.surface,
        topBar = {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "Add Member",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        AddMemberScreenContent(
            uiState = uiState,
            onUsernameChange = { value ->
                if (value == uiState.username) {
                    viewModel.retryUsernameValidation()
                } else {
                    viewModel.updateUsername(value)
                }
            },
            onPasswordChange = { value ->
                if (value == uiState.temporaryPassword) {
                    viewModel.retryPasswordValidation()
                } else {
                    viewModel.updateTemporaryPassword(value)
                }
            },
            onPermissionChange = { permission ->
                if (permission == uiState.selectedPermission) {
                    viewModel.clearError()
                } else {
                    viewModel.selectPermission(permission)
                }
            },
            onSubmit = { viewModel.addMember(onBack) },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun AddMemberScreenContent(
    uiState: AddMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSubmit = uiState.usernameValidation.status == MemberFieldStatus.Valid &&
        uiState.passwordValidation.status == MemberFieldStatus.Valid &&
        uiState.username.isNotBlank() &&
        uiState.temporaryPassword.isNotBlank() &&
        !uiState.isLoading

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(VisualQaTags.AddMember),
        color = AppTheme.colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AddMemberForm(
                uiState = uiState,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onPermissionChange = onPermissionChange,
                modifier = Modifier.weight(1f),
            )

            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = AppTheme.colors.surface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.s4)
                        .padding(vertical = AppTheme.spacing.s3)
                        .navigationBarsPadding(),
                ) {
                    LogFlareButton(
                        text = if (uiState.isLoading) "Saving..." else "Done",
                        onClick = onSubmit,
                        size = ButtonSize.Large,
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMemberForm(
    uiState: AddMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPermissionChange: (UserPermission) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val usernameFieldState = uiState.usernameValidation.status.toActionTextFieldState()
    val passwordFieldState = uiState.passwordValidation.status.toActionTextFieldState()

    Column(
        modifier = modifier
            .fillMaxSize()
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
            onActionClick = { onUsernameChange(uiState.username) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))

        val canSavePassword = uiState.passwordValidation.status == MemberFieldStatus.Valid && !uiState.isLoading

        LogFlareActionTextField(
            label = "Password",
            value = uiState.temporaryPassword,
            onValueChange = onPasswordChange,
            placeholder = "Use English, numbers, symbols",
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
            onActionClick = { onPasswordChange(uiState.temporaryPassword) },
            modifier = Modifier.fillMaxWidth(),
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
            )
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
            AddMemberBanner(
                text = message,
                isError = true,
                onDismiss = { onPermissionChange(uiState.selectedPermission) },
                modifier = Modifier.testTag(VisualQaTags.Error),
            )
        }

        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
            AddMemberBanner(
                text = message,
                isError = false,
                onDismiss = { onPermissionChange(uiState.selectedPermission) },
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.s6))
    }
}

@Composable
private fun AddMemberBanner(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val background = if (isError) {
        colors.red.default.copy(alpha = 0.08f)
    } else {
        colors.primary.default.copy(alpha = 0.12f)
    }
    val contentColor = if (isError) colors.red.default else colors.primary.default

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDismiss() },
        color = background,
        shape = AppTheme.radius.large,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.bodySmMedium,
            color = contentColor,
            modifier = Modifier
                .padding(horizontal = AppTheme.spacing.s4, vertical = AppTheme.spacing.s3),
        )
    }
}
