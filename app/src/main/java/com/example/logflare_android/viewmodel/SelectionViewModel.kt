package com.example.logflare_android.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SelectionViewModel @Inject constructor(): ViewModel() {
    private val _projectId = MutableStateFlow<Int?>(null)
    val projectId: StateFlow<Int?> = _projectId

    fun selectProject(id: Int) { _projectId.value = id }
}
