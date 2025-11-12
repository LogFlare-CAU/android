package com.example.logflare_android.data

import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.network.LogflareApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class ProjectsRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository
) {
    suspend fun list(): Result<List<ProjectDTO>> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res: ProjectSequenceResponse = api.listProjects(token)
        if (!res.success) throw IllegalStateException("listProjects failed")
        res.data ?: emptyList()
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            auth.clearToken()
            throw IllegalStateException("Unauthorized")
        } else throw e
    }
}
