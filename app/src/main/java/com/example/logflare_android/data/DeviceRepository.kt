package com.example.logflare_android.data

import com.example.logflare.core.model.DeviceRegisterParams
import com.example.logflare.core.network.LogflareApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DeviceRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository
) {
    suspend fun registerDevice(fcmToken: String) {
        val token = auth.token.first() ?: return
        val params = DeviceRegisterParams(token = fcmToken)
        // Best-effort register; ignore response for now
        try {
            api.postDevice(token, params)
        } catch (_: Exception) {
            // swallow; best-effort
        }
    }
}
