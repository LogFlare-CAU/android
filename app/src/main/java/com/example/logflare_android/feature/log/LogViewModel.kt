package com.example.logflare_android.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare_android.data.LogsRepository
import com.example.logflare_android.data.ProjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LogsUiState(
    val loading: Boolean = false,
    val items: List<ErrorlogDTO> = emptyList(),
    val error: String? = null,
    val filter: LogLevel? = null,
    val projectNames: Map<Int, String> = emptyMap()
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL
}

/**
 * ViewModel for managing log list state with filtering support.
 */
@HiltViewModel
class LogViewModel @Inject constructor(
    private val repo: LogsRepository,
    private val projectsRepo: ProjectsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(LogsUiState())
    val ui: StateFlow<LogsUiState> = _ui
    
    private var allLogs: List<ErrorlogDTO> = emptyList()

    fun refresh(projectId: Int, limit: Int = 50) {
        _ui.value = LogsUiState(loading = true)
        viewModelScope.launch {
            repo.getErrors(projectId, limit = limit)
                .onSuccess { list ->
                    allLogs = list
                    _ui.value = LogsUiState(
                        items = list,
                        projectNames = mapOf(projectId to (_ui.value.projectNames[projectId] ?: "Project #$projectId"))
                    )
                }
                .onFailure { e -> _ui.value = LogsUiState(error = e.message) }
        }
    }

    fun refreshAllProjects(limitPerProject: Int = 20) {
        _ui.value = LogsUiState(loading = true)
        viewModelScope.launch {
            projectsRepo.list()
                .onSuccess { projects ->
                    val names = projects.associate { it.id to it.name }
                    val aggregated = mutableListOf<ErrorlogDTO>()
                    var firstError: String? = null
                    for (project in projects) {
                        repo.getErrors(project.id, limit = limitPerProject)
                            .onSuccess { aggregated.addAll(it) }
                            .onFailure { e -> if (firstError == null) firstError = e.message }
                    }
                    aggregated.sortByDescending { it.timestamp }
                    allLogs = aggregated
                    _ui.value = LogsUiState(
                        items = aggregated,
                        error = if (aggregated.isEmpty()) firstError else null,
                        projectNames = names
                    )
                }
                .onFailure { e -> _ui.value = LogsUiState(error = e.message) }
        }
    }
    
    fun setFilter(level: LogLevel?) {
        val filtered = if (level == null) {
            allLogs
        } else {
            allLogs.filter { it.level.uppercase() == level.name }
        }
        _ui.value = _ui.value.copy(items = filtered, filter = level)
    }
}
