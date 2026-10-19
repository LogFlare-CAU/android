package com.logflare.android.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.FcmConfig
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.network.LogflareApi
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.push.PushTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.fcmDataStore by preferencesDataStore(name = "fcm")


@Singleton
class DeviceRepository internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    private val context: Context,
    private val json: Json,
) : PushTokenSource {

    @Inject
    constructor(
        api: LogflareApi,
        sessionRepository: SessionRepository,
        @ApplicationContext context: Context,
        json: Json,
    ) : this(api, sessionRepository.state, context, json)

    companion object {
        private val KEY_FCM_CONFIG: Preferences.Key<String> = stringPreferencesKey("config")
        private val KEY_ALERT_LEVEL: Preferences.Key<String> = stringPreferencesKey("alert_level")
        private const val TAG = "DeviceRepository"
    }

    /**
     * Fetches the FCM config from the backend (falling back to the persisted copy), persists it,
     * and initializes Firebase if needed. Returns whether Firebase is ready.
     */
    override suspend fun ensureFirebase(): Boolean {
        val remoteConfig = if (isSignedIn()) fetchRemoteConfig() else null
        val config = remoteConfig ?: loadPersistedConfig()
        if (config == null) {
            Log.w(TAG, "No FCM config available from backend or cache")
            return false
        }

        if (remoteConfig != null) {
            persistConfig(remoteConfig)
        }

        if (!initializeFirebase(config)) {
            Log.w(TAG, "Firebase initialization failed")
            return false
        }
        return true
    }

    override suspend fun ensureFirebaseFromCache(): Boolean {
        val config = loadPersistedConfig() ?: return false
        return initializeFirebase(config)
    }


    /**
     * Whether a session is signed in, from the same state the auth interceptor reads; waits while
     * it is still being loaded (a cold start from a push).
     */
    private suspend fun isSignedIn(): Boolean =
        sessions.first { it !is SessionState.Loading } is SessionState.SignedIn

    override suspend fun getFcmTokenOrNull(): String? {
        return runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.onFailure { error ->
            Log.w(TAG, "Failed to get FCM token", error)
        }.getOrNull()
    }

    private suspend fun fetchRemoteConfig(): FcmConfig? {
        return runCatching<FcmConfigResponse> {
            api.getFirebaseConfig()
        }.onFailure { error ->
            Log.w(TAG, "Fetching FCM config failed", error)
        }.getOrNull()?.takeIf { it.success }?.data
    }

    private suspend fun persistConfig(config: FcmConfig) {
        context.fcmDataStore.edit { prefs ->
            prefs[KEY_FCM_CONFIG] = json.encodeToString(config)
        }
    }

    private suspend fun loadPersistedConfig(): FcmConfig? {
        val raw = context.fcmDataStore.data
            .map { it[KEY_FCM_CONFIG] }
            .firstOrNull()
        return raw?.runCatching { json.decodeFromString<FcmConfig>(this) }
            ?.onFailure { error -> Log.w(TAG, "Failed to decode cached FCM config", error) }
            ?.getOrNull()
    }

    private fun initializeFirebase(config: FcmConfig): Boolean {
        return runCatching {
            val options = buildFirebaseOptions(config)
            val existing = FirebaseApp.getApps(context).firstOrNull()
            if (existing == null) {
                FirebaseApp.initializeApp(context, options)
            }
        }.onFailure { error ->
            Log.w(TAG, "Firebase initialization error", error)
        }.isSuccess
    }

    /**
     * Ensures Firebase is initialized by attempting to load cached config and initialize.
     * Used in LogflareMessagingService when a message is received but FirebaseApp is missing.
     */
    fun ensureFirebaseInitializedFromCacheAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val initialized = try {
                FirebaseApp.getInstance()
                true
            } catch (e: Exception) {
                false
            }
            if (initialized) {
                return@launch
            }
            Log.w(TAG, "FirebaseApp missing. Trying to reinitialize from cached config...")
            try {
                val config = loadPersistedConfig()
                if (config == null) {
                    Log.w(TAG, "No persisted FCM config; cannot reinitialize Firebase")
                    return@launch
                }

                val initOk = initializeFirebase(config)
                if (!initOk) {
                    Log.w(TAG, "Failed to initialize Firebase from cached config")
                    return@launch
                }

                Log.i(TAG, "Firebase reinitialized in MessagingService")
                // Registration is PushRegistrar's job (it honours the sign-out guard); a refreshed
                // token reaches it through LogflareMessagingService.onNewToken.
            } catch (e: Exception) {
                Log.e(TAG, "Error while reinitializing Firebase from cache", e)
            }
        }
    }

    private fun buildFirebaseOptions(config: FcmConfig): FirebaseOptions {
        val applicationId = config.mobilesdkAppId?.takeIf { it.isNotBlank() }
            ?: error("FCM config missing mobilesdk_app_id")
        val apiKey = config.apiKey?.takeIf { it.isNotBlank() }
            ?: error("FCM config missing api_key")
        val senderId = config.messagingSenderId?.takeIf { it.isNotBlank() }
            ?: error("FCM config missing messaging_sender_id")
        val projectId = config.projectId?.takeIf { it.isNotBlank() }
            ?: error("FCM config missing project_id")

        return FirebaseOptions.Builder()
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .setGcmSenderId(senderId)
            .setProjectId(projectId)
            .build()
    }

    suspend fun getAlertLevel(): String? {
        return context.fcmDataStore.data
            .map { it[KEY_ALERT_LEVEL] }
            .firstOrNull()
    }

    /** Drops the pre-rework alert level once it has been migrated. */
    suspend fun clearLegacyAlertLevel() {
        context.fcmDataStore.edit { it.remove(KEY_ALERT_LEVEL) }
    }

}
