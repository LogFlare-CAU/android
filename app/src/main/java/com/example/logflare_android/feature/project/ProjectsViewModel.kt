package com.example.logflare_android.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare_android.data.ProjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val loading: Boolean = false,
    val items: List<ProjectDTO> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for managing project list state.
 */
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repo: ProjectsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ProjectsUiState(loading = true))
    val ui: StateFlow<ProjectsUiState> = _ui

    fun refresh() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.list()
                .onSuccess { list -> _ui.value = ProjectsUiState(loading = false, items = list.map { it.dto }) }
                .onFailure { e -> _ui.value = ProjectsUiState(loading = false, error = e.message) }
        }
    }
}
