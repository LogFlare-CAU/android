package com.example.logflare_android.feature.log

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare_android.data.LogsRepository
import com.example.logflare_android.data.ProjectsRepository
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.LogSort
import com.example.logflare_android.feature.usecase.GetLogsUseCase
import com.example.logflare_android.ui.common.LogCardInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LogsUiState(
    val loading: Boolean = false,
    val errorLogs: List<ErrorlogDTO> = emptyList(),
    val error: String? = null,
    val filter: List<LogLevel> = emptyList(),
    val projectNames: Map<Int, String> = emptyMap(),
    val selectedProject: Int? = null,
    val projectOptions: List<ProjectToggleOption> = emptyList(),
    val sortBy: LogSort = LogSort.NEWEST,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true
)


/**
 * ViewModel for managing log list state with filtering support.
 */
@HiltViewModel
class LogViewModel @Inject constructor(
    private val repo: LogsRepository,
    private val projectsRepo: ProjectsRepository,
    private val getlogsUseCase: GetLogsUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(LogsUiState())
    val ui: StateFlow<LogsUiState> = _ui

    private var allLogs: List<ErrorlogDTO> = emptyList()

    private val pageSize = 20
    private var currentOffset = 0

    init {
        getLogs()
        getProjectOptions()
    }

    fun getLogs(
        limit: Int = pageSize,
        offset: Int = 0,
        projectid: Int? = _ui.value.selectedProject,
        sortBy: LogSort = _ui.value.sortBy
    ) {
        val isLoadMore = offset > 0
        if (isLoadMore) {
            _ui.value = _ui.value.copy(
                loadingMore = true,
                error = null
            )
        } else {
            allLogs = emptyList()
            _ui.value = _ui.value.copy(
                loading = true,
                error = null,
                errorLogs = emptyList(),
                hasMore = true,
                loadingMore = false
            )
        }
        viewModelScope.launch {
            if (!isLoadMore) {
                projectsRepo.list().onSuccess { list ->
                    val names = list.associate { p -> p.dto.id to p.dto.name }
                    _ui.value = _ui.value.copy(
                        projectNames = names
                    )
                }
            }
            getlogsUseCase(
                limit = limit,
                offset = offset,
                projectId = projectid,
                sortBy = sortBy
            )
                .onSuccess { newItems ->
                    allLogs = if (isLoadMore) {
                        allLogs + newItems
                    } else {
                        newItems
                    }

                    val hasMore = newItems.size == limit
                    val currentFilter = _ui.value.filter
                    val uiLogs = if (currentFilter.isEmpty()) {
                        allLogs
                    } else {
                        allLogs.filter { currentFilter.map { it.name }.contains(it.level.uppercase()) }
                    }
                    _ui.value = _ui.value.copy(
                        errorLogs = uiLogs,
                        loading = false,
                        loadingMore = false,
                        hasMore = hasMore,
                        error = null
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = e.message
                    )
                }
        }
    }


    fun loadMore() {
        if (!_ui.value.hasMore || _ui.value.loadingMore) return
        currentOffset += pageSize
        getLogs(limit = pageSize, offset = currentOffset)
    }


    fun setFilter(level: LogLevel) {
        val currentFilter = _ui.value.filter.toMutableList()
        if (currentFilter.contains(level)) {
            currentFilter.remove(level)
        } else {
            currentFilter.add(level)
        }
        _ui.value = _ui.value.copy(filter = currentFilter)
        val uiLogs = if (currentFilter.isEmpty()) {
            allLogs
        } else {
            allLogs.filter { it.level.uppercase() in currentFilter.map { it.name } }
        }
        _ui.value = _ui.value.copy(
            errorLogs = uiLogs
        )
    }

    fun getProjectOptions() {
        viewModelScope.launch {
            val projects = projectsRepo.getAll()
            Log.d("LogViewModel", "Currently selected project: ${_ui.value.selectedProject}")
            val options = projects.map {
                Log.d(
                    "LogViewModel",
                    "Adding project option: ${it.dto.id} - ${it.dto.name}, selected=${_ui.value.selectedProject == it.dto.id}"
                )
                ProjectToggleOption(
                    id = it.dto.id,
                    label = it.dto.name,
                    selected = (_ui.value.selectedProject == it.dto.id)
                )

            }
            _ui.value = _ui.value.copy(projectOptions = options)
        }
    }

    fun toggleProjectOption(projectId: Int) {
        if (_ui.value.selectedProject == projectId) {
            _ui.value = _ui.value.copy(selectedProject = null)
            Log.d("LogViewModel", "Clearing selected project filter")
            getLogs()
        } else {
            Log.d("LogViewModel", "Setting selected project filter to $projectId")
            _ui.value = _ui.value.copy(selectedProject = projectId)
            getLogs(projectid = projectId)
        }
        getProjectOptions()
    }

    fun setSortBy(sortBy: LogSort) {
        _ui.value = _ui.value.copy(sortBy = sortBy)
        getLogs(
            projectid = _ui.value.selectedProject,
            sortBy = sortBy
        )
    }

    fun onLogClick(log: ErrorlogDTO) {
        repo.selectLog(
            LogCardInfo(
                level = log.level,
                timestamp = log.timestamp,
                message = log.message,
                prefix = _ui.value.projectNames[log.project_id] ?: "Project #${log.project_id}",
                suffix = log.errortype ?: "Unknown"
            )
        )
    }

}
