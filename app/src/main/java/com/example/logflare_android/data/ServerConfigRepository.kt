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

private val Context.configDataStore by preferencesDataStore(name = "config")

@Singleton
class ServerConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_SERVER_URL: Preferences.Key<String> = stringPreferencesKey("server_url")
    }

    val serverUrl: Flow<String?> = context.configDataStore.data.map { it[KEY_SERVER_URL] }

    suspend fun setServerUrl(url: String) {
        context.configDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
    }
}
