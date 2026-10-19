package com.logflare.android.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.ProjectData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.legacyProjectsDataStore by preferencesDataStore(name = "projects")

private val projectListKey = stringPreferencesKey("projects")

/**
 * Read-only access to the pre-rework `projects` DataStore (per-project alert level and exclusion
 * keywords), kept only so [com.logflare.android.push.AlertSettingsRepository] can migrate it once.
 */
@Singleton
class LegacyProjectSettingsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** The stored list; empty when there is none or it can't be decoded. */
    suspend fun readAll(): List<ProjectData> =
        context.legacyProjectsDataStore.data.first()[projectListKey]?.let { raw ->
            runCatching { Json.decodeFromString<List<ProjectData>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()
}
