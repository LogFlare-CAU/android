package com.example.logflare_android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogoutUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LogoutViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(LogoutUiState())
    val ui: StateFlow<LogoutUiState> = _ui

    fun performLogout(onSuccess: () -> Unit) {
        if (_ui.value.isLoading) return

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                delay(800)
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
