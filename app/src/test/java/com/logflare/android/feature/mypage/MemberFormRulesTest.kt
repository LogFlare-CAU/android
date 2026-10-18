package com.logflare.android.feature.mypage

import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberFormRulesTest {

    private val taken = setOf("admin", "jihyun")
    private fun check(name: String, current: String? = null) =
        checkMemberName(name, { it.lowercase() in taken }, current)

    @Test
    fun checkMemberName_empty() {
        assertEquals(NameCheck.Empty, check(""))
    }

    @Test
    fun checkMemberName_format() {
        val format = NameCheck.Invalid(NameProblem.Format)
        assertEquals(format, check("ab"))
        assertEquals(format, check("a".repeat(33)))
        assertEquals(format, check("min su"))
        assertEquals(format, check(" minsu"))
        assertEquals(format, check("min@su"))
        assertEquals(format, check("민수민수"))
        assertEquals(NameCheck.Valid, check("abc"))
        assertEquals(NameCheck.Valid, check("a".repeat(32)))
        assertEquals(NameCheck.Valid, check("dev.ops_bot-2"))
    }

    @Test
    fun checkMemberName_taken() {
        assertEquals(NameCheck.Invalid(NameProblem.Duplicate), check("Admin"))
        assertEquals(NameCheck.Valid, check("minsu"))
    }

    @Test
    fun checkMemberName_currentNameIsNotTaken() {
        assertEquals(NameCheck.Valid, check("jihyun", current = "jihyun"))
        assertEquals(NameCheck.Valid, check("JiHyun", current = "jihyun"))
        assertEquals(NameCheck.Invalid(NameProblem.Duplicate), check("admin", current = "jihyun"))
    }

    @Test
    fun passwordRules_eachRule() {
        assertEquals(PasswordRules(length = false, letter = false, number = false), passwordRules(""))
        assertEquals(PasswordRules(length = true, letter = true, number = false), passwordRules("abcdefgh"))
        assertEquals(PasswordRules(length = true, letter = false, number = true), passwordRules("12345678"))
        assertEquals(PasswordRules(length = false, letter = true, number = true), passwordRules("abc123"))
    }

    @Test
    fun passwordRules_ok() {
        assertTrue(passwordRules("minsupass1").ok)
        assertTrue(passwordRules("1234567a").ok)
        assertFalse(passwordRules("minsupass").ok)
        assertFalse(passwordRules("pass1").ok)
    }

    @Test
    fun memberRole_fromPermission() {
        assertEquals(MemberRole.Member, MemberRole.of(0))
        assertEquals(MemberRole.Member, MemberRole.of(79))
        assertEquals(MemberRole.Admin, MemberRole.of(80))
        assertEquals(0, MemberRole.Member.permission)
        assertEquals(80, MemberRole.Admin.permission)
    }

    @Test
    fun canEditMember_neverSuperAdminOrSelf_onlyLowerRoles() {
        val member = Member(8, "minsu", 0)
        val admin = Member(9, "jihyun", 80)
        val superAdmin = Member(1, "admin", 100)
        // A Super Admin manages everyone but Super Admins and themselves.
        assertTrue(canEditMember(myIdx = 1, myPermission = 100, target = member))
        assertTrue(canEditMember(myIdx = 1, myPermission = 100, target = admin))
        assertFalse(canEditMember(myIdx = 1, myPermission = 100, target = superAdmin))
        assertFalse(canEditMember(myIdx = 2, myPermission = 100, target = superAdmin))
        // An Admin manages members only, never other admins.
        assertTrue(canEditMember(myIdx = 7, myPermission = 80, target = member))
        assertFalse(canEditMember(myIdx = 7, myPermission = 80, target = admin))
        assertFalse(canEditMember(myIdx = 9, myPermission = 100, target = admin))
        // Members manage no one.
        assertFalse(canEditMember(myIdx = 10, myPermission = 0, target = member))
    }
}
