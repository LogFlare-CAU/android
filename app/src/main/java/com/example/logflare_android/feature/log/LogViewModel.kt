package com.example.logflare_android.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare_android.data.LogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LogsUiState(
    val loading: Boolean = false,
    val items: List<ErrorlogDTO> = emptyList(),
    val error: String? = null,
    val filter: LogLevel? = null
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL
}

/**
 * ViewModel for managing log list state with filtering support.
 */
@HiltViewModel
class LogViewModel @Inject constructor(
    private val repo: LogsRepository
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
                    _ui.value = LogsUiState(items = list)
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
