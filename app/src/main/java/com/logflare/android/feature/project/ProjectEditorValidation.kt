package com.logflare.android.feature.project

/**
 * Stateless validation for project name and exclusion keyword input.
 */
object ProjectEditorValidation {
    private val nameRegex = Regex("^[\\p{IsHangul}\\p{IsLatin}\\p{N}\\p{P}\\p{Zs}]+$")
    private val keywordRegex =
        "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?\\s]+$".toRegex()

    fun isProjectNameValid(name: String): Boolean = nameRegex.matches(name)

    fun isKeywordValid(keyword: String): Boolean = keywordRegex.matches(keyword)
}
