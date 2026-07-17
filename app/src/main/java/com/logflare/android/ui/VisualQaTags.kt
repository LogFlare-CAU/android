package com.logflare.android.ui

object VisualQaTags {
    const val AppRoot = "app_root"
    const val Login = "login_screen"
    const val Home = "home_screen"
    const val Logs = "logs_list"
    const val LogDetail = "log_detail"
    const val Projects = "projects_list"
    const val ProjectCreate = "project_create"
    const val ProjectSettings = "project_settings"
    const val ProjectDetail = "project_detail"
    const val MyPage = "my_page"
    const val AddMember = "add_member"
    const val EditMember = "edit_member"
    const val Logout = "logout_confirmation"
    const val NavHome = "nav_home"
    const val NavLogs = "nav_logs"
    const val NavProjects = "nav_projects"
    const val NavMyPage = "nav_my_page"
    const val OpenProjectSettings = "open_project_settings"
    const val CreateProject = "create_project"
    const val LogoutAction = "logout"
    const val ConfirmLogout = "confirm_logout"
    const val NavigateBack = "navigate_back"
    const val FilterLogLevel = "filter_log_level"
    const val FilterProjects = "filter_projects"
    const val FilterSort = "filter_sort"
    const val Loading = "state_loading"
    const val Empty = "state_empty"
    const val Error = "state_error"

    fun projectCard(projectId: Int): String = "project_card_$projectId"

    fun logCard(logId: Int): String = "log_card_$logId"

    fun memberRow(username: String): String = "member_row_${sanitizeResourceId(username)}"

    fun sanitizeResourceId(raw: String): String {
        val collapsed = raw.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\-]+"), "_")
            .trim('_')
        return collapsed.ifBlank { "unknown" }
    }
}
