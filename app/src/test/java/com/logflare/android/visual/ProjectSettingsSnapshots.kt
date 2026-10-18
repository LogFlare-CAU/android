package com.logflare.android.visual

import androidx.compose.runtime.Composable
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.store.model.LogFileRef
import com.logflare.android.data.store.model.Member
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.feature.project.MemberAccess
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.ProjectSettingsActions
import com.logflare.android.feature.project.ProjectSettingsContent
import com.logflare.android.feature.project.ProjectSettingsUiState
import com.logflare.android.feature.project.SettingsSheet
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test

class ProjectSettingsSnapshots : DsSnapshotTest() {

    @Composable
    private fun Settings(state: ProjectSettingsUiState) = ProjectSettingsContent(state = state, actions = ProjectSettingsActions())

    @Test fun project_settings_admin_light() = capture("project_settings_admin", false) { Settings(ProjectSettingsFixtures.admin) }
    @Test fun project_settings_admin_dark() = capture("project_settings_admin", true) { Settings(ProjectSettingsFixtures.admin) }
    @Test fun project_settings_member_light() = capture("project_settings_member", false) { Settings(ProjectSettingsFixtures.member) }
    @Test fun project_settings_member_dark() = capture("project_settings_member", true) { Settings(ProjectSettingsFixtures.member) }
    @Test fun project_settings_alert_sheet_light() = capture("project_settings_alert_sheet", false) { Settings(ProjectSettingsFixtures.alertSheet) }
    @Test fun project_settings_alert_sheet_dark() = capture("project_settings_alert_sheet", true) { Settings(ProjectSettingsFixtures.alertSheet) }
    @Test fun project_settings_delete_sheet_light() = capture("project_settings_delete_sheet", false) { Settings(ProjectSettingsFixtures.deleteSheet) }
    @Test fun project_settings_delete_sheet_dark() = capture("project_settings_delete_sheet", true) { Settings(ProjectSettingsFixtures.deleteSheet) }
}

object ProjectSettingsFixtures {
    private val project = ProjectItem(
        id = 1,
        name = "api-server",
        logFiles = listOf(LogFileRef(1, "app.log")),
        errorCount24h = 3,
        warningCount24h = 12,
        lastErrorAt = null,
    )

    val admin = ProjectSettingsUiState(
        project = project,
        loading = false,
        isAdmin = true,
        alertLevel = LfLevel.Warning,
        mutedKeywords = listOf("healthcheck", "favicon.ico"),
        memberCount = 3,
        members = listOf(
            MemberAccess(Member(1, "admin", 100), hasAccess = true, locked = true),
            MemberAccess(Member(2, "jihyun", 80), hasAccess = true, locked = false),
            MemberAccess(Member(3, "devops-bot", 0), hasAccess = true, locked = false),
            MemberAccess(Member(4, "minsu", 0), hasAccess = false, locked = false),
        ),
        nameCheck = NameCheck.Valid,
    )

    val member = ProjectSettingsUiState(
        project = project,
        loading = false,
        isAdmin = false,
        alertLevel = LfLevel.Error,
    )

    val alertSheet = admin.copy(sheet = SettingsSheet.AlertLevel)

    val deleteSheet = admin.copy(sheet = SettingsSheet.Delete, deleteConfirmText = "api-ser")
}
