package com.logflare.android.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.logflare.core.network.auth.AccessServiceToken
import com.example.logflare.core.network.auth.AccessServiceTokenProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import com.logflare.android.data.ServerConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Access service token for one server, encrypted at rest. [current] returns it only while
 * that server is the active base URL, so a token is never sent to a different host.
 * The secret is not logged. [current] never throws: it runs inside an OkHttp interceptor for
 * every request, password sign-ins included.
 */
interface AccessCredentialStore : AccessServiceTokenProvider {
    fun save(serverUrl: String, clientId: String, secret: String)
    fun clear()
}

@Singleton
class EncryptedAccessCredentialStore internal constructor(
    private val baseUrlProvider: BaseUrlProvider,
    /** Plain prefs holding only "was a token ever saved", so a password user never opens the encrypted file. */
    private val flagPrefs: () -> SharedPreferences,
    private val openSecretPrefs: () -> SharedPreferences,
) : AccessCredentialStore {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        baseUrlProvider: BaseUrlProvider,
    ) : this(
        baseUrlProvider,
        { context.getSharedPreferences(FLAG_PREFS, Context.MODE_PRIVATE) },
        {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        },
    )

    private class Stored(val server: String, val token: AccessServiceToken)

    private val prefs: SharedPreferences by lazy(openSecretPrefs)

    /** Null = nothing saved. [loaded] says whether this reflects the encrypted file (or its absence). */
    private var cached: Stored? = null
    private var loaded = false

    @Synchronized
    override fun save(serverUrl: String, clientId: String, secret: String) {
        val server = ServerConfigRepository.normalizeServerUrl(serverUrl)
        prefs.edit()
            .putString(KEY_SERVER, server)
            .putString(KEY_ID, clientId)
            .putString(KEY_SECRET, secret)
            .commit()
        flagPrefs().edit().putBoolean(KEY_HAS_TOKEN, true).commit()
        cached = Stored(server, AccessServiceToken(clientId, secret))
        loaded = true
    }

    @Synchronized
    override fun clear() {
        cached = null
        loaded = true
        val hadToken = try {
            flagPrefs().getBoolean(KEY_HAS_TOKEN, false)
        } catch (e: Exception) {
            Log.w(TAG, "Reading the Access flag failed", e)
            true
        }
        if (!hadToken) return
        try {
            prefs.edit().clear().commit()
            flagPrefs().edit().putBoolean(KEY_HAS_TOKEN, false).commit()
        } catch (e: Exception) {
            // The in-memory copy is already gone, so no request carries the token any more.
            Log.w(TAG, "Clearing the Access token failed", e)
        }
    }

    @Synchronized
    override fun current(): AccessServiceToken? = try {
        val stored = load()
        val active = baseUrlProvider.getBaseUrl()
            ?.let { ServerConfigRepository.normalizeServerUrl(it) }
            ?.takeIf { it.isNotEmpty() }
        if (stored != null && active != null && stored.server == active) stored.token else null
    } catch (e: Exception) {
        // Only the exception type: its message could be wrapped around key material.
        Log.w(TAG, "Reading the Access token failed (${e.javaClass.simpleName})")
        null
    }

    private fun load(): Stored? {
        if (loaded) return cached
        // Remembered as "none" even on failure, so a broken keystore is not retried on every request.
        loaded = true
        if (!flagPrefs().getBoolean(KEY_HAS_TOKEN, false)) return null
        val server = prefs.getString(KEY_SERVER, null)
        val id = prefs.getString(KEY_ID, null)?.takeIf { it.isNotEmpty() }
        val secret = prefs.getString(KEY_SECRET, null)?.takeIf { it.isNotEmpty() }
        cached = if (server != null && id != null && secret != null) Stored(server, AccessServiceToken(id, secret)) else null
        return cached
    }

    private companion object {
        const val TAG = "AccessCredentialStore"
        const val PREFS = "logflare_access_credentials"
        const val FLAG_PREFS = "logflare_access_flag"
        const val KEY_HAS_TOKEN = "has_token"
        const val KEY_SERVER = "server"
        const val KEY_ID = "id"
        const val KEY_SECRET = "secret"
    }
}
