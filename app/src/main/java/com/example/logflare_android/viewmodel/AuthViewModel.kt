package com.example.logflare_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.network.LogflareApi
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare_android.data.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository,
    private val deviceRepository: com.example.logflare_android.data.DeviceRepository
) : ViewModel() {

    fun login(username: String, password: String, onSuccess: () -> Unit) {
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
