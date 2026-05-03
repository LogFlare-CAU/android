package com.logflare.android.feature.log

import com.example.logflare.core.model.ErrorlogDTO
import com.logflare.android.enums.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorLogsUiFilterTest {

    private fun row(level: String) = ErrorlogDTO(
        id = 1,
        project_id = 1,
        errortype = null,
        message = "m",
        level = level,
        timestamp = "t",
    )

    @Test
    fun emptyFilter_returnsAll() {
        val logs = listOf(row("ERROR"), row("INFO"))
        assertEquals(logs, logs.filterByLogLevels(emptyList()))
    }

    @Test
    fun filtersCaseInsensitive() {
        val logs = listOf(row("error"), row("INFO"))
        val out = logs.filterByLogLevels(listOf(LogLevel.ERROR))
        assertEquals(1, out.size)
        assertEquals("error", out[0].level)
    }
}
