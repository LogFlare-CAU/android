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
import com.logflare.android.feature.mypage.AddMemberRoute
import com.logflare.android.feature.mypage.EditMemberRoute
import com.logflare.android.feature.mypage.MyRoute
import com.logflare.android.feature.project.ProjectCreateRoute
import com.logflare.android.feature.project.ProjectSettingsRoute
import com.logflare.android.feature.project.ProjectsRoute
import com.logflare.android.feature.projectdetail.ProjectDetailRoute

/** Draws the screen registered under route pattern `route`; tests pass a stub. */
typealias DestinationContent =
    @Composable (route: String, entry: NavBackStackEntry, navController: NavHostController) -> Unit

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
            navController.showLogin()
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

/**
 * Signed out: Login alone on the back stack; a no-op when already there. Saved tab stacks belong
 * to the old session and are dropped.
 */
internal fun NavHostController.showLogin() {
    if (currentDestination?.route == Destinations.Login) return
    navigate(Destinations.Login) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
    Destinations.Tabs.forEach { clearBackStack(it) }
}

/** Login → Home, dropping Login; a no-op when the session redirect already did it. */
private fun NavHostController.leaveLogin() {
    if (currentDestination?.route != Destinations.Login) return
    navigate(Destinations.Home) {
        popUpTo(Destinations.Login) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * After a project was deleted: back to the Projects tab root, dropping its detail and settings.
 * Opened from another tab, that tab is first popped to its root so it doesn't keep the project,
 * and the Projects tab's saved stack is dropped so it opens at its root.
 */
internal fun NavHostController.popToProjectList() {
    if (popBackStack(Destinations.Projects, inclusive = false)) return
    popBackStack(Destinations.Home, inclusive = false)
    clearBackStack(Destinations.Projects)
    navigateToTab(Destinations.Projects)
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
        Destinations.My -> MyRoute(
            onAddMember = { nav.navigate(Destinations.AddMember) },
            onEditMember = { nav.navigate(Destinations.editMember(it)) },
            // The session redirect above usually got there first; this covers it either way.
            onSignedOut = { nav.showLogin() },
        )
        Destinations.ProjectCreate -> ProjectCreateRoute(
            onBack = back,
            // Replaces the create screen, so Back from the project lands on the list it came from.
            onOpenProject = {
                nav.navigate(Destinations.projectDetail(it)) {
                    popUpTo(Destinations.ProjectCreate) { inclusive = true }
                }
            },
        )
        Destinations.ProjectDetailPattern -> ProjectDetailRoute(
            onBack = back,
            onOpenSettings = { nav.navigate(Destinations.projectSettings(it)) },
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
        )
        Destinations.ProjectSettingsPattern -> ProjectSettingsRoute(
            onBack = back,
            onDeleted = { nav.popToProjectList() },
        )
        Destinations.LogDetailPattern -> LogDetailRoute(
            onBack = back,
            onOpenProject = { nav.navigate(Destinations.projectDetail(it)) },
            onOpenLog = { nav.navigate(Destinations.logDetail(it)) },
        )
        Destinations.AddMember -> AddMemberRoute(onBack = back)
        Destinations.EditMemberPattern -> EditMemberRoute(onBack = back)
    }
}
