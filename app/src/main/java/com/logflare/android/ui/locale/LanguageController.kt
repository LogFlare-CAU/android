package com.logflare.android.ui.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The app's own language; [SYSTEM] follows the device. [tag] is the per-app locale handed to Android. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    KOREAN("ko"),
    ;

    companion object {
        /** From per-app locale tags ("ko-KR", "en-US,ko"); none, or a language without strings, is [SYSTEM]. */
        fun fromTags(tags: String): AppLanguage {
            val language = tags.split(',').first().trim().substringBefore('-').lowercase()
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }
    }
}

/**
 * The app language, via AndroidX per-app locales: Android stores the choice (API 33+ itself, older
 * versions through AppCompat's `autoStoreLocales`) and recreates the activity in the new language.
 *
 * [MainActivity][com.logflare.android.MainActivity] calls [sync] on create, so a change made in
 * the system's app-language settings shows up here too.
 */
@Singleton
class LanguageController internal constructor(
    private val read: () -> String,
    private val apply: (String) -> Unit,
) {
    @Inject
    constructor() : this(
        read = { AppCompatDelegate.getApplicationLocales().toLanguageTags() },
        apply = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
    )

    private val _language = MutableStateFlow(AppLanguage.fromTags(read()))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun sync() {
        _language.value = AppLanguage.fromTags(read())
    }

    /** Applies at once (the activity is recreated); choosing the current language does nothing. */
    fun set(language: AppLanguage) {
        if (language == _language.value) return
        _language.value = language
        apply(language.tag)
    }
}
