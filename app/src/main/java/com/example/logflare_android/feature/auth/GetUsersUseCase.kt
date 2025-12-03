package com.example.logflare_android.feature.auth

import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUsersUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository
) {
}