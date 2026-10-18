package com.logflare.android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.session.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogoutUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/** Old confirm screen, kept until Task 11; signs out through [SignOutUseCase] like every path. */
@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(LogoutUiState())
    val ui: StateFlow<LogoutUiState> = _ui

    fun performLogout(onSuccess: () -> Unit) {
        if (_ui.value.isLoading) return

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                signOut()
                _ui.update { LogoutUiState() }
                onSuccess()
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _ui.update { it.copy(errorMessage = null) }
    }
}
