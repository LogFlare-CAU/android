package com.logflare.android.visual

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.mypage.MyActions
import com.logflare.android.feature.mypage.MyContent
import com.logflare.android.feature.mypage.MySheet
import com.logflare.android.feature.mypage.MyUiState
import com.logflare.android.push.GlobalAlert
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test

class MySnapshots : DsSnapshotTest() {

    @Composable
    private fun My(state: MyUiState, scrolledToEnd: Boolean = false) {
        // Starts past the end; the scroll state clamps to the bottom once laid out.
        val scroll = remember { ScrollState(if (scrolledToEnd) Int.MAX_VALUE else 0) }
        MyContent(state = state, actions = MyActions(), scrollState = scroll)
    }

    @Test fun my_admin_light() = capture("my_admin", false) { My(MyFixtures.admin) }
    @Test fun my_admin_dark() = capture("my_admin", true) { My(MyFixtures.admin) }
    @Test fun my_admin_no_members_light() = capture("my_admin_no_members", false) { My(MyFixtures.adminAlone) }
    @Test fun my_admin_no_members_dark() = capture("my_admin_no_members", true) { My(MyFixtures.adminAlone) }
    @Test fun my_member_light() = capture("my_member", false) { My(MyFixtures.member) }
    @Test fun my_member_dark() = capture("my_member", true) { My(MyFixtures.member) }
    @Test fun my_appearance_sheet_light() = capture("my_appearance_sheet", false) { My(MyFixtures.appearanceSheet) }
    @Test fun my_appearance_sheet_dark() = capture("my_appearance_sheet", true) { My(MyFixtures.appearanceSheet) }
    @Test fun my_push_sheet_light() = capture("my_push_sheet", false) { My(MyFixtures.pushSheet) }
    @Test fun my_push_sheet_dark() = capture("my_push_sheet", true) { My(MyFixtures.pushSheet) }
    @Test fun my_logout_sheet_light() = capture("my_logout_sheet", false) { My(MyFixtures.logoutSheet) }
    @Test fun my_logout_sheet_dark() = capture("my_logout_sheet", true) { My(MyFixtures.logoutSheet) }
    @Test fun my_scrolled_bottom_light() = capture("my_scrolled_bottom", false) { My(MyFixtures.allMembers, scrolledToEnd = true) }
    @Test fun my_scrolled_bottom_dark() = capture("my_scrolled_bottom", true) { My(MyFixtures.allMembers, scrolledToEnd = true) }
}

object MyFixtures {
    val admin = MyUiState(
        me = Member(1, "admin", 100),
        serverHost = "logs.example.com",
        theme = ThemePreference.SYSTEM,
        globalAlert = GlobalAlert.ErrorAndAbove,
        quietHours = true,
        members = listOf(
            Member(2, "jihyun", 80),
            Member(3, "devops-bot", 0),
            Member(4, "minsu", 0),
            Member(5, "yuna", 0),
        ),
        canManageMembers = true,
        version = "2.0.0",
    )

    val member = MyUiState(
        me = Member(4, "minsu", 0),
        serverHost = "logs.example.com",
        globalAlert = GlobalAlert.WarningAndAbove,
        version = "2.0.0",
    )

    /** An admin who is the server's only user. */
    val adminAlone = admin.copy(members = emptyList())

    val appearanceSheet = admin.copy(theme = ThemePreference.DARK, sheet = MySheet.Appearance)

    val pushSheet = admin.copy(sheet = MySheet.PushAlerts)

    val logoutSheet = admin.copy(sheet = MySheet.Logout)

    /** Enough members that the page overflows, so the bottom is only reached by scrolling. */
    val allMembers = admin.copy(
        showAllMembers = true,
        members = admin.members + (6..11).map { Member(it, "member-$it", 0) },
    )
}
