package com.example.logflare_android.feature.mypage

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.model.UserDTO
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.data.DeviceRepository
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.UserPermission
import com.example.logflare_android.feature.usecase.AuthMeUseCase
import com.example.logflare_android.feature.usecase.GetUsersUseCase
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
    private val getUsersUseCase: GetUsersUseCase,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MyPageUiState())
    val ui: StateFlow<MyPageUiState> = _ui

    private var me: UserDTO? = null


    init {
        viewModelScope.launch {
            getme()
            refreshAccountInfo()
            loadMembers()
            getLogLevelFromStorage()
        }
    }

    fun refresh() {
        _ui.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            getme()
            refreshAccountInfo()
            loadMembers()
        }
        _ui.update { it.copy(loading = false) }
    }

    private suspend fun getme() {
        me = authMeUseCase()
        if (me == null) {
            _ui.update {
                it.copy(
                    loading = false,
                    errorMessage = "Failed to load account info"
                )
            }
        }
    }

    private fun refreshAccountInfo() {
        _ui.update { it.copy(loading = true, errorMessage = null) }
        me?.let { user ->
            val permission = UserPermission.fromCode(user.permission)
            Log.d("MyPageViewModel", "User permission: $permission")
            _ui.update { it ->
                it.copy(
                    loading = false,
                    username = user.username,
                    permission = permission,
                    members = emptyList()
                )
            }
        }
    }

    private suspend fun loadMembers() {
        _ui.update { it.copy(loading = true, errorMessage = null) }
        val users = getUsersUseCase()
        if (users == null) {
            _ui.update {
                it.copy(
                    loading = false,
                    errorMessage = "Failed to load members"
                )
            }
            return
        }
        val memberUiModels = users.map { user ->
            MyPageMemberUiModel(
                username = user.username,
                role = UserPermission.fromCode(user.permission)
            )
        }
        _ui.update {
            it.copy(
                loading = false,
                members = memberUiModels
            )
        }
    }

    private suspend fun getLogLevelFromStorage() {
        val levelStr = deviceRepository.getAlertLevel() ?: return
        selectLogLevel(LogLevel.fromLabel(levelStr))
    }

    fun selectLogLevel(level: LogLevel) {
        if (level == _ui.value.selectedLogLevel) return
        _ui.update { it.copy(selectedLogLevel = level) }
        viewModelScope.launch {
            deviceRepository.setAlertLevel(level.name)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.clearToken() }
            onComplete()
        }
    }

}
