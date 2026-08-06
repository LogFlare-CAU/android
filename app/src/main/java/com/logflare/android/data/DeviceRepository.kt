package com.logflare.android.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.FcmConfig
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.network.LogflareApi
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val Context.fcmDataStore by preferencesDataStore(name = "fcm")

private fun Throwable.isFcmAlreadyRegistered(): Boolean =
    this is HttpException && code() == 409

@Singleton
class DeviceRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    companion object {
        private val KEY_FCM_CONFIG: Preferences.Key<String> = stringPreferencesKey("config")
        private val KEY_ALERT_LEVEL: Preferences.Key<String> = stringPreferencesKey("alert_level")
        private const val TAG = "DeviceRepository"
    }

    /**
     * Fetches FCM configuration from the backend, persists it locally, ensures Firebase is
     * initialized, and registers the current device token with the server.
     */
    suspend fun syncConfigAndRegister() {
        val bearer = auth.token.firstOrNull() ?: return
        val remoteConfig = fetchRemoteConfig(bearer)
        val config = remoteConfig ?: loadPersistedConfig()
        if (config == null) {
            Log.w(TAG, "No FCM config available from backend or cache")
            return
        }

        if (remoteConfig != null) {
            persistConfig(remoteConfig)
        }

        if (!initializeFirebase(config)) {
            Log.w(TAG, "Firebase initialization failed")
            return
        }

        registerCurrentToken(bearer)
    }

    /**
     * Called by [LogflareMessagingService] when Firebase issues a refreshed token.
     */
    suspend fun registerDevice(fcmToken: String) {
        val bearer = auth.token.firstOrNull() ?: return
        runCatching {
            api.registerFcmToken(bearer, FcmTokenParams(fcmToken))
        }.onFailure { error ->
            if (error.isFcmAlreadyRegistered()) {
                Log.i(TAG, "FCM token already registered")
            } else {
                Log.w(TAG, "Failed to register FCM token", error)
            }
        }
    }

    suspend fun getFcmTokenOrNull(): String? {
        return runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.onFailure { error ->
            Log.w(TAG, "Failed to get FCM token", error)
        }.getOrNull()
    }

    private suspend fun fetchRemoteConfig(bearer: String): FcmConfig? {
        return runCatching<FcmConfigResponse> {
            api.getFirebaseConfig(bearer)
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
                val newToken = getFcmTokenOrNull()
                if (!newToken.isNullOrBlank()) {
                    registerDevice(newToken)
                    Log.i(TAG, "Re-registered FCM token: $newToken")
                } else {
                    Log.w(TAG, "Could not get new FCM token after reinit")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while reinitializing Firebase from cache", e)
            }
        }
    }

    private suspend fun registerCurrentToken(bearer: String) {
        val token = getFcmTokenOrNull()
            ?: run {
                Log.w(TAG, "No FCM token available to register")
                return
            }
        runCatching {
            api.registerFcmToken(bearer, FcmTokenParams(token))
        }.onFailure { error ->
            if (error.isFcmAlreadyRegistered()) {
                Log.i(TAG, "FCM token already registered")
            } else {
                Log.w(TAG, "Failed to register current Firebase token", error)
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

    suspend fun setAlertLevel(level: String) {
        context.fcmDataStore.edit { prefs ->
            prefs[KEY_ALERT_LEVEL] = level
        }
    }

}
