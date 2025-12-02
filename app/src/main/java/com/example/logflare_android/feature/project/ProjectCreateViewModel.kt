package com.example.logflare_android.feature.project

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.ProjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectCreateUiState(
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
    private val authRepo: AuthRepository,
    private val api: LogflareApi
) : ViewModel() {
    private val _ui = MutableStateFlow(ProjectCreateUiState())
    val ui: StateFlow<ProjectCreateUiState> = _ui

    init {
        initPermissions()
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
        val updated = _ui.value.keywords + input
        _ui.value = _ui.value.copy(keywords = updated, keywordInput = "", keywordError = null)
    }

    fun removeKeyword(k: String) {
        _ui.value = _ui.value.copy(keywords = _ui.value.keywords.filter { it != k })
    }

    fun toggleAlertLevel(level: String) {
        val set = _ui.value.alertLevels.toMutableSet()
        if (set.contains(level)) set.remove(level) else set.add(level)
        _ui.value = _ui.value.copy(alertLevels = set)
    }

    fun setAlertLevel(level: String) {
        _ui.value = _ui.value.copy(alertLevels = setOf(level))
    }

    fun saveProject() {
        // create project via repo
        val name = _ui.value.name.trim()
        if (!_ui.value.nameValid) return
        val alreadySaved = _ui.value.saved
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.create(name)
                .onSuccess { token ->
                    val message = if (alreadySaved) "Project name updated successfully" else "Project created"
                    _ui.value = _ui.value.copy(loading = false, token = token, saved = true, snackbar = message)
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Unknown error")
                }
        }
    }

    fun editProject() {
        // For now, editing will re-create (placeholder)
        saveProject()
    }

    fun clearSnackbar() {
        _ui.value = _ui.value.copy(snackbar = null)
    }

    //================= Permission Toggles =================//
    fun initPermissions(){
        val defaultPermissions = listOf(
            PermissionToggleState(
                username = "{{username}}",
                role = "Super Admin",
                roleColor = Color(0xFF1A1A1A),
                activeColor = Color(0xFF2FA14F),
                inactiveColor = Color(0xFFCCCCCC),
                active = true
            ),
            PermissionToggleState(
                username = "{{username}}",
                role = "Admin",
                roleColor = Color(0xFF1A1A1A),
                activeColor = Color(0xFF2FA14F),
                inactiveColor = Color(0xFFCCCCCC),
                active = true
            ),
            PermissionToggleState(
                username = "{{username}}",
                role = "Member",
                roleColor = Color(0xFF1A1A1A),
                activeColor = Color(0xFF616161),
                inactiveColor = Color(0xFFC2C2C2),
                active = false
            )
        )
        _ui.value = _ui.value.copy(permissions = defaultPermissions)
    }

    fun onPermissionToggle(index: Int, checked: Boolean) {

    }
}
