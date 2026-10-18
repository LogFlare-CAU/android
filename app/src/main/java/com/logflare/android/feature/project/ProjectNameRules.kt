package com.logflare.android.feature.project

import com.example.logflare.core.designsystem.v2.components.maskToken

sealed interface NameCheck {
    data object Empty : NameCheck
    data object Valid : NameCheck
    data class Invalid(val reason: NameProblem) : NameCheck
}

/** [Format] is for member names only: outside the allowed characters or 3–32 length. */
enum class NameProblem { Spaces, Characters, Duplicate, TooLong, Format }

private const val MAX_NAME_LENGTH = 64
private val FORBIDDEN_NAME_CHARS = setOf('<', '>', '/', '\\')

/**
 * Checks a project name as typed (not trimmed: surrounding spaces are an error). [existing] are
 * the names already taken, compared case-insensitively; [current] is the project's own name when
 * renaming, which doesn't count as taken.
 */
fun checkProjectName(name: String, existing: Collection<String>, current: String? = null): NameCheck = when {
    name.isEmpty() -> NameCheck.Empty
    name != name.trim() -> NameCheck.Invalid(NameProblem.Spaces)
    name.any { it in FORBIDDEN_NAME_CHARS } -> NameCheck.Invalid(NameProblem.Characters)
    existing.any { it.equals(name, ignoreCase = true) && !it.equals(current, ignoreCase = true) } ->
        NameCheck.Invalid(NameProblem.Duplicate)
    name.length > MAX_NAME_LENGTH -> NameCheck.Invalid(NameProblem.TooLong)
    else -> NameCheck.Valid
}

/** The "send your first log" example for [projectName], with the token masked. */
fun curlSnippet(serverUrl: String, projectName: String, token: String): String =
    "curl -X POST ${serverUrl.trimEnd('/')}/log/error \\\n" +
        "  -H \"Project: $projectName\" \\\n" +
        "  -H \"ProjectKey: Bearer ${maskToken(token)}\" \\\n" +
        "  -d '{\"level\":\"ERROR\",\"message\":\"…\"}'"
