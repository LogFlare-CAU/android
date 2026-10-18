package com.logflare.android.visual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.mypage.LogoutScreenContent
import com.logflare.android.feature.mypage.LogoutUiState
import com.logflare.android.feature.mypage.MyPageContent
import com.logflare.android.feature.mypage.MyPageMemberUiModel
import com.logflare.android.feature.mypage.MyPageUiState
import com.logflare.android.feature.project.ProjectsContent
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.project.ProjectsUiState
import com.logflare.android.feature.projectdetail.ProjectDetailLog
import com.logflare.android.feature.projectdetail.ProjectDetailScreenContent
import com.logflare.android.feature.projectdetail.ProjectDetailUiState
import com.logflare.android.ui.VisualQaAppRoot
import com.logflare.android.ui.VisualQaTags
import com.logflare.android.ui.theme.LogflareandroidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@Config(qualifiers = "w1080dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class VisualQaSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun appRootExposesTestTagsAsResourceId() {
        compose.setContent {
            LogflareandroidTheme(false) {
                VisualQaAppRoot {
                    Text("inside")
                }
            }
        }
        val node = compose.onNodeWithTag(VisualQaTags.AppRoot).fetchSemanticsNode()
        assertTrue(hasTestTagsAsResourceId(node.config))
        compose.onNodeWithText("inside").assertExists()
    }

    @Test
    fun projectListExposesProjectCardTagsAndClicksReachCallback() {
        var clicked: Int? = null
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectsContent(
                    state = ProjectsUiState(
                        items = listOf(qaProject(101, "Payments"), qaProject(202, "Checkout")),
                        total = 2,
                        initialLoad = false,
                    ),
                    now = Instant.parse("2026-10-18T12:00:00Z"),
                    onToggleSearch = {},
                    onSearch = {},
                    onOpenSort = {},
                    onCloseSort = {},
                    onSelectSort = {},
                    onRefresh = {},
                    onOpenProject = { clicked = it },
                    onCreateProject = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Projects).assertExists()
        compose.onNodeWithTag(VisualQaTags.projectCard(101)).assertExists()
        compose.onNodeWithTag(VisualQaTags.projectCard(202)).performClick()
        assertEquals(202, clicked)
    }

    private fun qaProject(id: Int, name: String) =
        ProjectItem(id, name, emptyList(), errorCount24h = 0, warningCount24h = 0, lastErrorAt = null)

    @Test
    fun projectListEmptyCreateTagReachesCallback() {
        var created = false
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectsContent(
                    state = ProjectsUiState(initialLoad = false, canCreate = true),
                    now = Instant.parse("2026-10-18T12:00:00Z"),
                    onToggleSearch = {},
                    onSearch = {},
                    onOpenSort = {},
                    onCloseSort = {},
                    onSelectSort = {},
                    onRefresh = {},
                    onOpenProject = {},
                    onCreateProject = { created = true },
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Projects).assertExists()
        compose.onNodeWithTag(VisualQaTags.Empty).assertExists()
        compose.onNodeWithTag(VisualQaTags.CreateProject).performClick()
        assertTrue(created)
    }

    @Test
    fun myPageExposesMemberRowAndActionTagsWithReachableCallbacks() {
        var edited: String? = null
        var addClicked = false
        var logoutClicked = false
        compose.setContent {
            LogflareandroidTheme(false) {
                MyPageContent(
                    uiState = MyPageUiState(
                        loading = false,
                        username = "qa-admin",
                        permission = UserPermission.MODERATOR,
                        members = listOf(
                            MyPageMemberUiModel("qa-member", UserPermission.USER, idx = 2),
                        ),
                        selectedLogLevel = LogLevel.ERROR,
                    ),
                    onLogout = { logoutClicked = true },
                    onAddMember = { addClicked = true },
                    onEditMember = { edited = it },
                    onSelectLogLevel = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.MyPage).assertExists()
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-member")).performClick()
        assertEquals("qa-member", edited)
        compose.onNodeWithTag(VisualQaTags.AddMember).performClick()
        assertTrue(addClicked)
        compose.onNodeWithTag(VisualQaTags.MyPage).performTouchInput { swipeUp() }
        compose.onNodeWithTag(VisualQaTags.LogoutAction).performClick()
        assertTrue(logoutClicked)
    }

    @Test
    fun projectDetailExposesOpenSettingsAndLogCards() {
        var openedSettings: Int? = null
        var openedLog: Int? = null
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectDetailScreenContent(
                    uiState = ProjectDetailUiState(
                        loading = false,
                        projectId = 101,
                        projectName = "Payments",
                        settingsLabel = "Project Settings",
                        logs = listOf(
                            ProjectDetailLog(
                                id = 5001,
                                level = LogLevel.ERROR,
                                timestamp = "2026-01-01T00:00:00Z",
                                message = "payment gateway timeout",
                                projectName = "Payments",
                                fileName = "payments.log",
                            ),
                        ),
                    ),
                    onBack = {},
                    onOpenProjectSettings = { openedSettings = it },
                    onLevelSelected = {},
                    onLogfileSelected = {},
                    onSortSelected = {},
                    onLogClick = { openedLog = it.id },
                    onLoadMore = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectDetail).assertExists()
        compose.onNodeWithTag(VisualQaTags.OpenProjectSettings).performClick()
        assertEquals(101, openedSettings)
        compose.onNodeWithTag(VisualQaTags.logCard(5001)).performClick()
        assertEquals(5001, openedLog)
    }

    @Test
    fun logoutConfirmTagReachesCallback() {
        var confirmed = false
        compose.setContent {
            LogflareandroidTheme(false) {
                LogoutScreenContent(
                    uiState = LogoutUiState(),
                    onBack = {},
                    onLogout = { confirmed = true },
                    onDismissError = {},
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.Logout).assertExists()
        compose.onNodeWithTag(VisualQaTags.ConfirmLogout).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun topAppBarBackControlExposesNavigateBackTag() {
        var back = false
        compose.setContent {
            LogflareandroidTheme(false) {
                LogFlareTopAppBar(
                    titleType = TopAppBarTitleType.Title,
                    titleText = "LOGS",
                    onBack = { back = true },
                    backTestTag = VisualQaTags.NavigateBack,
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.NavigateBack).performClick()
        assertTrue(back)
    }

    @Test
    fun appRootIsSingleFullScreenResourceIdHost() {
        compose.setContent {
            LogflareandroidTheme(false) {
                VisualQaAppRoot(modifier = Modifier.fillMaxSize()) {
                    Column {
                        Text("child")
                    }
                }
            }
        }
        compose.onAllNodesWithTag(VisualQaTags.AppRoot).assertCountEquals(1)
        val node = compose.onNodeWithTag(VisualQaTags.AppRoot).fetchSemanticsNode()
        assertTrue(node.boundsInRoot.width > 0)
        assertTrue(node.boundsInRoot.height > 0)
        assertTrue(hasTestTagsAsResourceId(node.config))
    }

    private fun hasTestTagsAsResourceId(config: SemanticsConfiguration): Boolean {
        val entry = config.firstOrNull { it.key.name == "TestTagsAsResourceId" }
        assertNotNull("expected TestTagsAsResourceId semantics key on app_root", entry)
        return entry!!.value == true
    }
}
