package com.logflare.android.feature.mypage

import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.feature.project.NameProblem

/** What the server accepts as a username (3–32 characters), kept to plain ASCII. */
private val MEMBER_NAME = Regex("^[A-Za-z0-9._-]{3,32}$")
private const val MIN_PASSWORD_LENGTH = 8
private const val ADMIN_PERMISSION = 80
private const val SUPER_ADMIN_PERMISSION = 100

/**
 * Checks a member name as typed. [taken] says whether another user already has a name (the
 * caller compares case-insensitively); [current] is the member's own name when editing, which
 * doesn't count as taken.
 */
fun checkMemberName(name: String, taken: (String) -> Boolean, current: String? = null): NameCheck = when {
    name.isEmpty() -> NameCheck.Empty
    !MEMBER_NAME.matches(name) -> NameCheck.Invalid(NameProblem.Format)
    !name.equals(current, ignoreCase = true) && taken(name) -> NameCheck.Invalid(NameProblem.Duplicate)
    else -> NameCheck.Valid
}

data class PasswordRules(val length: Boolean, val letter: Boolean, val number: Boolean) {
    val ok: Boolean get() = length && letter && number
}

fun passwordRules(pw: String): PasswordRules = PasswordRules(
    length = pw.length >= MIN_PASSWORD_LENGTH,
    letter = pw.any { it in 'a'..'z' || it in 'A'..'Z' },
    number = pw.any { it in '0'..'9' },
)

/** The roles the app can give; Super Admin can't be assigned through the API. */
enum class MemberRole(val permission: Int) {
    Member(0),
    Admin(ADMIN_PERMISSION),
    ;

    companion object {
        fun of(permission: Int): MemberRole = if (permission >= ADMIN_PERMISSION) Admin else Member
    }
}

/**
 * Whether the signed-in user ([myIdx], [myPermission]) may edit or delete [target]: only users
 * with a lower permission than their own, never a Super Admin and never themselves (so nobody
 * demotes or deletes their own account by accident).
 */
fun canEditMember(myIdx: Int?, myPermission: Int, target: Member): Boolean =
    myPermission >= ADMIN_PERMISSION &&
        target.permission < SUPER_ADMIN_PERMISSION &&
        target.permission < myPermission &&
        target.idx != myIdx
