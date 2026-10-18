package com.logflare.android.visual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.logflare.core.designsystem.components.navigation.LogFlareTopAppBar
import com.example.logflare.core.designsystem.components.navigation.TopAppBarTitleType
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.mypage.MyActions
import com.logflare.android.feature.mypage.MyContent
import com.logflare.android.feature.mypage.MySheet
import com.logflare.android.feature.mypage.MyUiState
import com.logflare.android.feature.project.ProjectsContent
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.project.ProjectsUiState
import com.logflare.android.feature.projectdetail.ProjectDetailContent
import com.logflare.android.feature.projectdetail.ProjectDetailUiState
import com.logflare.android.data.store.model.LogEvent
import com.example.logflare.core.designsystem.v2.LfLevel
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
        var edited: Int? = null
        var addClicked = false
        var sheet: MySheet? = null
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                MyContent(
                    state = MyUiState(
                        me = Member(1, "qa-admin", 80),
                        serverHost = "logs.example.com",
                        members = listOf(Member(2, "qa-member", 0)),
                        canManageMembers = true,
                        version = "2.0.0",
                    ),
                    actions = MyActions(
                        onAddMember = { addClicked = true },
                        onEditMember = { edited = it },
                        onOpenSheet = { sheet = it },
                    ),
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.MyPage).assertExists()
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-member")).performClick()
        assertEquals(2, edited)
        compose.onNodeWithTag(VisualQaTags.AddMember).performClick()
        assertTrue(addClicked)
        compose.onNodeWithTag(VisualQaTags.LogoutAction).performScrollTo().performClick()
        assertEquals(MySheet.Logout, sheet)
    }

    @Test
    fun myPageSuperAdminAndEqualAdminRowsAreNotEditable() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                MyContent(
                    state = MyUiState(
                        me = Member(7, "qa-admin", 80),
                        members = listOf(Member(1, "qa-root", 100), Member(9, "qa-peer", 80), Member(2, "qa-member", 0)),
                        canManageMembers = true,
                    ),
                    actions = MyActions(),
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-root")).assertHasNoClickAction()
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-peer")).assertHasNoClickAction()
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-member")).assertHasClickAction()
    }

    @Test
    fun myPageMemberRowsAreNotClickableWithoutManageRights() {
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                MyContent(
                    state = MyUiState(
                        me = Member(1, "qa-member", 0),
                        members = listOf(Member(2, "qa-other", 0)),
                        canManageMembers = false,
                    ),
                    actions = MyActions(),
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.memberRow("qa-other")).assertHasNoClickAction()
        compose.onNodeWithTag(VisualQaTags.AddMember).assertDoesNotExist()
    }

    @Test
    fun projectDetailExposesOpenSettingsAndLogCards() {
        var openedSettings = 0
        var openedLog: Int? = null
        compose.setContent {
            LogflareandroidTheme(false) {
                ProjectDetailContent(
                    state = ProjectDetailUiState(
                        project = ProjectItem(101, "Payments", emptyList(), 0, 0, null),
                        initialLoad = false,
                        items = listOf(
                            LogEvent(
                                id = 5001,
                                projectId = 101,
                                logFileName = "payments.log",
                                type = null,
                                message = "payment gateway timeout",
                                level = LfLevel.Error,
                                lastSeen = Instant.parse("2026-01-01T00:00:00Z"),
                                firstSeen = Instant.parse("2026-01-01T00:00:00Z"),
                                occurrences = 1,
                            ),
                        ),
                    ),
                    onBack = {},
                    onOpenSettings = { openedSettings++ },
                    onToggleErrors = {},
                    onToggleWarnings = {},
                    onOpenSheet = {},
                    onCloseSheet = {},
                    onSelectFile = {},
                    onSelectLevel = {},
                    onSelectSort = {},
                    onClearFilters = {},
                    onRefresh = {},
                    onEndReached = {},
                    onOpenLog = { openedLog = it },
                )
            }
        }
        compose.onNodeWithTag(VisualQaTags.ProjectDetail).assertExists()
        compose.onNodeWithTag(VisualQaTags.OpenProjectSettings).performClick()
        assertEquals(1, openedSettings)
        compose.onNodeWithTag(VisualQaTags.logCard(5001)).performClick()
        assertEquals(5001, openedLog)
    }

    @Test
    fun logoutSheetConfirmReachesCallback() {
        var confirmed = false
        compose.setContent {
            LogFlareTheme(darkTheme = false) {
                MyContent(
                    state = MyUiState(me = Member(1, "qa-admin", 80), sheet = MySheet.Logout),
                    actions = MyActions(onConfirmLogout = { confirmed = true }),
                )
            }
        }
        compose.onNodeWithText("Log out?").assertExists()
        compose.onAllNodesWithText("Log out").filterToOne(hasClickAction() and !hasTestTag(VisualQaTags.LogoutAction)).performClick()
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
