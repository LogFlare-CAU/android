package com.logflare.qa

import com.logflare.qa.image.CompareResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class QaResultPresenterTest {
    @Test
    fun changedRatioUsesDotDecimalUnderCommaDecimalDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(
                "RESULT Changed ratio=0.00020000 count=2",
                QaResultPresenter.format(
                    CompareResult.Changed(changedPixels = 2, changedRatio = 0.0002),
                ),
            )
        } finally {
            Locale.setDefault(original)
        }
    }
}
