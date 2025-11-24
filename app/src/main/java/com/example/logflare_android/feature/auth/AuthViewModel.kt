package com.example.logflare_android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.network.LogflareApi
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.ServerConfigRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication feature.
 * Handles login with optional custom server URL and device registration.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: com.example.logflare_android.data.DeviceRepository,
    private val serverConfigRepository: ServerConfigRepository
) : ViewModel() {

    /**
     * Legacy login without dynamic server URL (kept for backward compatibility with existing UI).
     * Uses whatever server URL was previously saved (or default).
     */
    fun login(username: String, password: String, onSuccess: () -> Unit) {
        performLogin(username, password, onSuccess)
    }

    /**
     * New login entry point supporting user-provided serverUrl.
     * Persists serverUrl before making the auth request so that subsequent API calls route correctly.
     */
    fun login(serverUrl: String, username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            serverConfigRepository.setServerUrl(serverUrl)
            performLogin(username, password, onSuccess)
        }
    }

    private fun performLogin(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                api.authenticate(UserAuthParams(username, password))
            }.onSuccess { res: StringResponse ->
                val token = res.data
                if (res.success && !token.isNullOrBlank()) {
                    val bearer = "Bearer $token"
                    authRepository.setToken(bearer)
                    // Try to obtain current FCM token and register device
                    try {
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val fcm = task.result
                                    if (!fcm.isNullOrBlank()) {
                                        // fire-and-forget
                                        viewModelScope.launch {
                                            deviceRepository.registerDevice(fcm)
                                        }
                                    }
                                }
                            }
                    } catch (_: Exception) {
                        // ignore
                    }
                    onSuccess()
                }
            }.onFailure {
                // TODO: emit error state
            }
        }
    }
}
