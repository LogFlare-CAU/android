package com.logflare.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

private val Context.themeDataStore by preferencesDataStore(name = "theme")

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_preference")
    }

    val preference: Flow<ThemePreference> = context.themeDataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            ThemePreference.LIGHT.name -> ThemePreference.LIGHT
            ThemePreference.DARK.name -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    suspend fun setPreference(preference: ThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = preference.name
        }
    }

    fun resolveDarkTheme(preference: ThemePreference, systemDark: Boolean): Boolean =
        when (preference) {
            ThemePreference.SYSTEM -> systemDark
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }

    /** Login toggle: Light ↔ Dark. If currently SYSTEM, pick the opposite of resolved theme. */
    fun toggleLightDark(current: ThemePreference, systemDark: Boolean): ThemePreference {
        val resolvedDark = resolveDarkTheme(current, systemDark)
        return if (resolvedDark) ThemePreference.LIGHT else ThemePreference.DARK
    }
}
