package com.example.logflare_android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.auth.AuthMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_LOG_LEVEL_LABEL = "Log Level"
private val DEFAULT_LOG_LEVELS = listOf("ERROR", "WARN", "INFO", "DEBUG")

data class MyPageUiState(
    val loading: Boolean = true,
    val username: String? = null,
    val permission: UserPermission = UserPermission.USER,
    val members: List<MyPageMemberUiModel> = emptyList(),
    val selectedLogLevel: String = DEFAULT_LOG_LEVEL_LABEL,
    val logLevels: List<String> = DEFAULT_LOG_LEVELS,
    val errorMessage: String? = null
)

data class MyPageMemberUiModel(
    val username: String,
    val role: UserPermission
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val authMeUseCase: AuthMeUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MyPageUiState())
    val ui: StateFlow<MyPageUiState> = _ui

    init {
        refreshAccountInfo()
    }

    fun refreshAccountInfo() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, errorMessage = null) }
            val user = runCatching { authMeUseCase() }.getOrNull()
            if (user != null) {
                val permission = UserPermission.fromCode(user.permission)
                _ui.update {
                    it.copy(
                        loading = false,
                        username = user.username,
                        permission = permission,
                        members = buildMembers(user.username, permission)
                    )
                }
            } else {
                _ui.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Failed to load account info"
                    )
                }
            }
        }
    }

    fun selectLogLevel(level: String) {
        if (level == _ui.value.selectedLogLevel) return
        _ui.update { it.copy(selectedLogLevel = level) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.clearToken() }
            onComplete()
        }
    }

    private fun buildMembers(username: String, permission: UserPermission): List<MyPageMemberUiModel> {
        if (username.isBlank()) return emptyList()
        val defaultMembers = listOf(
            MyPageMemberUiModel(username = username, role = permission),
            MyPageMemberUiModel(username = "ops-monitor", role = UserPermission.MODERATOR),
            MyPageMemberUiModel(username = "bot-watcher", role = UserPermission.USER)
        )
        return defaultMembers.distinctBy { it.username }
    }
}
