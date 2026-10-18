package com.logflare.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.UserDTO
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

/** Everything about the signed-in user that is persisted in the "auth" DataStore. */
data class StoredAuth(
    val token: String?,
    val username: String?,
    val userIdx: Int?,
    val permission: Int?,
)

@Singleton
class AuthRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.dataStore)

    companion object {
        private val KEY_TOKEN: Preferences.Key<String> = stringPreferencesKey("jwt")
        private val KEY_USERNAME: Preferences.Key<String> = stringPreferencesKey("username")
        private val KEY_USER_IDX: Preferences.Key<Int> = intPreferencesKey("userIdx")
        private val KEY_PERMISSION: Preferences.Key<Int> = intPreferencesKey("permission")
    }

    val token: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }

    val stored: Flow<StoredAuth> = dataStore.data.map {
        StoredAuth(
            token = it[KEY_TOKEN],
            username = it[KEY_USERNAME],
            userIdx = it[KEY_USER_IDX],
            permission = it[KEY_PERMISSION],
        )
    }

    suspend fun setToken(jwt: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = jwt
        }
    }

    suspend fun getToken(): String {
        val token: String = dataStore.data.first()[KEY_TOKEN]?: throw IllegalStateException("No token found")
        return token
    }

    /**
     * Removes the token and the whole user identity (username/idx/permission) so a later login
     * as someone else can't inherit the previous user's session key.
     */
    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_USER_IDX)
            prefs.remove(KEY_PERMISSION)
        }
    }

    /**
     * Writes token and username in one transaction (old login path; profile not known yet).
     * Clears idx/permission so they can't belong to a different user.
     */
    suspend fun setTokenAndUsername(jwt: String, username: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = jwt
            prefs[KEY_USERNAME] = username
            prefs.remove(KEY_USER_IDX)
            prefs.remove(KEY_PERMISSION)
        }
    }

    /** Writes token and profile in one transaction. */
    suspend fun setSession(jwt: String, user: UserDTO) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = jwt
            prefs.writeUser(user)
        }
    }

    suspend fun setUserProfile(user: UserDTO) {
        dataStore.edit { prefs -> prefs.writeUser(user) }
    }

    private fun MutablePreferences.writeUser(user: UserDTO) {
        this[KEY_USERNAME] = user.username
        this[KEY_USER_IDX] = user.idx
        this[KEY_PERMISSION] = user.permission
    }

    suspend fun setUsername(username: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun getUsername(): String? {
        return dataStore.data.first()[KEY_USERNAME]
    }

}
