package com.example.logflare_android.feature.project

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.ProjectPermsDTO
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.ProjectsRepository
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.project.usecase.GetProjectPermsUseCase
import com.example.logflare_android.feature.project.usecase.GetUsersUseCase
import com.example.logflare_android.feature.project.usecase.UpdateProjectPermUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class PermissionToggleState(
    val username: String,
    val role: String,
    val rolenum: Int = 0,
    val roleColor: Color, // TODO: 이거 안쓰는데 왜 있는건지요?
    val activeColor: Color,
    val inactiveColor: Color,
    val active: Boolean
)


@HiltViewModel
class ProjectCommonViewModel @Inject constructor(
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

    private val nameRegex = Regex("^[\\p{IsHangul}\\p{IsLatin}\\p{N}\\p{P}\\p{Zs}]+$")
    private val keywordRegex = Regex("^[A-Za-z0-9 ]+$")

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
                    token = "0" // 이건 더미값입니다. 실제 토큰은 가져올 수 없습니다. 단지 UI 용도일 뿐입니다.
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
        updateUi { copy(name = value, nameValid = nameRegex.matches(value)) }
    }

    fun onKeywordInputChanged(value: String) {
        updateUi { copy(keywordInput = value, keywordError = null) }
    }

    fun addKeyword() {
        val projectId = projectId ?: return
        val input = _ui.value.keywordInput.trim()
        if (input.isEmpty()) return
        if (!keywordRegex.matches(input)) {
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
        // create project via repo
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
                            id = dto.id
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
                            snackbar = "Project name updated successfully"
                        )
                    }
                }
                .onFailure { e ->
                    updateUi { copy(loading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun deleteProject() {
        val projectId = projectId ?: return
        updateUi { copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.delete(projectId)
        }
    }


    fun clearSnackbar() {
        updateUi { copy(snackbar = null) }
    }

    //================= Permission =================//

    fun superUser(username: String = "{{username}}", active: Boolean = true) = PermissionToggleState(
        username = username,
        role = UserPermission.SUPER_USER.label,
        rolenum = UserPermission.SUPER_USER.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.SUPER_USER.color,
        inactiveColor = Color(0xFFCCCCCC),
        active = active
    )

    fun adminUser(username: String = "{{username}}", active: Boolean = true) = PermissionToggleState(
        username = username,
        role = UserPermission.MODERATOR.label,
        rolenum = UserPermission.MODERATOR.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.MODERATOR.color,
        inactiveColor = Color(0xFFCCCCCC),
        active = active
    )

    fun memberUser(username: String = "{{username}}", active: Boolean = false) = PermissionToggleState(
        username = username,
        role = UserPermission.USER.label,
        rolenum = UserPermission.USER.code,
        roleColor = Color(0xFF1A1A1A),
        activeColor = UserPermission.USER.color,
        inactiveColor = Color(0xFFC2C2C2),
        active = active
    )

    fun hydratePermissions(): List<PermissionToggleState> {
        return listOf(
            superUser(),
            adminUser(),
            memberUser()
        )
    }

    suspend fun getPermissions(): List<PermissionToggleState>? {
        val perms: List<ProjectPermsDTO>? = projectId?.let {
            getProjectpermsUseCase(it)
        }
        val users = getUsersUseCase() ?: return null
        val permissionStates = users.map { user ->
            when {
                user.permission >= UserPermission.SUPER_USER.code -> superUser(
                    username = user.username,
                    active = true
                )

                user.permission >= UserPermission.MODERATOR.code -> adminUser(
                    username = user.username,
                    active = perms?.any { it.user_id == user.idx } ?: true
                )

                else -> memberUser(
                    username = user.username,
                    active = perms?.any { it.user_id == user.idx } ?: false
                )
            }
        }
        return permissionStates
    }

    private fun initPermissions() {
        updateUi { copy(permissions = hydratePermissions()) }
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
                }
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
            updateProjectPermUseCase(projectId, users)
        }
    }


}
