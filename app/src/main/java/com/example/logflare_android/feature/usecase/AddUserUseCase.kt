package com.example.logflare_android.feature.usecase

import com.example.logflare.core.model.UserCreateParams
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.UserPermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(username: String, password: String, permission: UserPermission): Result<Unit> {
        val token = authRepository.getToken()
        val body = UserCreateParams(username = username, permission = permission.code, password = password)
        val res = runCatching {
            api.createUser(token, body)
        }.getOrNull() ?: return Result.failure(Exception("Network error"))
        return if (res.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(res.message))
        }
    }
}