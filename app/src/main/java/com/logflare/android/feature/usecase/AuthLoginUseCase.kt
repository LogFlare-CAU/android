package com.logflare.android.feature.usecase

import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.DeviceRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginResult {
    data object Success : LoginResult()
    data object RateLimited : LoginResult()
    data object Failed : LoginResult()
}

@Singleton
class AuthLoginUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) {

    suspend operator fun invoke(
        username: String,
        password: String
    ): LoginResult {
        val result = runCatching {
            api.authenticate(UserAuthParams(username, password, true))
        }.getOrElse { error ->
            return if (error is HttpException && error.code() == 429) {
                LoginResult.RateLimited
            } else {
                LoginResult.Failed
            }
        }

        val token = result.data
        if (!result.success || token.isNullOrBlank()) {
            return LoginResult.Failed
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

        return LoginResult.Success
    }
}
