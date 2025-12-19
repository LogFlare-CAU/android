package com.example.logflare_android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * ViewModel for settings/profile management.
 */
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
