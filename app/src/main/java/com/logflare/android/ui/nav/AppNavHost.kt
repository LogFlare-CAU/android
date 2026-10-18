package com.logflare.android.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.logflare.android.data.session.SessionState
import com.logflare.android.feature.auth.LoginRoute
import com.logflare.android.feature.home.HomeRoute
import com.logflare.android.feature.log.LogDetailRoute
import com.logflare.android.feature.log.LogsRoute
import com.logflare.android.feature.mypage.AddMemberScreen
import com.logflare.android.feature.mypage.EditMemberScreen
import com.logflare.android.feature.mypage.LogoutScreen
import com.logflare.android.feature.mypage.MyPageScreen
import com.logflare.android.feature.project.ProjectCreateScreen
import com.logflare.android.feature.project.ProjectSettingsScreen
import com.logflare.android.feature.project.ProjectsRoute
import com.logflare.android.feature.projectdetail.ProjectDetailRoute

/** Draws the screen registered under route pattern `route`; tests pass a stub. */
typealias DestinationContent =
    @Composable (route: String, entry: NavBackStackEntry, navController: NavHostController) -> Unit

/** Old confirm screen behind My › Log out until Task 11 replaces it with a sheet. */
internal const val LegacyLogoutRoute = "my/logout"

/**
 * The app's single nav graph. Build it only once [session] is known (not Loading): the start
 * destination is taken from it once; later sign-ins and sign-outs move between Login and Home.
 *
 * [deepLinkErrorId] (a tapped push) opens that error's detail above Home, so Back lands on Home;
 * while signed out it is dropped. Either way [onDeepLinkConsumed] is called once it was handled.
 */
@Composable
fun AppNavHost(
    session: SessionState,
    deepLinkErrorId: Int?,
    modifier: Modifier = Modifier,
    onDeepLinkConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    screen: DestinationContent = { route, entry, nav -> LegacyDestination(route, entry, nav) },
) {
    val signedIn = session is SessionState.SignedIn
    val start = remember { if (signedIn) Destinations.Home else Destinations.Login }
    val currentSignedIn by rememberUpdatedState(signedIn)

    // Later transitions only; the first run finds the start destination already right.
    LaunchedEffect(signedIn) {
        val current = navController.currentDestination?.route ?: return@LaunchedEffect
        if (!signedIn && current != Destinations.Login) {
            navController.navigate(Destinations.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            // Saved tab stacks belong to the old session.
            Destinations.Tabs.forEach { navController.clearBackStack(it) }
        } else if (signedIn && current == Destinations.Login) {
            navController.leaveLogin()
        }
    }

    LaunchedEffect(deepLinkErrorId) {
        val errorId = deepLinkErrorId ?: return@LaunchedEffect
        if (currentSignedIn) navController.navigate(Destinations.logDetail(errorId))
        onDeepLinkConsumed()
    }

    MainTabsScaffold(navController, modifier) {
        NavHost(navController = navController, startDestination = start) {
            destination(Destinations.Login, navController, screen)
            destination(Destinations.Home, navController, screen)
            destination(Destinations.Logs, navController, screen)
            destination(Destinations.Projects, navController, screen)
            destination(Destinations.My, navController, screen)
            destination(Destinations.ProjectCreate, navController, screen)
            destination(Destinations.ProjectDetailPattern, navController, screen, Destinations.ProjectIdArg)
            destination(Destinations.ProjectSettingsPattern, navController, screen, Destinations.ProjectIdArg)
            destination(Destinations.LogDetailPattern, navController, screen, Destinations.ErrorIdArg)
            destination(Destinations.AddMember, navController, screen)
            destination(Destinations.EditMemberPattern, navController, screen, Destinations.MemberIdxArg)
            destination(LegacyLogoutRoute, navController, screen)
        }
    }
}

private fun NavGraphBuilder.destination(
    route: String,
    navController: NavHostController,
    screen: DestinationContent,
    intArg: String? = null,
) {
    composable(
        route = route,
        arguments = listOfNotNull(intArg?.let { navArgument(it) { type = NavType.IntType } }),
    ) { entry -> screen(route, entry, navController) }
}

/** Login → Home, dropping Login; a no-op when the session redirect already did it. */
private fun NavHostController.leaveLogin() {
    if (currentDestination?.route != Destinations.Login) return
    navigate(Destinations.Home) {
        popUpTo(Destinations.Login) { inclusive = true }
        launchSingleTop = true
    }
}

/** Pops back to the nearest list a deleted project could have been opened from. */
private fun NavHostController.popToProjectList() {
    if (!popBackStack(Destinations.Projects, inclusive = false)) {
        popBackStack(Destinations.Home, inclusive = false)
    }
}

/**
 * The pre-rework screens, until each screen's task replaces its branch. Sub-screens that relied on
 * the old shell's top bar have no Back button meanwhile (system Back works).
 */
@Composable
private fun LegacyDestination(route: String, entry: NavBackStackEntry, nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    when (route) {
        Destinations.Login -> LoginRoute(onSignedIn = { nav.leaveLogin() })
        Destinations.Home -> HomeRoute(
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
            onOpenProject = { nav.navigate(Destinations.projectDetail(it)) },
            onOpenLogs = { nav.openLogs(it.minLevel) },
            onOpenProjects = { nav.navigateToTab(Destinations.Projects) },
        )
        Destinations.Logs -> LogsRoute(
            entryState = entry.savedStateHandle,
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
        )
        Destinations.Projects -> ProjectsRoute(
            onOpenProject = { nav.navigate(Destinations.projectDetail(it)) },
            onCreateProject = { nav.navigate(Destinations.ProjectCreate) },
        )
        Destinations.My -> MyPageScreen(
            onLogout = { nav.navigate(LegacyLogoutRoute) },
            onAddMember = { nav.navigate(Destinations.AddMember) },
            onEditMember = { nav.navigate(Destinations.editMember(it)) },
        )
        Destinations.ProjectCreate -> ProjectCreateScreen(
            onCreated = {
                nav.popBackStack()
                nav.navigateToTab(Destinations.Projects)
            },
        )
        Destinations.ProjectDetailPattern -> ProjectDetailRoute(
            onBack = back,
            onOpenSettings = { nav.navigate(Destinations.projectSettings(it)) },
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
        )
        Destinations.ProjectSettingsPattern -> ProjectSettingsScreen(
            projectId = entry.arguments?.getInt(Destinations.ProjectIdArg) ?: return,
            onBack = back,
            onDelete = { nav.popToProjectList() },
        )
        Destinations.LogDetailPattern -> LogDetailRoute(
            onBack = back,
            onOpenProject = { nav.navigate(Destinations.projectDetail(it)) },
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
        )
        Destinations.AddMember -> AddMemberScreen(onBack = back)
        Destinations.EditMemberPattern -> EditMemberScreen(onBack = back)
        // Signing out flips the session; the redirect above then shows Login.
        LegacyLogoutRoute -> LogoutScreen(onBack = back, onLogout = {})
    }
}
