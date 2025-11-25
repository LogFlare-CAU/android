package com.example.logflare_android.feature.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI model for the upcoming Project Detail surface. The current implementation
 * focuses on presenting a rich layout that mirrors the wireframe so we can
 * iterate on visuals before wiring real data sources.
 */
data class ProjectDetailUiState(
    val loading: Boolean = true,
    val projectId: Int = 0,
    val projectName: String = "",
    val statusTimeLabel: String = "12:30",
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
    val selectedLevel: ProjectLogLevel = ProjectLogLevel.FATAL,
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
    TRACE("TRACE"),
    DEBUG("DEBUG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    FATAL("FATAL")
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: Int = savedStateHandle["projectId"] ?: 0

    private val _ui = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val ui: StateFlow<ProjectDetailUiState> = _ui

    init {
        hydratePlaceholder()
    }

    private fun hydratePlaceholder() {
        val placeholderLogs = (0 until 5).map { index ->
            ProjectDetailLog(
                id = index,
                level = ProjectLogLevel.FATAL,
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
}
