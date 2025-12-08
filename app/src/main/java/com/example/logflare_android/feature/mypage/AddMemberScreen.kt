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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare.core.designsystem.AppTheme
import com.example.logflare.core.designsystem.components.button.ButtonSize
import com.example.logflare.core.designsystem.components.button.LogFlareButton
import com.example.logflare.core.designsystem.components.dropdown.DropdownSize
import com.example.logflare.core.designsystem.components.dropdown.LogFlareDropdown
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.example.logflare_android.ui.component.common.LogFlareActionTextField
import com.example.logflare_android.ui.component.common.LogFlareActionTextFieldHelperTone
import com.example.logflare_android.ui.component.common.LogFlareActionTextFieldState
import com.example.logflare_android.ui.component.common.MemberFieldStatus
import com.example.logflare_android.ui.component.common.toActionTextFieldState
import com.example.logflare_android.enums.UserPermission

@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.ui.collectAsState()

    val canSubmit = uiState.usernameValidation.status == MemberFieldStatus.Valid &&
        uiState.passwordValidation.status == MemberFieldStatus.Valid &&
        uiState.username.isNotBlank() &&
        uiState.temporaryPassword.isNotBlank() &&
        !uiState.isLoading

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.neutral.white,
        topBar = {
            LogFlareTopAppBar(
                titleType = TopAppBarTitleType.Title,
                titleText = "Add Member",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = AppTheme.colors.neutral.white
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.s4)
                        .padding(vertical = AppTheme.spacing.s3)
                        .navigationBarsPadding()
                ) {
                    LogFlareButton(
                        text = if (uiState.isLoading) "Saving..." else "Done",
                        onClick = { viewModel.addMember(onBack) },
                        size = ButtonSize.Large,
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        AddMemberContent(
            uiState = uiState,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updateTemporaryPassword,
            onPermissionSelect = viewModel::selectPermission,
            onRequestUsernameValidation = viewModel::retryUsernameValidation,
            onRequestPasswordValidation = viewModel::retryPasswordValidation,
            onClearBanner = viewModel::clearError,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}


@Composable
private fun AddMemberContent(
    uiState: AddMemberUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPermissionSelect: (UserPermission) -> Unit,
    onRequestUsernameValidation: () -> Unit,
    onRequestPasswordValidation: () -> Unit,
    onClearBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val usernameFieldState = uiState.usernameValidation.status.toActionTextFieldState()
    val passwordFieldState = uiState.passwordValidation.status.toActionTextFieldState()

    Column(
        modifier = modifier
            .fillMaxSize()
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
            modifier = Modifier.fillMaxWidth()
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
                keyboardType = KeyboardType.Password
            ),
            visualTransformation = PasswordVisualTransformation(),
            onActionClick = onRequestPasswordValidation,
            modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.width(140.dp)
            )
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
            AddMemberBanner(
                text = message,
                isError = true,
                onDismiss = onClearBanner
            )
        }

        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(AppTheme.spacing.s4))
            AddMemberBanner(
                text = message,
                isError = false,
                onDismiss = onClearBanner
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
    modifier: Modifier = Modifier
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
        shape = AppTheme.radius.large
    ) {
        Text(
            text = text,
            style = AppTheme.typography.bodySmMedium,
            color = contentColor,
            modifier = Modifier
                .padding(horizontal = AppTheme.spacing.s4, vertical = AppTheme.spacing.s3)
        )
    }
}
