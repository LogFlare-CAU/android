package com.logflare.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualQaTagsTest {
    @Test
    fun requiredStaticTagsMatchMaestroContract() {
        assertEquals("app_root", VisualQaTags.AppRoot)
        assertEquals("login_screen", VisualQaTags.Login)
        assertEquals("home_screen", VisualQaTags.Home)
        assertEquals("logs_list", VisualQaTags.Logs)
        assertEquals("log_detail", VisualQaTags.LogDetail)
        assertEquals("projects_list", VisualQaTags.Projects)
        assertEquals("project_create", VisualQaTags.ProjectCreate)
        assertEquals("project_settings", VisualQaTags.ProjectSettings)
        assertEquals("project_detail", VisualQaTags.ProjectDetail)
        assertEquals("my_page", VisualQaTags.MyPage)
        assertEquals("add_member", VisualQaTags.AddMember)
        assertEquals("edit_member", VisualQaTags.EditMember)
        assertEquals("logout_confirmation", VisualQaTags.Logout)
        assertEquals("nav_home", VisualQaTags.NavHome)
        assertEquals("nav_logs", VisualQaTags.NavLogs)
        assertEquals("nav_projects", VisualQaTags.NavProjects)
        assertEquals("nav_my_page", VisualQaTags.NavMyPage)
        assertEquals("open_project_settings", VisualQaTags.OpenProjectSettings)
        assertEquals("create_project", VisualQaTags.CreateProject)
        assertEquals("logout", VisualQaTags.LogoutAction)
        assertEquals("confirm_logout", VisualQaTags.ConfirmLogout)
        assertEquals("navigate_back", VisualQaTags.NavigateBack)
        assertEquals("filter_log_level", VisualQaTags.FilterLogLevel)
        assertEquals("filter_projects", VisualQaTags.FilterProjects)
        assertEquals("filter_sort", VisualQaTags.FilterSort)
        assertEquals("state_loading", VisualQaTags.Loading)
        assertEquals("state_empty", VisualQaTags.Empty)
        assertEquals("state_error", VisualQaTags.Error)
    }

    @Test
    fun dynamicFactoriesProduceStableResourceIds() {
        assertEquals("project_card_101", VisualQaTags.projectCard(101))
        assertEquals("project_card_202", VisualQaTags.projectCard(202))
        assertEquals("log_card_5001", VisualQaTags.logCard(5001))
        assertEquals("log_card_5002", VisualQaTags.logCard(5002))
        assertEquals("member_row_qa-member", VisualQaTags.memberRow("qa-member"))
    }

    @Test
    fun sanitizeMakesArbitraryInputResourceIdSafe() {
        assertEquals("member_row_qa_member", VisualQaTags.memberRow("qa member"))
        assertEquals("member_row_user_name", VisualQaTags.memberRow("user@name!"))
        assertEquals("member_row_a_b_c", VisualQaTags.memberRow("a..b//c"))
        val weird = VisualQaTags.memberRow("  Foo!!Bar  ")
        assertTrue(weird.startsWith("member_row_"))
        assertFalse(weird.contains(" "))
        assertFalse(weird.contains("!"))
        assertTrue(weird.all { it.isLetterOrDigit() || it == '_' || it == '-' })
    }
}
