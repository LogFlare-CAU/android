package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.mypage.AddMemberScreenContent
import com.logflare.android.feature.mypage.AddMemberUiState
import com.logflare.android.feature.mypage.EditMemberScreenContent
import com.logflare.android.feature.mypage.EditMemberUiState
import com.logflare.android.feature.mypage.InputValidationUiState
import com.logflare.android.feature.mypage.LogoutScreenContent
import com.logflare.android.feature.mypage.LogoutUiState
import com.logflare.android.feature.mypage.MyPageContent
import com.logflare.android.feature.mypage.MyPageMemberUiModel
import com.logflare.android.feature.mypage.MyPageUiState
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.component.common.MemberFieldStatus
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MyPageScreenRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun myPageRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                MyPageContent(
                    uiState = MyPageUiState(
                        loading = false,
                        username = "qa-admin",
                        permission = UserPermission.MODERATOR,
                        members = listOf(
                            MyPageMemberUiModel("qa-member", UserPermission.USER),
                        ),
                        selectedLogLevel = LogLevel.ERROR,
                    ),
                    onLogout = {},
                    onAddMember = {},
                    onEditMember = { _ -> },
                    onSelectLogLevel = { _ -> },
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.MyPage).assertExists()
    }

    @Test fun addMemberRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                AddMemberScreenContent(
                    uiState = AddMemberUiState(
                        username = "new-member",
                        temporaryPassword = "Password1!",
                    ),
                    onBack = {},
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onValidateUsername = {},
                    onValidatePassword = {},
                    onPermissionChange = { _ -> },
                    onSubmit = {},
                    onDismissMessage = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.AddMember).assertExists()
    }

    @Test fun addMemberSaveInvokesValidateNotChange() {
        var usernameChanges = 0
        var passwordChanges = 0
        var usernameValidations = 0
        var passwordValidations = 0

        compose.setContent {
            LogflareandroidTheme(false) {
                AddMemberScreenContent(
                    uiState = AddMemberUiState(
                        username = "new-member",
                        temporaryPassword = "Password1!",
                        usernameValidation = InputValidationUiState(
                            helperText = "Looks good",
                            status = MemberFieldStatus.Valid,
                        ),
                        passwordValidation = InputValidationUiState(
                            helperText = "Looks strong",
                            status = MemberFieldStatus.Valid,
                        ),
                    ),
                    onBack = {},
                    onUsernameChange = { usernameChanges++ },
                    onPasswordChange = { passwordChanges++ },
                    onValidateUsername = { usernameValidations++ },
                    onValidatePassword = { passwordValidations++ },
                    onPermissionChange = { _ -> },
                    onSubmit = {},
                    onDismissMessage = {},
                )
            }
        }

        val saveButtons = compose.onAllNodesWithText("Save")
        saveButtons[0].performClick()
        saveButtons[1].performClick()

        assertEquals(1, usernameValidations)
        assertEquals(1, passwordValidations)
        assertEquals(0, usernameChanges)
        assertEquals(0, passwordChanges)
    }

    @Test fun editMemberRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                EditMemberScreenContent(
                    uiState = EditMemberUiState(
                        originalUsername = "qa-member",
                        username = "qa-member",
                        selectedPermission = UserPermission.USER,
                        originalPermission = UserPermission.USER,
                    ),
                    onBack = {},
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onValidateUsername = {},
                    onValidatePassword = {},
                    onPermissionChange = { _ -> },
                    onSave = {},
                    onDeleteRequest = {},
                    onDeleteConfirm = {},
                    onDeleteDismiss = {},
                    onDismissMessage = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.EditMember).assertExists()
    }

    @Test fun editMemberSaveInvokesValidateNotChange() {
        var usernameChanges = 0
        var passwordChanges = 0
        var usernameValidations = 0
        var passwordValidations = 0

        compose.setContent {
            LogflareandroidTheme(false) {
                EditMemberScreenContent(
                    uiState = EditMemberUiState(
                        originalUsername = "qa-member",
                        username = "qa-member",
                        newPassword = "Password1!",
                        usernameValidation = InputValidationUiState(
                            helperText = "Looks good",
                            status = MemberFieldStatus.Valid,
                        ),
                        passwordValidation = InputValidationUiState(
                            helperText = "Looks strong",
                            status = MemberFieldStatus.Valid,
                        ),
                        selectedPermission = UserPermission.USER,
                        originalPermission = UserPermission.USER,
                    ),
                    onBack = {},
                    onUsernameChange = { usernameChanges++ },
                    onPasswordChange = { passwordChanges++ },
                    onValidateUsername = { usernameValidations++ },
                    onValidatePassword = { passwordValidations++ },
                    onPermissionChange = { _ -> },
                    onSave = {},
                    onDeleteRequest = {},
                    onDeleteConfirm = {},
                    onDeleteDismiss = {},
                    onDismissMessage = {},
                )
            }
        }

        val saveButtons = compose.onAllNodesWithText("Save")
        saveButtons[0].performClick()
        saveButtons[1].performClick()

        assertEquals(1, usernameValidations)
        assertEquals(1, passwordValidations)
        assertEquals(0, usernameChanges)
        assertEquals(0, passwordChanges)
    }

    @Test fun editMemberErrorSnackbarUsesStateFlagNotMessageText() {
        compose.setContent {
            LogflareandroidTheme(false) {
                EditMemberScreenContent(
                    uiState = EditMemberUiState(
                        originalUsername = "qa-member",
                        username = "qa-member",
                        selectedPermission = UserPermission.USER,
                        originalPermission = UserPermission.USER,
                        snackbarMessage = "Looks fine but tagged as error",
                        snackbarIsError = true,
                    ),
                    onBack = {},
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onValidateUsername = {},
                    onValidatePassword = {},
                    onPermissionChange = { _ -> },
                    onSave = {},
                    onDeleteRequest = {},
                    onDeleteConfirm = {},
                    onDeleteDismiss = {},
                    onDismissMessage = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Error).assertExists()
        compose.onNodeWithText("Looks fine but tagged as error").assertExists()
    }

    @Test fun editMemberSuccessSnackbarOmitsErrorTag() {
        compose.setContent {
            LogflareandroidTheme(false) {
                EditMemberScreenContent(
                    uiState = EditMemberUiState(
                        originalUsername = "qa-member",
                        username = "qa-member",
                        selectedPermission = UserPermission.USER,
                        originalPermission = UserPermission.USER,
                        snackbarMessage = "Failed to update member: network",
                        snackbarIsError = false,
                    ),
                    onBack = {},
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onValidateUsername = {},
                    onValidatePassword = {},
                    onPermissionChange = { _ -> },
                    onSave = {},
                    onDeleteRequest = {},
                    onDeleteConfirm = {},
                    onDeleteDismiss = {},
                    onDismissMessage = {},
                )
            }
        }
        compose.onNodeWithText("Failed to update member: network").assertExists()
        assertTrue(
            compose.onAllNodesWithTag(VisualQaTags.Error)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test fun logoutRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LogoutScreenContent(
                    uiState = LogoutUiState(),
                    onBack = {},
                    onLogout = {},
                    onDismissError = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logout).assertExists()
    }
}
