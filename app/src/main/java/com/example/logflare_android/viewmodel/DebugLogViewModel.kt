package com.example.logflare_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DebugLogViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {
    fun postSampleError() {
        viewModelScope.launch {
            logRepository.postError(level = "ERROR", message = "Sample error from Android", errortype = "sample")
        }
    }
}
