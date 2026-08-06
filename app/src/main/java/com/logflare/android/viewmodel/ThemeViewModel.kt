package com.logflare.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val preference: StateFlow<ThemePreference> = themeRepository.preference.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreference.SYSTEM,
    )

    fun setPreference(preference: ThemePreference) {
        viewModelScope.launch {
            themeRepository.setPreference(preference)
        }
    }

    fun toggleLightDark(systemDark: Boolean) {
        viewModelScope.launch {
            val next = themeRepository.toggleLightDark(preference.value, systemDark)
            themeRepository.setPreference(next)
        }
    }
}
