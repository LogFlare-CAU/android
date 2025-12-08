package com.example.logflare_android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.ui.common.member.MemberFieldStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditMemberUiState(
    val originalUsername: String = "",
    val username: String = "",
    val newPassword: String = "",
    val usernameValidation: InputValidationUiState = InputValidationUiState(),
    val passwordValidation: InputValidationUiState = InputValidationUiState(
        helperText = "Use English, numbers, and symbols only",
        status = MemberFieldStatus.Idle
    ),
    val selectedPermission: UserPermission = UserPermission.USER,
    val originalPermission: UserPermission = UserPermission.USER,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val requestedUsername: String = savedStateHandle.get<String>("username") ?: ""

    private val _ui = MutableStateFlow(EditMemberUiState(originalUsername = requestedUsername))
    val ui: StateFlow<EditMemberUiState> = _ui

    private var usernameValidationJob: Job? = null
    private var passwordValidationJob: Job? = null

    init {
        loadMemberData()
    }

    private fun loadMemberData() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            delay(400)
            val seedUsername = requestedUsername.ifBlank { MOCK_EDIT_MEMBER_USERNAME }
            val initialPermission = when (seedUsername.lowercase()) {
                "ops-monitor" -> UserPermission.MODERATOR
                "super-observer" -> UserPermission.SUPER_USER
                                else -> UserPermission.USER
            }
            _ui.update {
                it.copy(
                    isLoading = false,
                    originalUsername = seedUsername,
                    username = seedUsername,
                    selectedPermission = initialPermission,
                    originalPermission = initialPermission,
                    usernameValidation = InputValidationUiState(
                        helperText = "Looks good",
                        status = MemberFieldStatus.Valid
                    )
                )
            }
        }
    }

    fun updateUsername(username: String) {
        _ui.update {
            it.copy(
                username = username,
                usernameValidation = InputValidationUiState(),
                snackbarMessage = null
            )
        }
        scheduleUsernameValidation()
    }

    fun retryUsernameValidation() {
        scheduleUsernameValidation(forceImmediate = true)
    }

    fun updatePassword(password: String) {
        _ui.update {
            it.copy(
                newPassword = password,
                passwordValidation = InputValidationUiState(
                    helperText = "Use English, numbers, and symbols only",
                    status = if (password.isBlank()) MemberFieldStatus.Idle else MemberFieldStatus.Idle
                ),
                snackbarMessage = null
            )
        }
        schedulePasswordValidation()
    }

    fun retryPasswordValidation() {
        schedulePasswordValidation(forceImmediate = true)
    }

    fun selectPermission(permission: UserPermission) {
        _ui.update {
            it.copy(
                selectedPermission = permission,
                snackbarMessage = null
            )
        }
    }

    fun saveChanges() {
        val current = _ui.value
        val usernameChanged = current.username != current.originalUsername &&
            current.usernameValidation.status == MemberFieldStatus.Valid
        val passwordReady = current.passwordValidation.status == MemberFieldStatus.Valid
        val roleChanged = current.selectedPermission != current.originalPermission

        if (!usernameChanged && !passwordReady && !roleChanged) {
            _ui.update { it.copy(snackbarMessage = "No changes to save") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            delay(600)
            val snackbarText = when {
                passwordReady -> "Member’s password updated successfully"
                roleChanged -> "Member’s role updated successfully"
                else -> "Member profile updated successfully"
            }
            _ui.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = snackbarText,
                    originalUsername = if (usernameChanged) it.username else it.originalUsername,
                    originalPermission = if (roleChanged) it.selectedPermission else it.originalPermission,
                    newPassword = if (passwordReady) "" else it.newPassword,
                    passwordValidation = if (passwordReady) {
                        InputValidationUiState(status = MemberFieldStatus.Completed)
                    } else {
                        it.passwordValidation
                    },
                    usernameValidation = if (usernameChanged) {
                        InputValidationUiState(status = MemberFieldStatus.Completed)
                    } else {
                        it.usernameValidation
                    }
                )
            }
        }
    }

    fun showDeleteDialog() {
        _ui.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _ui.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteMember(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, showDeleteDialog = false) }
            delay(600)
            val username = _ui.value.originalUsername
            _ui.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = "${username.ifBlank { "Member" }} deleted successfully"
                )
            }
            delay(600)
            onDeleted()
        }
    }

    fun dismissSnackbar() {
        _ui.update { it.copy(snackbarMessage = null) }
    }

    private fun scheduleUsernameValidation(forceImmediate: Boolean = false) {
        usernameValidationJob?.cancel()
        if (_ui.value.username.isBlank()) {
            setUsernameValidation(InputValidationUiState())
            return
        }

        usernameValidationJob = viewModelScope.launch {
            setUsernameValidation(
                InputValidationUiState(
                    helperText = "Validating username...",
                    status = MemberFieldStatus.Validating
                )
            )
            delay(if (forceImmediate) MANUAL_VALIDATION_DELAY else AUTO_VALIDATION_DELAY)
            val trimmed = _ui.value.username.trim()
            when {
                trimmed.length < 3 -> setUsernameValidation(
                    InputValidationUiState(
                        helperText = "Use at least 3 characters",
                        status = MemberFieldStatus.Error
                    )
                )

                !trimmed.matches(USERNAME_ALLOWED_REGEX) -> setUsernameValidation(
                    InputValidationUiState(
                        helperText = "Use English, numbers, and '_' only",
                        status = MemberFieldStatus.Error
                    )
                )

                else -> setUsernameValidation(
                    InputValidationUiState(
                        helperText = "Looks good",
                        status = MemberFieldStatus.Valid
                    )
                )
            }
        }
    }

    private fun schedulePasswordValidation(forceImmediate: Boolean = false) {
        passwordValidationJob?.cancel()
        if (_ui.value.newPassword.isBlank()) {
            setPasswordValidation(
                InputValidationUiState(
                    helperText = "Use English, numbers, and symbols only",
                    status = MemberFieldStatus.Idle
                )
            )
            return
        }

        passwordValidationJob = viewModelScope.launch {
            setPasswordValidation(
                InputValidationUiState(
                    helperText = "Checking password...",
                    status = MemberFieldStatus.Validating
                )
            )
            delay(if (forceImmediate) MANUAL_VALIDATION_DELAY else AUTO_VALIDATION_DELAY)
            val password = _ui.value.newPassword
            when {
                password.length < 8 -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Use at least 8 characters",
                        status = MemberFieldStatus.Error
                    )
                )

                password.none { it.isDigit() } -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Include at least one number",
                        status = MemberFieldStatus.Error
                    )
                )

                !password.matches(PASSWORD_ALLOWED_REGEX) -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Use English letters, digits, or symbols",
                        status = MemberFieldStatus.Error
                    )
                )

                else -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Looks strong",
                        status = MemberFieldStatus.Valid
                    )
                )
            }
        }
    }

    private fun setUsernameValidation(state: InputValidationUiState) {
        _ui.update { it.copy(usernameValidation = state) }
    }

    private fun setPasswordValidation(state: InputValidationUiState) {
        _ui.update { it.copy(passwordValidation = state) }
    }

    companion object {
        private const val AUTO_VALIDATION_DELAY = 600L
        private const val MANUAL_VALIDATION_DELAY = 250L
        private const val MOCK_EDIT_MEMBER_USERNAME = "edit_member_demo"
        private val MOCK_EDIT_MEMBER_PERMISSION = UserPermission.MODERATOR
        private val USERNAME_ALLOWED_REGEX = Regex("^[A-Za-z0-9_]+$")
        private val PASSWORD_ALLOWED_REGEX = Regex(
            """^[A-Za-z0-9!@#${'$'}%^&*()_+\-=\[\]{};':"\\|,.<>/?`~]+$"""
        )
    }
}
