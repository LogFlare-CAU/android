package com.logflare.android.feature.usecase

import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
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
    ): Result<Unit> {
        val token = authRepository.getToken()
        val body = ProjectPermsBatchParams(projectId, usernames)
        val res = runCatching {
            api.resetProjectPerms(token, body)
        }.getOrNull() ?: return Result.failure(Exception("Network error"))
        return if (res.success) {
            Result.success(Unit)
        } else {
            val msg = res.message.ifBlank { "Failed to update project permissions" }
            Result.failure(Exception(msg))
        }
    }
}