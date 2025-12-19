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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN: Preferences.Key<String> = stringPreferencesKey("jwt")
        private val KEY_USERNAME: Preferences.Key<String> = stringPreferencesKey("username")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }

    suspend fun setToken(jwt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = jwt
        }
    }

    suspend fun getToken(): String {
        val token: String = context.dataStore.data.first()[KEY_TOKEN]?: throw IllegalStateException("No token found")
        return token
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
        }
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun getUsername(): String? {
        return context.dataStore.data.first()[KEY_USERNAME]
    }

}
