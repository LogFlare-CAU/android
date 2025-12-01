package com.example.logflare_android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.ServerConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication feature.
 * Handles login with optional custom server URL and device registration.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authLoginUseCase: AuthLoginUseCase,
    private val serverConfigRepository: ServerConfigRepository
) : ViewModel() {

    /**
     * Legacy login without dynamic server URL (kept for backward compatibility with existing UI).
     * Uses whatever server URL was previously saved (or default).
     */
    fun login(username: String, password: String, onSuccess: () -> Unit) {
        performLoginInternal(username, password, onSuccess)
    }

    /**
     * New login entry point supporting user-provided serverUrl.
     * Persists serverUrl before making the auth request so that subsequent API calls route correctly.
     */
    fun login(serverUrl: String, username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            serverConfigRepository.setServerUrl(serverUrl)
            performLoginInternal(username, password, onSuccess)
        }
    }

    private fun performLoginInternal(
        username: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val ok = authLoginUseCase(username, password)
            if (ok) {
                onSuccess()
            } else {
                // TODO: 에러 상태 emit
            }
        }
    }
}
