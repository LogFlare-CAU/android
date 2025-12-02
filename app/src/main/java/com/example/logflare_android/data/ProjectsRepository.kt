package com.example.logflare_android.data

import android.content.Context
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.feature.project.ProjectsUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val Context.dataStore by preferencesDataStore(name = "projects")

@Singleton
class ProjectsRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PROJECTS = stringPreferencesKey("projects")
    }

    private val projects: Flow<List<ProjectDTO>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROJECTS]?.let { rawJson ->
            runCatching { Json.decodeFromString<List<ProjectDTO>>(rawJson) }
                .getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(project: ProjectDTO) {
        context.dataStore.edit { prefs ->
            val oldList = prefs[KEY_PROJECTS]?.let {
                runCatching { Json.decodeFromString<List<ProjectDTO>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()

            val newList = oldList + project
            prefs[KEY_PROJECTS] = Json.encodeToString(newList)
        }
    }

    suspend fun getAll(): List<ProjectDTO> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_PROJECTS] ?: return emptyList()

        return runCatching {
            Json.decodeFromString<List<ProjectDTO>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun get(projectId: Int): ProjectDTO? {
        return getAll().find { it.id == projectId }
    }


    suspend fun list(): Result<List<ProjectDTO>> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res: ProjectSequenceResponse = api.listProjects(token)
        if (!res.success) throw IllegalStateException("listProjects failed")
        val list = res.data ?: emptyList()
        for (project in list){
            add(project)
        }
        list
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            auth.clearToken()
            throw IllegalStateException("Unauthorized")
        } else throw e
    }

    suspend fun create(name: String): Result<String> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res = api.createProject(token, com.example.logflare.core.model.ProjectCreateParams(name = name))
        if (!res.success) throw IllegalStateException("createProject failed: ${'$'}{res.message}")
        res.data ?: throw IllegalStateException("Empty token returned")
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            auth.clearToken()
            throw IllegalStateException("Unauthorized")
        } else throw e
    }


}
