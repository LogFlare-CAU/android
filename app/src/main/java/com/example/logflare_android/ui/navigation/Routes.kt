package com.example.logflare_android.ui.navigation

/**
 * Navigation routes with hierarchical structure.
 * RootGraph: Auth vs Main
 * MainGraph (BottomBar): Home, Logs, Projects, MyPage
 */
sealed class Route(val path: String) {
    // Root level
    data object Auth : Route("auth")
    data object Main : Route("main")

    // Main graph - Bottom navigation items (ordered as per spec)
    data object Home : Route("home")
    data object Logs : Route("logs")
    data object Projects : Route("projects")
    data object MyPage : Route("mypage")

    // Create project screen
    data object ProjectCreate : Route("projects/create")

    // Detail screens with arguments
    data object ProjectDetail : Route("project/{projectId}") {
        fun createRoute(projectId: Int) = "project/$projectId"
    }

    data object ProjectSettings : Route("project/{projectId}/settings") {
        fun createRoute(projectId: Int) = "project/$projectId/settings"
    }

    data object LogDetail : Route("log/{projectId}") {
        fun createRoute(projectId: Int) = "log/$projectId"
    }
}
