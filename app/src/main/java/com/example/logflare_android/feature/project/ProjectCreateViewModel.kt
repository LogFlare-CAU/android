package com.example.logflare_android.feature.project

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.ProjectsRepository
import com.example.logflare_android.enum.LogLevel
import com.example.logflare_android.enum.UserPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectCreateUiState(
    val id: Int = 0,
    val name: String = "",
    val nameValid: Boolean = false,
    val loading: Boolean = false,
    val token: String? = null,
    val error: String? = null,
    val keywords: List<String> = emptyList(),
    val keywordInput: String = "",
    val keywordError: String? = null,
    val alertLevels: Set<String> = emptySet(),
    val saved: Boolean = false,
    val snackbar: String? = null,
    val permissions: List<PermissionToggleState> = emptyList(),
)

@HiltViewModel
class ProjectCreateViewModel @Inject constructor(
    private val repo: ProjectsRepository,
    private val getUsersUseCase: GetUsersUseCase,
    private val authRepository: AuthRepository,
    private val updateProjectPermUseCase: UpdateProjectPermUseCase
) : ViewModel() {
    private val _ui = MutableStateFlow(ProjectCreateUiState())
    val ui: StateFlow<ProjectCreateUiState> = _ui

    init {
        initPermissions()
        toggleAlertLevel(LogLevel.WARNING.label)
    }

    private val nameRegex = Regex("^[\\p{IsHangul}\\p{IsLatin}\\p{N}\\p{P}\\p{Zs}]+$")
    private val keywordRegex = Regex("^[A-Za-z0-9 ]+$")

    fun onNameChanged(value: String) {
        _ui.value = _ui.value.copy(name = value, nameValid = nameRegex.matches(value))
    }

    fun onKeywordInputChanged(value: String) {
        _ui.value = _ui.value.copy(keywordInput = value, keywordError = null)
    }

    fun addKeyword() {
        val input = _ui.value.keywordInput.trim()
        if (input.isEmpty()) return
        if (!keywordRegex.matches(input)) {
            _ui.value = _ui.value.copy(keywordError = "Use English letters and numbers only")
            return
        }
        viewModelScope.launch {
            repo.addKeyword(_ui.value.id, input)
                .onSuccess { updated ->
                    _ui.value = _ui.value.copy(keywords = updated.toList(), keywordInput = "", keywordError = null)
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(keywordError = e.message ?: "Unknown error")
                }
        }
    }

    fun removeKeyword(k: String) {
        viewModelScope.launch {
            repo.removeKeyword(_ui.value.id, k).onSuccess { updated ->
                _ui.value = _ui.value.copy(keywords = updated.toList())
            }
        }
    }

    fun toggleAlertLevel(level: String) {
        val levels = LogLevel.getAboveLevel(level)
        var set = emptySet<String>()
        for (l in levels) set = set.plus(l.label)
        _ui.value = _ui.value.copy(alertLevels = set)
        viewModelScope.launch {
            repo.setAlertLevel(ui.value.id, level)
        }
    }

    fun saveProject() {
        // create project via repo
        val name = _ui.value.name.trim()
        if (!_ui.value.nameValid) return
        val alreadySaved = _ui.value.saved
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.create(name)
                .onSuccess { dto ->
                    val message = if (alreadySaved) "Project name updated successfully" else "Project created"
                    _ui.value = _ui.value.copy(
                        loading = false,
                        token = dto.token,
                        saved = true,
                        snackbar = message,
                        id = dto.id
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Unknown error")
                }
        }
    }

    fun editProject() {
        // For now, editing will do noting (placeholder)
        // saveProject()
    }

    fun clearSnackbar() {
        _ui.value = _ui.value.copy(snackbar = null)
    }

    //================= Permission Toggles =================//

    fun initPermissions() {
        val superUser = PermissionToggleState(
            username = "{{username}}",
            role = UserPermission.SUPER_USER.label,
            rolenum = UserPermission.SUPER_USER.code,
            roleColor = Color(0xFF1A1A1A),
            activeColor = UserPermission.SUPER_USER.color,
            inactiveColor = Color(0xFFCCCCCC),
            active = true
        )

        val adminUser = PermissionToggleState(
            username = "{{username}}",
            role = UserPermission.MODERATOR.label,
            rolenum = UserPermission.MODERATOR.code,
            roleColor = Color(0xFF1A1A1A),
            activeColor = UserPermission.MODERATOR.color,
            inactiveColor = Color(0xFFCCCCCC),
            active = true
        )

        val memberUser = PermissionToggleState(
            username = "{{username}}",
            role = UserPermission.USER.label,
            rolenum = UserPermission.USER.code,
            roleColor = Color(0xFF1A1A1A),
            activeColor = UserPermission.USER.color,
            inactiveColor = Color(0xFFC2C2C2),
            active = false
        )
        _ui.update { current ->
            current.copy(
                permissions = listOf(
                    superUser.copy(),
                    adminUser.copy(),
                    memberUser.copy()
                )
            )
        }
        viewModelScope.launch {
            val users = getUsersUseCase() ?: return@launch
            val permissionStates = users.map { user ->
                when {
                    user.permission >= UserPermission.SUPER_USER.code -> superUser.copy(
                        username = user.username,
                        active = true
                    )

                    user.permission >= UserPermission.MODERATOR.code -> adminUser.copy(
                        username = user.username,
                        active = true
                    )

                    else -> memberUser.copy(username = user.username, active = false)
                }
            }
            _ui.value = _ui.value.copy(permissions = permissionStates)
        }
    }

    fun onPermissionToggle(index: Int, checked: Boolean) {
        viewModelScope.launch {
            val currentUsername = authRepository.getUsername()
            _ui.value = _ui.value.copy(
                permissions = _ui.value.permissions.mapIndexed { i, perm ->
                    if (perm.rolenum >= UserPermission.SUPER_USER.code) {
                        perm
                    } else if (currentUsername != null && perm.username == currentUsername) {
                        perm
                    } else {
                        if (i == index) perm.copy(active = checked) else perm
                    }
                }
            )
        }
    }

    //================= Permission Toggles =================//
    fun savePerms() {
        val currentProjectId = _ui.value.id
        val users: Set<String> = _ui.value.permissions
            .filter { it.active }
            .map { it.username }
            .toSet()
        viewModelScope.launch {
            updateProjectPermUseCase(currentProjectId, users)
        }
    }

}
