package com.logflare.android.push

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.logflare.android.data.session.SessionKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** The server accepted [fcmToken] for [session]. */
data class PushRegistration(val session: SessionKey, val fcmToken: String)

/** The last [PushRegistration], kept across process restarts so [PushRegistrar] can skip resending it. */
interface PushRegistrationStore {
    suspend fun get(): PushRegistration?

    /** Null forgets it. */
    suspend fun set(registration: PushRegistration?)
}

private val Context.pushRegistrationDataStore by preferencesDataStore(name = "push_registration")

@Singleton
class DataStorePushRegistrationStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) : PushRegistrationStore {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.pushRegistrationDataStore)

    override suspend fun get(): PushRegistration? {
        val prefs = dataStore.data.first()
        // Rows written before the server bound pushes to login sessions carry no version: ignored,
        // so the next start registers once more and the server binds them to the login token.
        if (prefs[KEY_VERSION] != SCHEMA_VERSION) return null
        val session = prefs[KEY_SESSION] ?: return null
        val token = prefs[KEY_FCM_TOKEN] ?: return null
        return PushRegistration(SessionKey(session), token)
    }

    override suspend fun set(registration: PushRegistration?) {
        dataStore.edit { prefs ->
            if (registration == null) {
                prefs.remove(KEY_SESSION)
                prefs.remove(KEY_FCM_TOKEN)
                prefs.remove(KEY_VERSION)
            } else {
                prefs[KEY_SESSION] = registration.session.value
                prefs[KEY_FCM_TOKEN] = registration.fcmToken
                prefs[KEY_VERSION] = SCHEMA_VERSION
            }
        }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session_key")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        val KEY_VERSION = intPreferencesKey("schema_version")

        /** 2: the registration is bound to a login session server-side (push-sessions release). */
        const val SCHEMA_VERSION = 2
    }
}
