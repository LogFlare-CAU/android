package com.logflare.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    /** Shown when /user/me fails after login (e.g. expired session). */
    val profileError: String? = null,
)


/**
 * Profile lookup for the legacy Home screen (removed with it). Sign-in lives in [LoginViewModel].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authMeUseCase: AuthMeUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

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

}
