package com.example.logflare_android.feature.project.usecase

import com.example.logflare.core.model.ProjectPermsDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetProjectPermsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(projectId: Int): List<ProjectPermsDTO>? {
        val token = authRepository.getToken()
        val res = runCatching {
            api.getProjectPermissions(token, projectId)
        }.getOrNull() ?: return null
        return res.data
    }
}