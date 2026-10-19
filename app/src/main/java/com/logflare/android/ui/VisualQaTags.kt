package com.logflare.android.ui

object VisualQaTags {
    const val AppRoot = "app_root"
    const val Login = "login_screen"
    const val LoginServerUrl = "login_server_url"
    const val LoginUsername = "login_username"
    const val LoginPassword = "login_password"
    const val LoginSubmit = "login_submit"
    const val LoginModes = "login_modes"
    const val LoginScan = "login_scan_qr"
    const val LoginAccessConfirm = "login_access_confirm"
    const val LoginAccessClient = "login_access_client"
    const val LoginAccessCard = "login_access_card"
    const val LoginManualEntry = "login_manual_entry"
    const val LoginNoModes = "login_no_modes"
    const val LoginScanScreen = "login_scan_screen"
    const val LoginScanError = "login_scan_error"
    const val LoginScanCancel = "login_scan_cancel"
    const val LoginManualScreen = "login_manual_screen"
    const val LoginManualId = "login_manual_id"
    const val LoginManualServer = "login_manual_server"
    const val LoginPasswordBlocked = "login_password_blocked"
    const val LoginManualSecret = "login_manual_secret"
    const val LoginManualContinue = "login_manual_continue"
    const val LoginAccessKeep = "login_access_keep"
    const val LoginAccessError = "login_access_error"
    const val LoginAccessSubmit = "login_access_submit"
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
    const val NavHome = "nav_home"
    const val NavLogs = "nav_logs"
    const val NavProjects = "nav_projects"
    const val NavMyPage = "nav_my_page"
    const val OpenProjectSettings = "open_project_settings"
    const val CreateProject = "create_project"
    const val LogoutAction = "logout"
    const val FilterLogLevel = "filter_log_level"
    const val FilterProjects = "filter_projects"
    const val FilterSort = "filter_sort"
    const val Empty = "state_empty"

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
