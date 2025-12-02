package com.example.logflare_android.feature.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ProjectDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * UI model for the upcoming Project Detail surface. The current implementation
 * focuses on presenting a rich layout that mirrors the wireframe so we can
 * iterate on visuals before wiring real data sources.
 */
data class ProjectDetailUiState(
    val loading: Boolean = true,
    val projectId: Int = 0,
    val projectName: String = "",
    val statusTimeLabel: String = LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm")),
    val settingsLabel: String = "Project Settings",
    val logs: List<ProjectDetailLog> = emptyList(),
    val filterState: ProjectDetailFilterState = ProjectDetailFilterState()
)

data class ProjectDetailLog(
    val id: Int,
    val level: ProjectLogLevel,
    val timestamp: String,
    val message: String,
    val projectName: String,
    val fileName: String
)

data class ProjectDetailFilterState(
    val selectedLevel: ProjectLogLevel = ProjectLogLevel.CIRITCAL,
    val showOnlyOptions: List<ProjectDetailFilterOption> = defaultShowOnlyOptions(),
    val sortOptions: List<ProjectDetailFilterOption> = defaultSortOptions()
) {
    companion object {
        private fun defaultShowOnlyOptions() = listOf(
            ProjectDetailFilterOption(key = "timestamp", label = "Timestamp"),
            ProjectDetailFilterOption(key = "level", label = "Log Level"),
            ProjectDetailFilterOption(key = "source", label = "Source"),
            ProjectDetailFilterOption(key = "message", label = "Message")
        )

        private fun defaultSortOptions() = listOf(
            ProjectDetailFilterOption(key = "newest", label = "Newest", selected = true),
            ProjectDetailFilterOption(key = "oldest", label = "Oldest"),
            ProjectDetailFilterOption(key = "level_desc", label = "Level ↓"),
            ProjectDetailFilterOption(key = "level_asc", label = "Level ↑")
        )
    }
}

data class ProjectDetailFilterOption(
    val key: String,
    val label: String,
    val selected: Boolean = false
)

enum class ProjectLogLevel(val displayName: String) {
    //    TRACE("TRACE"),
    DEBUG("DEBUG"),
    INFO("INFO"),
    WARNING("WARNING"),
    ERROR("ERROR"),
    CIRITCAL("CIRITCAL")
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectDetailUseCase: GetProjectDetailUseCase,
    private val getProjectLogsUseCase: GetProjectLogsUseCase,
) : ViewModel() {

    private val projectId: Int = savedStateHandle["projectId"] ?: 0

    private val _ui = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    // TODO: 과연 이게 필요할까요 어차피 시간은 내 핸드폰에서 나오는건데
    val ui: StateFlow<ProjectDetailUiState> = _ui

    init {
        hydratePlaceholder()
        viewModelScope.launch {
            getLogs()
        }
    }

    private fun hydratePlaceholder() {
        val placeholderLogs = (0 until 5).map { index ->
            ProjectDetailLog(
                id = index,
                level = ProjectLogLevel.CIRITCAL,
                timestamp = "2025.11.16 23:58:09",
                message = "User login request received for user=hello\n" +
                        "Authentication failed: invalid password\n" +
                        "Access token expired, requesting refresh\n" +
                        "User successfully logged out\n" +
                        "2FA verification failed (code mismatch)",
                projectName = "Project $projectId",
                fileName = "File${index + 1}"
            )
        }

        _ui.value = ProjectDetailUiState(
            loading = false,
            projectId = projectId,
            projectName = "Project $projectId",
            logs = placeholderLogs
        )
    }

    private suspend fun getLogs() {
        val project = getProjectDetailUseCase(projectId) ?: return
        addInitialLogs(project)
        val logfiles = project.logfiles ?: return
        var first = true
        for (logfile in logfiles) {
            val logs = getProjectLogsUseCase(
                projectId,
                project.name,
                logfile.id,
                logfile.file_name
            )?: continue
            addLogs(project, logs, first)
            first = false
        }
    }

    private fun addInitialLogs(project: ProjectDTO){
        val defaultLog = ProjectDetailLog(
            id = 0,
            level = ProjectLogLevel.CIRITCAL,
            timestamp = "2025.11.16 23:58:09",
            message = "This is a DEFAULT placeholder for log\n\n" +
                    "If this message keeps displayed please check your logfile configuration",
            projectName = "Project $projectId",
            fileName = "File 1"
        )
        _ui.value = ProjectDetailUiState(
            loading = false,
            projectId = project.id,
            projectName = project.name,
            logs = listOf(defaultLog),
        )
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private fun addLogs(
        project: ProjectDTO,
        newLogs: List<ProjectDetailLog>,
        first: Boolean,
        inverted: Boolean = false
    ) {
        val prev = _ui.value.logs

        val merged = if (first) {
            newLogs
        } else {
            prev + newLogs
        }
        // timestamp 기준 정렬
        var sorted = merged.sortedByDescending { LocalDateTime.parse(it.timestamp, formatter) }
        if (inverted) {
            sorted = merged.sortedBy { LocalDateTime.parse(it.timestamp, formatter) }
        }

        _ui.value = ProjectDetailUiState(
            loading = false,
            projectId = project.id,
            projectName = project.name,
            logs = sorted
        )
    }

}
