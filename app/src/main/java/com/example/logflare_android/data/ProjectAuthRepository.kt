package com.example.logflare_android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.projectDataStore by preferencesDataStore(name = "project_auth")

@Singleton
class ProjectAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PROJECT_NAME: Preferences.Key<String> = stringPreferencesKey("project_name")
        private val KEY_PROJECT_KEY: Preferences.Key<String> = stringPreferencesKey("project_key")
    }

    val projectName: Flow<String?> = context.projectDataStore.data.map { it[KEY_PROJECT_NAME] }
    val projectKey: Flow<String?> = context.projectDataStore.data.map { it[KEY_PROJECT_KEY] }

    suspend fun setProject(name: String, keyBearer: String) {
        context.projectDataStore.edit { prefs ->
            prefs[KEY_PROJECT_NAME] = name
            prefs[KEY_PROJECT_KEY] = keyBearer
        }
    }

    suspend fun clear() {
        context.projectDataStore.edit { prefs ->
            prefs.remove(KEY_PROJECT_NAME)
            prefs.remove(KEY_PROJECT_KEY)
        }
    }
}
