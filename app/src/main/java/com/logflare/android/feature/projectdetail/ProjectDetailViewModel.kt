package com.logflare.android.feature.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.LogFileDTO
import com.example.logflare.core.model.ProjectData
import com.logflare.android.enums.LogLevel
import com.logflare.android.feature.log.PendingLogDetailStore
import com.logflare.android.enums.LogSort
import com.logflare.android.feature.usecase.GetProjectDetailUseCase
import com.logflare.android.feature.usecase.GetProjectLogsUseCase
import com.logflare.android.ui.common.LogCardInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI model for the upcoming Project Detail surface. The current implementation
 * focuses on presenting a rich layout that mirrors the wireframe so we can
 * iterate on visuals before wiring real data sources.
 */
data class ProjectDetailUiState(
    val loading: Boolean = true,
    val projectId: Int = 0,
    val projectName: String = "",
    val settingsLabel: String = "Project Settings",
    val logs: List<ProjectDetailLog> = emptyList(),
    val filterState: ProjectDetailFilterState = ProjectDetailFilterState(),
    val showMoreLoading: Boolean = false,
    val showMoreHasMore: Boolean = true,
    val error: String? = null,
)

data class ProjectDetailLog(
    val id: Int,
    val level: LogLevel,
    val timestamp: String,
    val message: String,
    val projectName: String,
    val fileName: String
)

data class ProjectDetailFilterState(
    val selectedLevel: List<LogLevel> = emptyList(),
    val logfileOptions: List<ProjectLogFileOption> = defaultLogFileOptions(),
    val sortBy: LogSort = LogSort.NEWEST,
) {
    companion object {
        private fun defaultLogFileOptions() = listOf(
            ProjectLogFileOption(id = 1, fileName = "Debug", selected = true),
            ProjectLogFileOption(id = 2, fileName = "Warning"),
            ProjectLogFileOption(id = 3, fileName = "NetWork"),
            ProjectLogFileOption(id = 4, fileName = "Frontend")
        )
    }
}

data class ProjectLogFileOption(
    val id: Int,
    val fileName: String,
    val selected: Boolean = false
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectDetailUseCase: GetProjectDetailUseCase,
    private val getProjectLogsUseCase: GetProjectLogsUseCase,
    private val pendingLogDetailStore: PendingLogDetailStore,
) : ViewModel() {

    private val projectId: Int = savedStateHandle["projectId"] ?: 0
    private var project: ProjectData? = null
    private var selectedLogfile: LogFileDTO? = null
    private var globalOffset: Int = 0
    private var allLogs: List<ProjectDetailLog> = emptyList()
    private var sortBy: LogSort = LogSort.NEWEST
    private val _ui = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val ui: StateFlow<ProjectDetailUiState> = _ui

    init {
        viewModelScope.launch {
            project = getProjectDetailUseCase(projectId)
            if (project == null) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = "Project not found",
                )
                return@launch
            }
            hydratePlaceholder()
            addInitialLogs()
            fillLogFiles()
            getLogs()
        }
    }

    private fun hydratePlaceholder() {
        val defaultLog = ProjectDetailLog(
            id = 0,
            level = LogLevel.CRITICAL,
            timestamp = "2025.11.16 23:58:09",
            message = "This is a DEFAULT placeholder for log\n\n" +
                    "If this message keeps displayed your project is no longer available or not found",
            projectName = "Project $projectId",
            fileName = "File 1"
        )
        _ui.value = ProjectDetailUiState(
            loading = false,
            projectId = 0,
            projectName = "Loading...",
            logs = listOf(defaultLog),
        )
    }

    private fun addInitialLogs() {
        val defaultLog = ProjectDetailLog(
            id = 0,
            level = LogLevel.CRITICAL,
            timestamp = "2025.11.16 23:58:09",
            message = "This is a DEFAULT placeholder for log\n\n" +
                    "If this message keeps displayed please check your logfile configuration",
            projectName = "Project $projectId",
            fileName = "File 1"
        )
        val dto = project?.dto ?: return
        _ui.value = _ui.value.copy(
            loading = false,
            projectId = dto.id,
            projectName = dto.name,
            logs = listOf(defaultLog),
        )
    }

    private fun fillLogFiles() {
        val logfiles = project?.dto?.logfiles ?: return
        if (logfiles.isEmpty()) return
        var isFirst = true
        val options = logfiles.map { logfile ->
            val selected = isFirst
            if (isFirst) {
                selectedLogfile = logfile
                isFirst = false
            }
            ProjectLogFileOption(
                id = logfile.id,
                fileName = logfile.file_name,
                selected = selected
            )
        }
        _ui.value = _ui.value.copy(
            filterState = _ui.value.filterState.copy(
                logfileOptions = options
            )
        )
    }

    fun loadMoreLogs() {
        _ui.value = _ui.value.copy(showMoreLoading = true)
        globalOffset += 50
        viewModelScope.launch {
            getLogs(offset = globalOffset)
        }
    }

    private suspend fun getLogs(limit: Int = 50, offset: Int = 0) {
        val dto = project?.dto
        if (dto == null) {
            _ui.value = _ui.value.copy(
                showMoreLoading = false,
                error = _ui.value.error ?: "Project not found",
            )
            return
        }
        val logfile = selectedLogfile
        if (logfile == null) {
            _ui.value = _ui.value.copy(
                showMoreLoading = false,
                error = _ui.value.error ?: "No log file selected",
            )
            return
        }
        val logs = getProjectLogsUseCase(
            projectId = dto.id,
            projectName = dto.name,
            logfileId = logfile.id,
            fileName = logfile.file_name,
            limit = limit,
            offset = offset,
            sortBy = sortBy
        )
        if (logs == null) {
            _ui.value = _ui.value.copy(
                loading = false,
                showMoreLoading = false,
                error = "Failed to load logs",
            )
            return
        }
        val first = offset == 0
        addLogs(
            newLogs = logs,
            first = first
        )
        _ui.value = _ui.value.copy(
            showMoreLoading = false,
            showMoreHasMore = logs.size >= limit,
        )
    }


    private fun addLogs(
        newLogs: List<ProjectDetailLog>,
        first: Boolean,
    ) {
        val project = project?.dto ?: return
        allLogs = if (first) {
            newLogs
        } else {
            allLogs + newLogs
        }
        _ui.value = _ui.value.copy(
            loading = false,
            projectId = project.id,
            projectName = project.name,
            logs = filterLogs()
        )
    }

    fun filterLogs(): List<ProjectDetailLog> {
        val currentLevels = _ui.value.filterState.selectedLevel
        return if (currentLevels.isEmpty()) {
            allLogs
        } else {
            allLogs.filter { currentLevels.contains(it.level) }
        }
    }

    fun onSortSelected(sortBy: LogSort) {
        if (this.sortBy == sortBy) return
        this.sortBy = sortBy
        _ui.value = _ui.value.copy(
            filterState = _ui.value.filterState.copy(
                sortBy = sortBy
            )
        )
        globalOffset = 0
        viewModelScope.launch {
            getLogs()
        }
    }

    fun onLevelSelected(level: LogLevel) {
        val currentLevels = _ui.value.filterState.selectedLevel.toMutableList()
        if (currentLevels.contains(level)) {
            currentLevels.remove(level)
        } else {
            currentLevels.add(level)
        }
        _ui.value = _ui.value.copy(
            filterState = _ui.value.filterState.copy(
                selectedLevel = currentLevels
            )
        )
        _ui.value = _ui.value.copy(
            logs = filterLogs()
        )
    }

    fun onLogfileSelected(logfileId: Int) {
        addInitialLogs()
        globalOffset = 0
        val project = project?.dto ?: return
        val logfile = project.logfiles?.find { it.id == logfileId } ?: return
        selectedLogfile = logfile
        _ui.value = _ui.value.copy(
            filterState = _ui.value.filterState.copy(
                logfileOptions = _ui.value.filterState.logfileOptions.map {
                    it.copy(selected = it.id == logfileId)
                }
            ),
            showMoreHasMore = true,
        )
        viewModelScope.launch {
            getLogs()
        }
    }

    fun onLogClick(log: ProjectDetailLog) {
        pendingLogDetailStore.setPending(
            LogCardInfo(
                level = log.level.label,
                timestamp = log.timestamp,
                message = log.message,
                prefix = log.projectName,
                suffix = log.fileName
            )
        )
    }
}
