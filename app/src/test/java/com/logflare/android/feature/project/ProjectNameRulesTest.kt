package com.logflare.android.feature.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectNameRulesTest {

    private val existing = listOf("api-server", "Worker")

    private fun check(name: String, current: String? = null) = checkProjectName(name, existing, current)

    @Test fun checkProjectName_emptyIsEmpty() = assertEquals(NameCheck.Empty, check(""))

    @Test fun checkProjectName_validNames() {
        assertEquals(NameCheck.Valid, check("payment-api"))
        assertEquals(NameCheck.Valid, check("프로젝트 A"))
        assertEquals(NameCheck.Valid, check("Alpha 1"))
    }

    @Test fun checkProjectName_leadingOrTrailingSpace() {
        val expected = NameCheck.Invalid(NameProblem.Spaces)
        assertEquals(expected, check(" api"))
        assertEquals(expected, check("api "))
        assertEquals(expected, check("   "))
    }

    @Test fun checkProjectName_forbiddenCharacters() {
        val expected = NameCheck.Invalid(NameProblem.Characters)
        for (c in listOf("<", ">", "/", "\\")) assertEquals(expected, check("a${c}b"))
    }

    @Test fun checkProjectName_duplicateIsCaseInsensitive() {
        val expected = NameCheck.Invalid(NameProblem.Duplicate)
        assertEquals(expected, check("api-server"))
        assertEquals(expected, check("WORKER"))
    }

    @Test fun checkProjectName_currentNameIsNotADuplicateOfItself() {
        assertEquals(NameCheck.Valid, check("api-server", current = "api-server"))
        assertEquals(NameCheck.Valid, check("API-Server", current = "api-server"))
        assertTrue(check("worker", current = "api-server") is NameCheck.Invalid)
    }

    @Test fun checkProjectName_lengthLimit() {
        assertEquals(NameCheck.Valid, check("a".repeat(64)))
        assertEquals(NameCheck.Invalid(NameProblem.TooLong), check("a".repeat(65)))
    }

    @Test fun curlSnippet_usesServerUrlAndMaskedToken() {
        val token = "lf_0123456789abcdef"
        val snippet = curlSnippet("https://logs.example.com/", "api-server", token)
        assertTrue(snippet, snippet.startsWith("curl -X POST https://logs.example.com/log/error"))
        assertTrue(snippet, snippet.contains("\"Project: api-server\""))
        assertTrue(snippet, snippet.contains("ProjectKey: Bearer lf_012"))
        assertFalse(snippet, snippet.contains(token))
        assertEquals(snippet, curlSnippet("https://logs.example.com", "api-server", token))
    }
}
