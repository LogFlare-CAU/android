package com.logflare.android.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ProjectDTO
import com.logflare.android.data.ProjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val items: List<ProjectDTO> = emptyList(),
    val error: String? = null,
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

    /**
     * @param fromPull when true and the list already has items, only [ProjectsUiState.refreshing] is shown (pull-to-refresh).
     */
    fun refresh(fromPull: Boolean = false) {
        val hasItems = _ui.value.items.isNotEmpty()
        val useBlockingLoader = !fromPull || !hasItems
        if (useBlockingLoader) {
            _ui.update { it.copy(loading = true, refreshing = false, error = null) }
        } else {
            _ui.update { it.copy(refreshing = true, error = null) }
        }
        viewModelScope.launch {
            repo.list()
                .onSuccess { list ->
                    _ui.value = ProjectsUiState(
                        loading = false,
                        refreshing = false,
                        items = list.map { it.dto },
                        error = null,
                    )
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = e.message,
                        )
                    }
                }
        }
    }
}
