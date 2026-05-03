package com.logflare.android.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.AuthRepository
import com.logflare.android.data.ProjectsRepository
import com.logflare.android.enums.LogLevel
import com.logflare.android.enums.UserPermission
import com.logflare.android.feature.usecase.GetProjectPermsUseCase
import com.logflare.android.feature.usecase.GetUsersUseCase
import com.logflare.android.feature.usecase.UpdateProjectPermUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared project create/settings editor logic. Screen-specific [ProjectCreateViewModel] and
 * [ProjectSettingsViewModel] extend this so each navigation destination gets its own instance.
 */
open class ProjectEditorViewModel(
    private val repo: ProjectsRepository,
    private val authRepository: AuthRepository,
    private val updateProjectPermUseCase: UpdateProjectPermUseCase,
    private val getUsersUseCase: GetUsersUseCase,
    private val getProjectpermsUseCase: GetProjectPermsUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(ProjectCreateUiState())
    val ui: StateFlow<ProjectCreateUiState> = _ui

    init {
        initPermissions()
        toggleAlertLevel(LogLevel.WARNING.label)
    }

    private var projectId: Int? = null

    private fun updateUi(block: ProjectCreateUiState.() -> ProjectCreateUiState) {
        _ui.update { current ->
            current.block()
        }
    }

    fun initWithProject(projectId: Int) {
        viewModelScope.launch {
            val proj = repo.get(projectId) ?: return@launch
            setProjectId(projectId)
            updateUi {
                copy(
                    id = proj.dto.id,
                    name = proj.dto.name,
                    nameValid = true,
                    keywords = proj.excludeKeywords.toList(),
                    alertLevels = LogLevel.getAboveLevel(proj.alertLevel).map { it.label }.toSet(),
                    saved = true,
                    token = null,
                )
            }
            getPermissions()?.let { perms ->
                updateUi { copy(permissions = perms) }
            }
        }
    }

    fun setProjectId(id: Int) {
        projectId = id
    }

    fun onNameChanged(value: String) {
        updateUi { copy(name = value, nameValid = ProjectEditorValidation.isProjectNameValid(value)) }
    }

    fun onKeywordInputChanged(value: String) {
        updateUi { copy(keywordInput = value, keywordError = null) }
    }

    fun addKeyword() {
        val projectId = projectId ?: return
        val input = _ui.value.keywordInput.trim()
        if (input.isEmpty()) return
        if (!ProjectEditorValidation.isKeywordValid(input)) {
            updateUi { copy(keywordError = "Use English letters and numbers only") }
            return
        }
        viewModelScope.launch {
            repo.addKeyword(projectId, input)
                .onSuccess { updated ->
                    updateUi { copy(keywords = updated.toList(), keywordInput = "", keywordError = null) }
                }
                .onFailure { e ->
                    updateUi { copy(keywordError = e.message ?: "Unknown error") }
                }
        }
    }

    fun removeKeyword(k: String) {
        val projectId = projectId ?: return
        viewModelScope.launch {
            repo.removeKeyword(projectId, k).onSuccess { updated ->
                updateUi {
                    copy(keywords = updated.toList())
                }
            }
        }
    }

    fun toggleAlertLevel(level: String) {
        val levels = LogLevel.getAboveLevel(level)
        var set = emptySet<String>()
        for (l in levels) set = set.plus(l.label)
        updateUi { copy(alertLevels = set) }
        viewModelScope.launch {
            repo.setAlertLevel(ui.value.id, level)
        }
    }

    fun saveProject() {
        val name = _ui.value.name.trim()
        if (!_ui.value.nameValid) return
        val alreadySaved = _ui.value.saved
        updateUi { copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.create(name)
                .onSuccess { dto ->
                    val message = if (alreadySaved) "Project name updated successfully" else "Project created"
                    updateUi {
                        copy(
                            loading = false,
                            token = dto.token,
                            saved = true,
                            snackbar = message,
                            id = dto.id,
                        )
                    }
                    setProjectId(dto.id)
                }
                .onFailure { e ->
                    updateUi { copy(loading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun editProject() {
        val projectId = projectId ?: return
        val name = _ui.value.name.trim()
        if (!_ui.value.nameValid) return
        updateUi { copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.rename(projectId, name)
                .onSuccess {
                    updateUi {
                        copy(
                            loading = false,
                            snackbar = "Project name updated successfully",
                        )
                    }
                }
                .onFailure { e ->
                    updateUi { copy(loading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun deleteProject(onSuccess: () -> Unit) {
        if (_ui.value.loading) return
        val projectId = projectId ?: return
        updateUi { copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                repo.delete(projectId)
                onSuccess()
            } catch (e: Exception) {
                updateUi { copy(loading = false, error = e.message) }
            }
        }
    }

    fun clearSnackbar() {
        updateUi { copy(snackbar = null) }
    }

    suspend fun getPermissions(): List<PermissionToggleState>? {
        val perms = projectId?.let { getProjectpermsUseCase(it) }
        val users = getUsersUseCase() ?: return null
        return ProjectPermissionUiFactory.mapUsersToPermissionStates(users, perms)
    }

    private fun initPermissions() {
        updateUi { copy(permissions = ProjectPermissionUiFactory.hydratePlaceholder()) }
        viewModelScope.launch {
            val perms = getPermissions() ?: return@launch
            updateUi { copy(permissions = perms) }
        }
    }

    private fun getUiPermissions(): List<PermissionToggleState> {
        return _ui.value.permissions
    }

    fun onPermissionToggle(index: Int, checked: Boolean) {
        viewModelScope.launch {
            val currentUsername = authRepository.getUsername()
            _ui.value = _ui.value.copy(
                permissions = getUiPermissions().mapIndexed { i, perm ->
                    if (perm.rolenum >= UserPermission.SUPER_USER.code) {
                        perm
                    } else if (currentUsername != null && perm.username == currentUsername) {
                        perm
                    } else {
                        if (i == index) perm.copy(active = checked) else perm
                    }
                },
            )
        }
    }

    fun savePerms() {
        val projectId = projectId ?: return
        val users: Set<String> = getUiPermissions()
            .filter { it.active }
            .map { it.username }
            .toSet()
        viewModelScope.launch {
            updateProjectPermUseCase(projectId, users).onFailure { e ->
                updateUi { copy(snackbar = e.message ?: "Failed to update permissions") }
            }
        }
    }
}
