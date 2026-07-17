package com.logflare.android.visual

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.mypage.AddMemberScreenContent
import com.logflare.android.feature.mypage.AddMemberUiState
import com.logflare.android.feature.mypage.EditMemberScreenContent
import com.logflare.android.feature.mypage.EditMemberUiState
import com.logflare.android.feature.mypage.LogoutScreenContent
import com.logflare.android.feature.mypage.LogoutUiState
import com.logflare.android.feature.mypage.MyPageContent
import com.logflare.android.feature.mypage.MyPageMemberUiModel
import com.logflare.android.feature.mypage.MyPageUiState
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.LogflareandroidTheme
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
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onPermissionChange = { _ -> },
                    onSubmit = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.AddMember).assertExists()
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
                    onUsernameChange = { _ -> },
                    onPasswordChange = { _ -> },
                    onPermissionChange = { _ -> },
                    onSave = {},
                    onDeleteRequest = {},
                    onDeleteConfirm = {},
                    onDeleteDismiss = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.EditMember).assertExists()
    }

    @Test fun logoutRendersFromStateOnly() {
        compose.setContent {
            LogflareandroidTheme(false) {
                LogoutScreenContent(
                    uiState = LogoutUiState(),
                    onBack = {},
                    onLogout = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logout).assertExists()
    }
}
