package com.logflare.android.feature.usecase

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMeUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
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

        // Push registration is no longer part of loading a screen: PushRegistrar does it in the
        // background once per session.
        try {
            authRepository.setUsername(user.username)
        } catch (_: Exception) {
        }

        return Result.success(user)
    }
}