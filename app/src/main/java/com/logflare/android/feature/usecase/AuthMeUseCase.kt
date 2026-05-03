package com.logflare.android.feature.usecase

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMeUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): Result<UserDTO> {
        val token = runCatching { authRepository.getToken() }
            .getOrElse { return Result.failure(it) }

        val response = runCatching { api.getme(token) }
            .getOrElse { return Result.failure(it) }

        if (!response.success) {
            val msg = response.message.ifBlank { "Failed to load profile" }
            return Result.failure(IllegalStateException(msg))
        }

        val user = response.data ?: return Result.failure(IllegalStateException("Empty profile response"))

        try {
            deviceRepository.syncConfigAndRegister()
        } catch (_: Exception) {
        }
        try {
            authRepository.setUsername(user.username)
        } catch (_: Exception) {
        }

        return Result.success(user)
    }
}