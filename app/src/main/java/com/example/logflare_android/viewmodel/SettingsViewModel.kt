package com.example.logflare_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                authRepository.clearToken()
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
