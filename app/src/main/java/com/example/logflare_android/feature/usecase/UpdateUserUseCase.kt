package com.example.logflare_android.feature.usecase

import com.example.logflare.core.model.UserUpdateParams
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.UserPermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(
        userId: Int, username: String?, password: String?, permission: UserPermission?
    ): Result<Unit> {
        val token = authRepository.getToken()
        val body = UserUpdateParams(username = username, password = password, permission = permission?.code)
        val res = runCatching {
            api.updateUser(token, userId, body)
        }.getOrNull() ?: return Result.failure(Exception("Failed to update user"))
        return if (res.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(res.message))
        }
    }
}