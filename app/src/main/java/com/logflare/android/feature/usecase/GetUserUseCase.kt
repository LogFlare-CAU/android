package com.logflare.android.feature.usecase

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(username: String): UserDTO? {
        val token = authRepository.getToken()
        val res = runCatching {
            api.getUserByUsername(token, username)
        }.getOrNull() ?: return null
        if (!res.success) return null
        return res.data
    }
}