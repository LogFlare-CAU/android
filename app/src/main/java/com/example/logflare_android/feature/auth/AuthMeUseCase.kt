package com.example.logflare_android.feature.auth

import android.util.Log
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMeUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
){
    suspend operator fun invoke(): UserDTO?{
        val token = authRepository.getToken()
        val result = runCatching {
            api.getme(token)
        }.getOrElse {
            return null
            // TODO: 실제 예외 처리
        }
        result.data?.username?.let { authRepository.setUsername(it) }
        return result.data
    }
}