package com.example.logflare_android.ui.navigation

sealed class Route(val path: String) {
    data object Auth : Route("auth")
    data object Home : Route("home")
    data object Projects : Route("projects")
    data object Logs : Route("logs")
    data object Settings : Route("settings")
}
