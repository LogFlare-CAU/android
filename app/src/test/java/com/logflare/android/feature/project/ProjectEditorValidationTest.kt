package com.logflare.android.feature.project

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectEditorValidationTest {

    @Test
    fun projectName_acceptsHangulAndLatin() {
        assertTrue(ProjectEditorValidation.isProjectNameValid("프로젝트 A"))
        assertTrue(ProjectEditorValidation.isProjectNameValid("Alpha 1"))
    }

    @Test
    fun keyword_acceptsAlphanumericAndSymbols() {
        assertTrue(ProjectEditorValidation.isKeywordValid("err-1"))
        assertFalse(ProjectEditorValidation.isKeywordValid("한글"))
    }
}
