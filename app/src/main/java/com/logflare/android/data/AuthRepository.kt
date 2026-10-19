package com.logflare.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.UserDTO
import com.logflare.android.data.session.SessionKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

/** Everything about the signed-in user that is persisted in the "auth" DataStore. */
data class StoredAuth(
    val token: String?,
    val username: String?,
    val userIdx: Int?,
    val permission: Int?,
    /**
     * [SessionKey] of the stored token, written in the same transaction as the token and kept
     * until the token changes. Null only for installs from before it existed.
     */
    val sessionKey: String? = null,
)

@Singleton
class AuthRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    /** Normalized server URL the next session key is derived from. */
    private val serverUrl: Flow<String?>,
) {
    @Inject
    constructor(@ApplicationContext context: Context, serverConfig: ServerConfigRepository) :
        this(context.dataStore, serverConfig.serverUrl)

    companion object {
        private val KEY_TOKEN: Preferences.Key<String> = stringPreferencesKey("jwt")
        private val KEY_USERNAME: Preferences.Key<String> = stringPreferencesKey("username")
        private val KEY_USER_IDX: Preferences.Key<Int> = intPreferencesKey("userIdx")
        private val KEY_PERMISSION: Preferences.Key<Int> = intPreferencesKey("permission")
        private val KEY_SESSION_KEY: Preferences.Key<String> = stringPreferencesKey("session_key")
    }

    /** Only tests read the bare token; production code goes through [stored]. */
    @VisibleForTesting
    val token: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }

    val stored: Flow<StoredAuth> = dataStore.data.map {
        StoredAuth(
            token = it[KEY_TOKEN],
            username = it[KEY_USERNAME],
            userIdx = it[KEY_USER_IDX],
            permission = it[KEY_PERMISSION],
            sessionKey = it[KEY_SESSION_KEY],
        )
    }

    /**
     * Stores a token without a profile. A different token drops the stored session key; it is
     * derived (once) from whatever identity is stored when the session is first loaded.
     */
    @VisibleForTesting
    suspend fun setToken(jwt: String) {
        dataStore.edit { prefs ->
            if (prefs[KEY_TOKEN] != jwt) prefs.remove(KEY_SESSION_KEY)
            prefs[KEY_TOKEN] = jwt
        }
    }

    /**
     * Removes the token, the session key and the whole user identity (username/idx/permission)
     * so a later login as someone else can't inherit the previous user's session.
     */
    suspend fun clearToken() {
        clearTokenIf(expected = null)
    }

    /**
     * [clearToken], but only while the stored token is [expected] (any token when null), checked
     * in the same transaction. Returns whether a token was removed.
     */
    suspend fun clearTokenIf(expected: String?): Boolean {
        var cleared = false
        dataStore.edit { prefs ->
            val current = prefs[KEY_TOKEN]
            if (expected != null && current != expected) return@edit
            cleared = current != null
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_SESSION_KEY)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_USER_IDX)
            prefs.remove(KEY_PERMISSION)
        }
        return cleared
    }

    /**
     * Writes token, username and session key in one transaction (old login path; profile not
     * known yet). Clears idx/permission so they can't belong to a different user.
     */
    suspend fun setTokenAndUsername(jwt: String, username: String): SessionKey {
        val newKey = SessionKey.of(currentServerUrl(), null, username)
        var key = newKey
        dataStore.edit { prefs ->
            key = prefs.writeToken(jwt, newKey)
            prefs[KEY_USERNAME] = username
            prefs.remove(KEY_USER_IDX)
            prefs.remove(KEY_PERMISSION)
        }
        return key
    }

    /** Writes token, profile and session key in one transaction. */
    suspend fun setSession(jwt: String, user: UserDTO): SessionKey {
        val newKey = SessionKey.of(currentServerUrl(), user.idx, user.username)
        var key = newKey
        dataStore.edit { prefs ->
            key = prefs.writeToken(jwt, newKey)
            prefs.writeUser(user)
        }
        return key
    }

    /**
     * Updates the profile; never the session key. With [session], only while that is still the
     * stored session (a late answer for a replaced session is dropped). Returns whether written.
     */
    suspend fun setUserProfile(user: UserDTO, session: SessionKey? = null): Boolean {
        var written = false
        dataStore.edit { prefs ->
            if (session != null && (prefs[KEY_TOKEN] == null || prefs[KEY_SESSION_KEY] != session.value)) return@edit
            prefs.writeUser(user)
            written = true
        }
        return written
    }

    /**
     * Persists [key] for a token stored without one (installs from before session keys), unless
     * the token changed or a key was stored meanwhile.
     */
    suspend fun ensureSessionKey(jwt: String, key: SessionKey) {
        dataStore.edit { prefs ->
            if (prefs[KEY_TOKEN] == jwt && prefs[KEY_SESSION_KEY] == null) prefs[KEY_SESSION_KEY] = key.value
        }
    }

    /**
     * Stores [jwt]. The session key changes only with the token: re-storing the same token keeps
     * its key. Returns the key now stored.
     */
    private fun MutablePreferences.writeToken(jwt: String, newKey: SessionKey): SessionKey {
        val kept = this[KEY_SESSION_KEY]?.takeIf { this[KEY_TOKEN] == jwt }
        this[KEY_TOKEN] = jwt
        val key = kept?.let(::SessionKey) ?: newKey
        this[KEY_SESSION_KEY] = key.value
        return key
    }

    private suspend fun currentServerUrl(): String =
        serverUrl.catch { emit(null) }.first().orEmpty()

    private fun MutablePreferences.writeUser(user: UserDTO) {
        this[KEY_USERNAME] = user.username
        this[KEY_USER_IDX] = user.idx
        this[KEY_PERMISSION] = user.permission
    }

    /** Old screens' profile refresh; never changes the session key. */
    suspend fun setUsername(username: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
        }
    }

}
