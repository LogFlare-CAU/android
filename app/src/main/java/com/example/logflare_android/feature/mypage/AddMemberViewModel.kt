package com.example.logflare_android.feature.mypage

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

data class InputValidationUiState(
    val helperText: String? = null,
    val status: MemberFieldStatus = MemberFieldStatus.Idle
)

data class AddMemberUiState(
    val username: String = "",
    val temporaryPassword: String = "",
    val usernameValidation: InputValidationUiState = InputValidationUiState(),
    val passwordValidation: InputValidationUiState = InputValidationUiState(
        helperText = "Use English, numbers, and symbols only",
        status = MemberFieldStatus.Idle
    ),
    val selectedPermission: UserPermission = UserPermission.USER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AddMemberViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(AddMemberUiState())
    val ui: StateFlow<AddMemberUiState> = _ui

    private var usernameValidationJob: Job? = null
    private var passwordValidationJob: Job? = null

    fun updateUsername(username: String) {
        _ui.update {
            it.copy(
                username = username,
                usernameValidation = InputValidationUiState(),
                errorMessage = null,
                successMessage = null
            )
        }
        scheduleUsernameValidation()
    }

    fun updateTemporaryPassword(password: String) {
        _ui.update {
            it.copy(
                temporaryPassword = password,
                passwordValidation = InputValidationUiState(),
                errorMessage = null,
                successMessage = null
            )
        }
        schedulePasswordValidation()
    }

    fun retryUsernameValidation() {
        scheduleUsernameValidation(forceImmediate = true)
    }

    fun retryPasswordValidation() {
        schedulePasswordValidation(forceImmediate = true)
    }

    fun selectPermission(permission: UserPermission) {
        _ui.update {
            it.copy(
                selectedPermission = permission,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun addMember(onSuccess: () -> Unit) {
        val currentState = _ui.value
        if (currentState.usernameValidation.status != MemberFieldStatus.Valid) {
            _ui.update { it.copy(errorMessage = "Username must be valid before continuing") }
            return
        }

        if (currentState.passwordValidation.status != MemberFieldStatus.Valid) {
            _ui.update { it.copy(errorMessage = "Password must be valid before continuing") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {
                delay(1000)

                _ui.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Member added successfully",
                        usernameValidation = InputValidationUiState(status = MemberFieldStatus.Completed),
                        passwordValidation = InputValidationUiState(status = MemberFieldStatus.Completed)
                    )
                }

                delay(500)
                onSuccess()
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add member: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _ui.update { it.copy(errorMessage = null, successMessage = null) }
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
                trimmed.isBlank() -> setUsernameValidation(InputValidationUiState())
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

                RESERVED_MEMBER_NAMES.any { it.equals(trimmed, ignoreCase = true) } -> setUsernameValidation(
                    InputValidationUiState(
                        helperText = "This member name already exists",
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
        if (_ui.value.temporaryPassword.isBlank()) {
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
            val current = _ui.value.temporaryPassword
            when {
                current.length < 8 -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Use at least 8 characters",
                        status = MemberFieldStatus.Error
                    )
                )

                current.none { it.isDigit() } -> setPasswordValidation(
                    InputValidationUiState(
                        helperText = "Include at least one number",
                        status = MemberFieldStatus.Error
                    )
                )

                !current.matches(PASSWORD_ALLOWED_REGEX) -> setPasswordValidation(
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
        private val USERNAME_ALLOWED_REGEX = Regex("^[A-Za-z0-9_]+$")
        private val RESERVED_MEMBER_NAMES = setOf("logflare_admin", "project_owner", "viewer01")
        private val PASSWORD_ALLOWED_REGEX = Regex(
            """^[A-Za-z0-9!@#${'$'}%^&*()_+\-=\[\]{};':"\\|,.<>/?`~]+$"""
        )
    }
}
