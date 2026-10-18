package com.logflare.android.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavHostTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: TestNavHostController
    private var session by mutableStateOf<SessionState>(SessionState.SignedOut)
    private var deepLink by mutableStateOf<Int?>(null)

    @Test
    fun signedOut_startsAtLogin() {
        show(SessionState.SignedOut)

        assertEquals(listOf(Destinations.Login), routes())
    }

    @Test
    fun signedIn_startsAtHome() {
        show(SIGNED_IN)

        assertEquals(listOf(Destinations.Home), routes())
        compose.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun tabReselect_keepsBackStackState() {
        show(SIGNED_IN)

        compose.onNodeWithText("Logs").performClick()
        compose.runOnIdle { nav.navigate(Destinations.logDetail(7)) }
        compose.runOnIdle { nav.navigateToTab(Destinations.Home) }
        compose.waitForIdle()
        assertEquals(listOf(Destinations.Home), routes())

        compose.onNodeWithText("Logs").performClick()
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home, Destinations.Logs, Destinations.LogDetailPattern), routes())
        assertEquals(7, nav.currentBackStackEntry?.arguments?.getInt(Destinations.ErrorIdArg))
    }

    @Test
    fun reselectCurrentTab_keepsSameEntry() {
        show(SIGNED_IN)
        listOf("Logs" to Destinations.Logs, "Projects" to Destinations.Projects, "My" to Destinations.My)
            .forEach { (label, route) ->
                compose.onNodeWithText(label).performClick()
                compose.waitForIdle()
                val entry = compose.runOnIdle { nav.currentBackStackEntry }
                val before = routes()
                assertEquals(listOf(Destinations.Home, route), before)

                compose.onNodeWithText(label).performClick()
                compose.waitForIdle()

                assertSame(entry, compose.runOnIdle { nav.currentBackStackEntry })
                assertEquals(before, routes())
            }
    }

    @Test
    fun reselectTab_fromInsideIt_popsToItsRoot() {
        show(SIGNED_IN)
        compose.onNodeWithText("Logs").performClick()
        compose.waitForIdle()
        val logsEntry = compose.runOnIdle { nav.currentBackStackEntry }
        compose.runOnIdle { nav.navigate(Destinations.logDetail(42)) }

        compose.runOnIdle { nav.navigateToTab(Destinations.Logs) }
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home, Destinations.Logs), routes())
        assertSame(logsEntry, compose.runOnIdle { nav.currentBackStackEntry })
    }

    @Test
    fun reselectHome_fromASubScreenOpenedOnHome_popsToHome() {
        show(SIGNED_IN)
        val homeEntry = compose.runOnIdle { nav.currentBackStackEntry }
        compose.runOnIdle { nav.navigate(Destinations.projectDetail(3)) }

        compose.runOnIdle { nav.navigateToTab(Destinations.Home) }
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home), routes())
        assertSame(homeEntry, compose.runOnIdle { nav.currentBackStackEntry })
    }

    @Test
    fun deepLink_errorId_opensDetailAboveHome() {
        var consumed = 0
        show(SIGNED_IN, deepLinkErrorId = 42, onConsumed = { consumed++; deepLink = null })

        assertEquals(listOf(Destinations.Home, Destinations.LogDetailPattern), routes())
        assertEquals(42, nav.currentBackStackEntry?.arguments?.getInt(Destinations.ErrorIdArg))
        assertEquals(1, consumed)

        compose.runOnIdle { nav.popBackStack() }
        compose.waitForIdle()
        assertEquals(listOf(Destinations.Home), routes())
    }

    @Test
    fun openLogs_presetsMinLevel_keepingTheTabEntry() {
        show(SIGNED_IN)
        compose.onNodeWithText("Logs").performClick()
        compose.waitForIdle()
        val logsEntry = compose.runOnIdle { nav.currentBackStackEntry }
        compose.runOnIdle { nav.navigateToTab(Destinations.Home) }
        compose.waitForIdle()

        compose.runOnIdle { nav.openLogs(LfLevel.Warning) }
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home, Destinations.Logs), routes())
        val entry = compose.runOnIdle { nav.currentBackStackEntry!! }
        assertEquals(logsEntry?.id, entry.id)
        assertEquals("WARNING", entry.savedStateHandle.get<String>(Destinations.LogsMinLevelArg))

        // No preset ("See all"): clears an earlier one.
        compose.runOnIdle { nav.navigateToTab(Destinations.Home) }
        compose.runOnIdle { nav.openLogs(null) }
        compose.waitForIdle()
        assertNull(compose.runOnIdle { nav.currentBackStackEntry!!.savedStateHandle.get<String>(Destinations.LogsMinLevelArg) })
    }

    @Test
    fun deepLink_whileSignedOut_isDropped() {
        var consumed = 0
        show(SessionState.SignedOut, deepLinkErrorId = 42, onConsumed = { consumed++; deepLink = null })

        assertEquals(listOf(Destinations.Login), routes())
        assertEquals(1, consumed)
        assertNull(deepLink)
    }

    @Test
    fun signOut_returnsToLoginWithEmptyBackStack() {
        show(SIGNED_IN)
        compose.onNodeWithText("Projects").performClick()
        compose.runOnIdle { nav.navigate(Destinations.projectDetail(3)) }

        session = SessionState.SignedOut
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Login), routes())
    }

    @Test
    fun signOut_forgetsSavedTabStacks() {
        show(SIGNED_IN)
        compose.onNodeWithText("Logs").performClick()
        compose.runOnIdle { nav.navigate(Destinations.logDetail(7)) }
        compose.runOnIdle { nav.navigateToTab(Destinations.Home) }

        session = SessionState.SignedOut
        compose.waitForIdle()
        session = SIGNED_IN
        compose.waitForIdle()
        compose.onNodeWithText("Logs").performClick()
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home, Destinations.Logs), routes())
    }

    @Test
    fun signIn_leavesLoginForHome() {
        show(SessionState.SignedOut)

        session = SIGNED_IN
        compose.waitForIdle()

        assertEquals(listOf(Destinations.Home), routes())
    }

    @Test
    fun bottomBar_hiddenOnSubScreens() {
        show(SIGNED_IN)
        compose.onNodeWithText("Logs").assertIsDisplayed()

        compose.runOnIdle { nav.navigate(Destinations.logDetail(1)) }
        compose.waitForIdle()

        compose.onNodeWithText("Logs").assertDoesNotExist()
    }

    @Test
    fun addMember_isNotTakenForAnEditMemberIdx() {
        show(SIGNED_IN)

        compose.runOnIdle { nav.navigate(Destinations.AddMember) }
        compose.waitForIdle()
        assertEquals(Destinations.AddMember, nav.currentDestination?.route)

        compose.runOnIdle { nav.navigate(Destinations.editMember(5)) }
        compose.waitForIdle()
        assertEquals(Destinations.EditMemberPattern, nav.currentDestination?.route)
        assertEquals(5, nav.currentBackStackEntry?.arguments?.getInt(Destinations.MemberIdxArg))
    }

    @Test
    fun projectCreate_isNotTakenForAProjectId() {
        show(SIGNED_IN)

        compose.runOnIdle { nav.navigate(Destinations.ProjectCreate) }
        compose.waitForIdle()
        assertEquals(Destinations.ProjectCreate, nav.currentDestination?.route)

        compose.runOnIdle { nav.navigate(Destinations.projectSettings(9)) }
        compose.waitForIdle()
        assertEquals(Destinations.ProjectSettingsPattern, nav.currentDestination?.route)
        assertEquals(9, nav.currentBackStackEntry?.arguments?.getInt(Destinations.ProjectIdArg))
    }

    private fun show(
        initial: SessionState,
        deepLinkErrorId: Int? = null,
        onConsumed: () -> Unit = {},
    ) {
        session = initial
        deepLink = deepLinkErrorId
        compose.setContent {
            nav = rememberTestNavController()
            LogFlareTheme(darkTheme = false) {
                AppNavHost(
                    session = session,
                    deepLinkErrorId = deepLink,
                    onDeepLinkConsumed = onConsumed,
                    navController = nav,
                    screen = { route, _, _ -> Text("screen:$route") },
                )
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun rememberTestNavController(): TestNavHostController {
        val context = LocalContext.current
        return remember {
            TestNavHostController(context).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
        }
    }

    /** Back stack routes, without the graph entry. */
    private fun routes(): List<String?> = compose.runOnIdle {
        nav.backStack
            .map { it.destination }
            .filter { it.parent != null }
            .map { it.route }
    }

    private companion object {
        val SIGNED_IN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 100,
            key = SessionKey("aaaaaaaaaaaaaaaa"),
        )
    }
}
