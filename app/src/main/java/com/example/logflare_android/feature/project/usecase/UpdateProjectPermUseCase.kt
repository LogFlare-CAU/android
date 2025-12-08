package com.example.logflare_android.feature.project.usecase

import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateProjectPermUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        projectId: Int,
        usernames: Set<String>
    ) {
        val token = authRepository.getToken()
        val body = ProjectPermsBatchParams(projectId, usernames)
        api.resetProjectPerms(token, body)
    }
}