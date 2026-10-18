package com.logflare.android.visual

import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.mypage.AddMemberUiState
import com.logflare.android.feature.mypage.EditMemberUiState
import com.logflare.android.feature.mypage.InputValidationUiState
import com.logflare.android.feature.mypage.LogoutUiState
import com.logflare.android.feature.mypage.MyPageMemberUiModel
import com.logflare.android.feature.mypage.MyPageUiState
import com.logflare.android.feature.project.ProjectCreateUiState
import com.logflare.android.feature.project.ProjectPermissionUiFactory
import com.logflare.android.ui.component.common.MemberFieldStatus

object SnapshotFixtures {
    private const val BASE_TIMESTAMP = "2026-01-15T10:30:00Z"

    fun projectEditor(
        saved: Boolean = false,
        loading: Boolean = false,
        error: String? = null,
        invalid: Boolean = false,
    ): ProjectCreateUiState =
        ProjectCreateUiState(
            id = if (saved) 101 else 0,
            name = when {
                invalid -> "Bad/Name?"
                saved || loading -> "QA Payments"
                else -> ""
            },
            nameValid = when {
                invalid -> false
                saved || loading -> true
                else -> false // blank initial matches production default
            },
            loading = loading,
            token = if (saved) "qa-token-101-abcdef" else null,
            error = error,
            keywords = if (saved) listOf("timeout", "retry") else emptyList(),
            alertLevels = if (saved) setOf("Error", "Critical") else emptySet(),
            saved = saved,
            permissions = if (saved) {
                listOf(
                    ProjectPermissionUiFactory.adminUser(username = "qa-admin"),
                    ProjectPermissionUiFactory.memberUser(username = "qa-member", active = true),
                )
            } else {
                emptyList()
            },
        )

    fun myPage(
        empty: Boolean = false,
        loading: Boolean = false,
        error: String? = null,
        admin: Boolean = true,
    ): MyPageUiState =
        MyPageUiState(
            loading = loading,
            username = "qa-admin",
            permission = if (admin) UserPermission.MODERATOR else UserPermission.USER,
            members = when {
                loading || error != null -> emptyList()
                empty -> emptyList()
                else -> listOf(MyPageMemberUiModel("qa-member", UserPermission.USER, idx = 2))
            },
            selectedLogLevel = LogLevel.ERROR,
            errorMessage = error,
        )

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

    fun logout(
        loading: Boolean = false,
        error: String? = null,
    ): LogoutUiState =
        LogoutUiState(
            isLoading = loading,
            errorMessage = error,
        )
}
