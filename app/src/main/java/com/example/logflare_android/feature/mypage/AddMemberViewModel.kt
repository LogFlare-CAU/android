package com.example.logflare_android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.enums.UserPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddMemberUiState(
    val username: String = "",
    val selectedPermission: UserPermission = UserPermission.USER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AddMemberViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(AddMemberUiState())
    val ui: StateFlow<AddMemberUiState> = _ui

    fun updateUsername(username: String) {
        _ui.update {
            it.copy(
                username = username,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun selectPermission(permission: UserPermission) {
        _ui.update {
            it.copy(
                selectedPermission = permission,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun addMember(onSuccess: () -> Unit) {
        val currentUsername = _ui.value.username.trim()
        if (currentUsername.isBlank()) {
            _ui.update { it.copy(errorMessage = "Username cannot be empty") }
            return
        }

        if (currentUsername.length < 3) {
            _ui.update { it.copy(errorMessage = "Username must be at least 3 characters") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            
            try {
                // TODO: Implement actual API call to add member
                // For now, simulate a successful add
                kotlinx.coroutines.delay(1000)
                
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        successMessage = "Member added successfully"
                    ) 
                }
                
                kotlinx.coroutines.delay(500)
                onSuccess()
            } catch (e: Exception) {
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to add member: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _ui.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
