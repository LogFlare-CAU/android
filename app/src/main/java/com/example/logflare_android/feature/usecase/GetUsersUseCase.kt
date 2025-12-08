package com.example.logflare_android.feature.usecase

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(): List<UserDTO>? {
        val token = authRepository.getToken()
        val res = runCatching { api.getAllUsers(token) }.getOrNull()?: return null
        return res.data
    }
}