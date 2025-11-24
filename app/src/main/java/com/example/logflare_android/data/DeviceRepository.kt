package com.example.logflare_android.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.FcmConfig
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.network.LogflareApi
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.fcmDataStore by preferencesDataStore(name = "fcm")

@Singleton
class DeviceRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    companion object {
        private val KEY_FCM_CONFIG: Preferences.Key<String> = stringPreferencesKey("config")
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
            Log.w(TAG, "Failed to register FCM token", error)
        }
    }

    private suspend fun fetchRemoteConfig(bearer: String): FcmConfig? {
        return runCatching {
            api.getFcmConfig(bearer)
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

    private suspend fun registerCurrentToken(bearer: String) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .onFailure { error -> Log.w(TAG, "Fetching Firebase token failed", error) }
            .getOrNull()

        if (token.isNullOrBlank()) {
            Log.w(TAG, "Firebase returned empty token")
            return
        }

        runCatching {
            api.registerFcmToken(bearer, FcmTokenParams(token))
        }.onFailure { error ->
            Log.w(TAG, "Failed to register current Firebase token", error)
        }
    }

    private fun buildFirebaseOptions(config: FcmConfig): FirebaseOptions {
        val client = config.client.firstOrNull()
            ?: error("FCM config missing client entry")
        val clientInfo = client.clientInfo
        val apiKey = client.apiKey.firstOrNull()?.currentKey
            ?: error("FCM config missing API key")
        val projectInfo = config.projectInfo

        return FirebaseOptions.Builder()
            .setApplicationId(clientInfo.mobileSdkAppId)
            .setApiKey(apiKey)
            .setGcmSenderId(projectInfo.projectNumber)
            .setProjectId(projectInfo.projectId)
            .apply {
                projectInfo.storageBucket?.takeIf { it.isNotBlank() }?.let { setStorageBucket(it) }
            }
            .build()
    }
}
