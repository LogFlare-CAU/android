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
        val normalized = normalize(url)
        context.configDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalized
        }
    }

    private fun normalize(raw: String): String {
        var working = raw.trim()
        if (working.isEmpty()) return working
        // Add scheme if missing
        if (!working.startsWith("http://") && !working.startsWith("https://")) {
            working = "http://$working"
        }
        return try {
            val uri = java.net.URI(working)
            val host = uri.host ?: return working
            val needsPort = uri.port == -1 && (host == "localhost" || host == "10.0.2.2")
            val portPart = if (needsPort) ":8000" else if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.rawPath ?: ""
            val rebuilt = "${uri.scheme}://$host$portPart$path" + if (working.endsWith("/") || path.endsWith("/")) "" else "/"
            rebuilt
        } catch (e: Exception) {
            // Fallback: ensure trailing slash
            if (!working.endsWith("/")) working + "/" else working
        }
    }
}
