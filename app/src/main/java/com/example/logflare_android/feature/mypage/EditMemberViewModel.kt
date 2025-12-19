package com.example.logflare_android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.UserDTO
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.usecase.AuthMeUseCase
import com.example.logflare_android.feature.usecase.DeleteUserUseCase
import com.example.logflare_android.feature.usecase.GetUserUseCase
import com.example.logflare_android.feature.usecase.UpdateUserUseCase
import com.example.logflare_android.ui.component.common.MemberFieldStatus
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
    val showDeleteDialog: Boolean = false,
    val disabled: Boolean = false
)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authMeUseCase: AuthMeUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) : ViewModel() {

    private val requestedUsername: String = savedStateHandle.get<String>("username") ?: ""
    private var requestedUser: UserDTO? = null
    private var me: UserDTO? = null
    private val _ui = MutableStateFlow(EditMemberUiState(originalUsername = requestedUsername))
    val ui: StateFlow<EditMemberUiState> = _ui

    private var usernameValidationJob: Job? = null
    private var passwordValidationJob: Job? = null

    init {
        viewModelScope.launch {
            initUsers()
            loadMemberData()
            checkPermissionToEdit()
        }
    }

    private suspend fun initUsers() {
        _ui.update { it.copy(isLoading = true) }
        requestedUser = getUserUseCase(requestedUsername)
        me = authMeUseCase()
        _ui.update { it.copy(isLoading = false) }
    }

    private fun checkPermissionToEdit() {
        val myPermission = UserPermission.fromCode(me?.permission ?: 0)
        val targetPermission = UserPermission.fromCode(requestedUser?.permission ?: 0)
        if ((myPermission <= targetPermission && me?.username != requestedUser?.username) || requestedUser?.permission == UserPermission.SUPER_USER.code) {
            val message = if (requestedUser?.permission == UserPermission.SUPER_USER.code) {
                "You cannot edit a Super User member"
            } else {
                "You do not have permission to edit this member"
            }
            _ui.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = message,
                    disabled = true,
                    usernameValidation = InputValidationUiState(
                        status = MemberFieldStatus.Error,
                        helperText = message
                    ),
                )
            }
        }
    }

    private fun loadMemberData() {
        _ui.update { it.copy(isLoading = true) }
        val user = requestedUser ?: return
        val seedUsername = user.username
        val initialPermission = UserPermission.fromCode(user.permission)
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
            updateUserUseCase(
                userId = requestedUser?.idx ?: return@launch,
                username = if (usernameChanged) current.username.trim() else null,
                password = if (passwordReady) current.newPassword else null,
                permission = if (roleChanged) current.selectedPermission else null
            )
                .onSuccess {
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
                .onFailure { error ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Failed to update member: ${error.message}"
                        )
                    }
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
            deleteUserUseCase(requestedUser?.idx ?: return@launch)
                .onSuccess {
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
                .onFailure { error ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Failed to delete member: ${error.message}"
                        )
                    }
                }
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
