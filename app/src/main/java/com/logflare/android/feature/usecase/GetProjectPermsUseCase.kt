package com.logflare.android.feature.usecase

import com.example.logflare.core.model.ProjectPermsDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
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
        if (!res.success) return null
        return res.data
    }
}