package com.logflare.android.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.logflare.android.data.ServerConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentServersDataStore by preferencesDataStore(name = "recent_servers")

/** Server URLs the user signed in to, most recent first, deduplicated by normalized URL, at most [MAX]. */
@Singleton
class RecentServersRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.recentServersDataStore)

    val servers: Flow<List<String>> = dataStore.data.map { decode(it[KEY_SERVERS]) }

    suspend fun remember(url: String) {
        val normalized = ServerConfigRepository.normalizeServerUrl(url)
        if (normalized.isEmpty()) return
        dataStore.edit { prefs ->
            val updated = (listOf(normalized) + decode(prefs[KEY_SERVERS]).filterNot { it == normalized })
                .take(MAX)
            prefs[KEY_SERVERS] = updated.joinToString(SEPARATOR)
        }
    }

    private fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val MAX = 5
        const val SEPARATOR = "\n"
        val KEY_SERVERS: Preferences.Key<String> = stringPreferencesKey("servers")
    }
}
