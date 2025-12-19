package com.example.logflare_android.feature.usecase

import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(userid: Int): Result<Unit> {
        val token = authRepository.getToken()
        val res = runCatching {
            api.deleteUser(token, userid)
        }.getOrNull() ?: return Result.failure(Exception("Network error"))
        return if (res.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(res.message))
        }
    }
}