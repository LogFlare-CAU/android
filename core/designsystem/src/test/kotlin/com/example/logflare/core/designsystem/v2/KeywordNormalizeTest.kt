package com.example.logflare.core.designsystem.v2

import com.example.logflare.core.designsystem.v2.components.KeywordResult
import com.example.logflare.core.designsystem.v2.components.normalizeKeyword
import org.junit.Assert.assertEquals
import org.junit.Test

class KeywordNormalizeTest {
    @Test fun trimsAndAdds() {
        assertEquals(KeywordResult.Added("pool"), normalizeKeyword("  pool ", emptyList()))
    }

    @Test fun blankIsBlank() {
        assertEquals(KeywordResult.Blank, normalizeKeyword("", emptyList()))
        assertEquals(KeywordResult.Blank, normalizeKeyword("   ", emptyList()))
    }

    @Test fun duplicateIsCaseInsensitive() {
        assertEquals(KeywordResult.Duplicate, normalizeKeyword("Pool", listOf("pool")))
    }
}
