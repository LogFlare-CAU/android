package com.example.logflare_android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.auth.AuthMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val loading: Boolean = true,
    val username: String? = null,
    val permission: UserPermission = UserPermission.USER,
    val members: List<MyPageMemberUiModel> = emptyList(),
    val selectedLogLevel: LogLevel? = null,
    val logLevels: List<LogLevel> = LogLevel.entries.toList(),
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
                        members = emptyList()
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

    fun selectLogLevel(level: LogLevel) {
        if (level == _ui.value.selectedLogLevel) return
        _ui.update { it.copy(selectedLogLevel = level) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.clearToken() }
            onComplete()
        }
    }

}
