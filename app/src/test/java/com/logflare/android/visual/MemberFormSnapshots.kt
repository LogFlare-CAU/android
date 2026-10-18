package com.logflare.android.visual

import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.mypage.AddMemberActions
import com.logflare.android.feature.mypage.AddMemberContent
import com.logflare.android.feature.mypage.AddMemberUiState
import com.logflare.android.feature.mypage.AddedMember
import com.logflare.android.feature.mypage.EditLoad
import com.logflare.android.feature.mypage.EditMemberActions
import com.logflare.android.feature.mypage.EditMemberContent
import com.logflare.android.feature.mypage.EditMemberUiState
import com.logflare.android.feature.mypage.MemberLock
import com.logflare.android.feature.mypage.MemberRole
import com.logflare.android.feature.mypage.passwordRules
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem
import com.logflare.android.visual.ds.DsSnapshotTest
import org.junit.Test

class MemberFormSnapshots : DsSnapshotTest() {

    private fun add(name: String, state: AddMemberUiState, dark: Boolean) =
        capture(name, dark) { AddMemberContent(state = state, actions = AddMemberActions()) }

    private fun edit(name: String, state: EditMemberUiState, dark: Boolean) =
        capture(name, dark) { EditMemberContent(state = state, actions = EditMemberActions()) }

    @Test fun member_add_empty_light() = add("member_add_empty", MemberFormFixtures.addEmpty, false)
    @Test fun member_add_empty_dark() = add("member_add_empty", MemberFormFixtures.addEmpty, true)
    @Test fun member_add_valid_light() = add("member_add_valid", MemberFormFixtures.addValid, false)
    @Test fun member_add_valid_dark() = add("member_add_valid", MemberFormFixtures.addValid, true)
    @Test fun member_add_taken_light() = add("member_add_taken", MemberFormFixtures.addTaken, false)
    @Test fun member_add_taken_dark() = add("member_add_taken", MemberFormFixtures.addTaken, true)
    @Test fun member_add_success_light() = add("member_add_success", MemberFormFixtures.addSuccess, false)
    @Test fun member_add_success_dark() = add("member_add_success", MemberFormFixtures.addSuccess, true)
    @Test fun member_edit_dirty_light() = edit("member_edit_dirty", MemberFormFixtures.editDirty, false)
    @Test fun member_edit_dirty_dark() = edit("member_edit_dirty", MemberFormFixtures.editDirty, true)
    @Test fun member_edit_delete_sheet_light() = edit("member_edit_delete_sheet", MemberFormFixtures.editDeleteSheet, false)
    @Test fun member_edit_delete_sheet_dark() = edit("member_edit_delete_sheet", MemberFormFixtures.editDeleteSheet, true)
    @Test fun member_edit_locked_light() = edit("member_edit_locked", MemberFormFixtures.editLocked, false)
    @Test fun member_edit_locked_dark() = edit("member_edit_locked", MemberFormFixtures.editLocked, true)
}

object MemberFormFixtures {
    val addEmpty = AddMemberUiState()

    val addValid = AddMemberUiState(
        name = "minsu",
        nameCheck = NameCheck.Valid,
        password = "minsupass",
        passwordRules = passwordRules("minsupass"),
        role = MemberRole.Admin,
    )

    val addTaken = AddMemberUiState(
        name = "jihyun",
        nameCheck = NameCheck.Invalid(NameProblem.Duplicate),
        password = "minsupass1",
        passwordRules = passwordRules("minsupass1"),
    )

    val addSuccess = AddMemberUiState(
        name = "minsu",
        added = AddedMember("minsu", MemberRole.Member),
        serverHost = "logs.example.com",
    )

    private val jihyun = Member(9, "jihyun", 80)

    val editDirty = EditMemberUiState(
        load = EditLoad.Ready,
        member = jihyun,
        name = "jihyun.kim",
        nameCheck = NameCheck.Valid,
        password = "newpass12",
        passwordRules = passwordRules("newpass12"),
        role = MemberRole.Member,
    )

    val editDeleteSheet = EditMemberUiState(
        load = EditLoad.Ready,
        member = jihyun,
        name = "jihyun",
        nameCheck = NameCheck.Valid,
        role = MemberRole.Admin,
        deleteOpen = true,
    )

    val editLocked = EditMemberUiState(
        load = EditLoad.Ready,
        member = Member(1, "admin", 100),
        lock = MemberLock.SuperAdmin,
        name = "admin",
    )
}
