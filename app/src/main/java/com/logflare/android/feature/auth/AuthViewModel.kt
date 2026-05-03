package com.logflare.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.ServerConfigRepository
import com.logflare.android.feature.usecase.AuthLoginUseCase
import com.logflare.android.feature.usecase.AuthMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AuthUiState(
    val loading: Boolean = false,
    val username: String? = null,
    val permission: Int = 0,
    val loginError: String? = null,
    /** Shown when /user/me fails after login (e.g. expired session). */
    val profileError: String? = null,
)


/**
 * ViewModel for authentication feature.
 * Handles login with optional custom server URL and device registration.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authLoginUseCase: AuthLoginUseCase,
    private val serverConfigRepository: ServerConfigRepository,
    private val authMeUseCase: AuthMeUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

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

    fun getMe() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, profileError = null)
            authMeUseCase()
                .onSuccess { me ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        username = me.username,
                        permission = me.permission,
                        profileError = null,
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        profileError = e.message ?: "Could not verify session",
                    )
                }
        }
    }

    private fun performLoginInternal(
        username: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, loginError = null)

            val ok = authLoginUseCase(username, password)
            if (ok) {
                _ui.value = _ui.value.copy(loading = false, loginError = null)
                onSuccess()
            } else {
                _ui.value = _ui.value.copy(
                    loading = false,
                    loginError = "Login failed"
                )
            }
        }
    }

}
