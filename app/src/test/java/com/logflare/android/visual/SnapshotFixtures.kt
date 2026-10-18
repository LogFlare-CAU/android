package com.logflare.android.visual

import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.mypage.AddMemberUiState
import com.logflare.android.feature.mypage.EditMemberUiState
import com.logflare.android.feature.mypage.InputValidationUiState
import com.logflare.android.ui.component.common.MemberFieldStatus

object SnapshotFixtures {
    private const val BASE_TIMESTAMP = "2026-01-15T10:30:00Z"

    fun addMember(
        valid: Boolean = false,
        loading: Boolean = false,
        error: String? = null,
        validationError: Boolean = false,
    ): AddMemberUiState =
        when {
            validationError -> AddMemberUiState(
                username = "qa",
                temporaryPassword = "short",
                usernameValidation = InputValidationUiState(
                    helperText = "Username must be at least 4 characters",
                    status = MemberFieldStatus.Error,
                ),
                passwordValidation = InputValidationUiState(
                    helperText = "Password must include uppercase, lowercase, and symbol",
                    status = MemberFieldStatus.Error,
                ),
            )
            valid -> AddMemberUiState(
                username = "qa-new-member",
                temporaryPassword = "Password1!",
                usernameValidation = InputValidationUiState(
                    helperText = "Looks good",
                    status = MemberFieldStatus.Valid,
                ),
                passwordValidation = InputValidationUiState(
                    helperText = "Looks strong",
                    status = MemberFieldStatus.Valid,
                ),
            )
            else -> AddMemberUiState()
        }.let { base ->
            base.copy(
                isLoading = loading,
                errorMessage = error,
            )
        }

    fun editMember(
        showDelete: Boolean = false,
        disabled: Boolean = false,
        loading: Boolean = false,
        validationError: Boolean = false,
    ): EditMemberUiState {
        val base = EditMemberUiState(
            originalUsername = "qa-member",
            username = if (validationError) "qa" else "qa-member",
            selectedPermission = UserPermission.USER,
            originalPermission = UserPermission.USER,
            showDeleteDialog = showDelete,
            disabled = disabled,
            isLoading = loading,
        )
        return if (validationError) {
            base.copy(
                usernameValidation = InputValidationUiState(
                    helperText = "Username must be at least 4 characters",
                    status = MemberFieldStatus.Error,
                ),
            )
        } else {
            base
        }
    }
}
