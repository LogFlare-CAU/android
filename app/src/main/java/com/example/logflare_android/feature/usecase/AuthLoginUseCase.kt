package com.example.logflare_android.feature.usecase

import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthLoginUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {

    suspend operator fun invoke(
        username: String,
        password: String
    ): Boolean {
        val result = runCatching {
            api.authenticate(UserAuthParams(username, password, true))
        }.getOrElse {
            // 네트워크/서버 에러
            return false
        }

        val token = result.data
        if (!result.success || token.isNullOrBlank()) {
            return false
        }

        val bearer = "Bearer $token"
        authRepository.setToken(bearer)
        authRepository.setUsername(username)
        runCatching {
            deviceRepository.syncConfigAndRegister()
        }.onFailure {
            // Firebase 초기화 실패해도 로그인 자체는 성공했으므로
            // 앱이 죽는 건 막기 위해 swallow 가능
        }


        return true
    }
}
