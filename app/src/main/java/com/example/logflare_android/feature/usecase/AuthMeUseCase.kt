package com.example.logflare_android.feature.usecase

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMeUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
){
    suspend operator fun invoke(): UserDTO?{
        val token = authRepository.getToken()
        val result = runCatching {
            api.getme(token)
        }.getOrElse {
            return null
            // TODO: 실제 예외 처리
        }
        deviceRepository.syncConfigAndRegister()
        result.data?.username?.let { authRepository.setUsername(it) }
        return result.data
    }
}