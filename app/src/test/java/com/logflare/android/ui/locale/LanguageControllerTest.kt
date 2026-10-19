package com.logflare.android.ui.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageControllerTest {

    @Test
    fun fromTags_mapsByLanguageOnly() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US,ko"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromTags("ko-KR"))
        // A locale the app has no strings for (set from system settings, say) falls back to System.
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("fr-FR"))
    }

    @Test
    fun sync_picksUpAChangeMadeOutsideTheApp() {
        var stored = ""
        val controller = LanguageController(read = { stored }, apply = { stored = it })
        assertEquals(AppLanguage.SYSTEM, controller.language.value)

        stored = "ko"
        controller.sync()
        assertEquals(AppLanguage.KOREAN, controller.language.value)
    }

    @Test
    fun set_appliesTheLanguageTag() {
        val applied = mutableListOf<String>()
        val controller = LanguageController(read = { "" }, apply = { applied += it })
        controller.set(AppLanguage.ENGLISH)
        controller.set(AppLanguage.SYSTEM)
        assertEquals(listOf("en", ""), applied)
        assertEquals(AppLanguage.SYSTEM, controller.language.value)
    }
}
