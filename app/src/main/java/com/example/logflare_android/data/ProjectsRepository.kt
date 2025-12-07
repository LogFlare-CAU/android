package com.example.logflare_android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectDTOWithToken
import com.example.logflare.core.model.ProjectData
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.enums.LogLevel
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

    private val projects: Flow<List<ProjectData>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROJECTS]?.let { rawJson ->
            runCatching { Json.decodeFromString<List<ProjectData>>(rawJson) }
                .getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(project: ProjectData) {
        context.dataStore.edit { prefs ->
            val oldList: List<ProjectData> = prefs[KEY_PROJECTS]
                ?.let { json ->
                    runCatching { Json.decodeFromString<List<ProjectData>>(json) }
                        .getOrElse { emptyList() }
                }
                ?: emptyList()
            val index = oldList.indexOfFirst { it.dto.id == project.dto.id }
            val newList = if (index >= 0) {
                oldList.toMutableList().apply {
                    this[index] = project
                }
            } else {
                oldList + project
            }
            prefs[KEY_PROJECTS] = Json.encodeToString(newList)
        }
    }


    suspend fun getAll(): List<ProjectData> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_PROJECTS] ?: return emptyList()

        return runCatching {
            Json.decodeFromString<List<ProjectData>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun get(projectId: Int): ProjectData? {
        return getAll().find { it.dto.id == projectId }
    }

    suspend fun setAlertLevel(projectId: Int, level: String) {
        context.dataStore.edit { prefs ->
            val oldList = prefs[KEY_PROJECTS]?.let {
                runCatching { Json.decodeFromString<List<ProjectData>>(it) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
            val newList = oldList.map { old ->
                if (old.dto.id == projectId) {
                    old.copy(alertLevel = level)
                } else {
                    old
                }
            }
            prefs[KEY_PROJECTS] = Json.encodeToString(newList)
        }
    }

    /**
     *  Fetches the list of projects from the remote API, merges it with the locally stored
     *  project data, and updates the local storage. Returns the updated list of projects.
     */
    suspend fun list(): Result<List<ProjectData>> {
        return try {
            val token = auth.token.first() ?: throw IllegalStateException("No token")
            val res: ProjectSequenceResponse = api.listProjects(token)
            if (!res.success) {
                throw IllegalStateException("listProjects failed: ${res.message}")
            }
            val remoteList: List<ProjectDTO> = res.data ?: emptyList()
            context.dataStore.edit { prefs ->
                val oldList = prefs[KEY_PROJECTS]?.let {
                    runCatching { Json.decodeFromString<List<ProjectData>>(it) }
                        .getOrDefault(emptyList())
                } ?: emptyList()
                val oldById = oldList.associateBy { it.dto.id }
                val merged: List<ProjectData> = remoteList.map { dto ->
                    val existing = oldById[dto.id]
                    existing?.copy(dto = dto)
                        ?: ProjectData(
                            dto = dto,
                            alertLevel = LogLevel.WARNING.label
                        )
                }
                prefs[KEY_PROJECTS] = Json.encodeToString(merged)
            }
            val finalList = getAll()
            Result.success(finalList)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                auth.clearToken()
                Result.failure(IllegalStateException("Unauthorized", e))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun create(name: String): Result<ProjectDTOWithToken> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res = api.createProject(token, ProjectCreateParams(name = name))
        if (!res.success) throw IllegalStateException("createProject failed: ${'$'}{res.message}")
        val dto = res.data ?: throw IllegalStateException("Empty project returned")
        val projectData = ProjectData(
            dto = ProjectDTO(
                id = dto.id,
                name = dto.name,
                alias = dto.alias,
                description = dto.description,
                logfiles = dto.logfiles
            ),
            alertLevel = LogLevel.WARNING.label
        )
        add(projectData)
        dto
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            auth.clearToken()
            throw IllegalStateException("Unauthorized")
        } else throw e
    }

    suspend fun addKeyword(projectId: Int, keyword: String): Result<Set<String>> {
        val project = get(projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val updatedKeywords = project.excludeKeywords + keyword
        val updatedProject = project.copy(excludeKeywords = updatedKeywords)
        add(updatedProject)
        return Result.success(updatedKeywords)
    }

    suspend fun removeKeyword(projectId: Int, keyword: String): Result<Set<String>> {
        val project = get(projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val updatedKeywords = project.excludeKeywords - keyword
        val updatedProject = project.copy(excludeKeywords = updatedKeywords)
        add(updatedProject)
        return Result.success(updatedKeywords)
    }
}
