package com.example.logflare_android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare_android.enums.UserPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditMemberUiState(
    val username: String = "",
    val selectedPermission: UserPermission = UserPermission.USER,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val username: String = savedStateHandle.get<String>("username") ?: ""

    private val _ui = MutableStateFlow(EditMemberUiState(username = username))
    val ui: StateFlow<EditMemberUiState> = _ui

    init {
        loadMemberData()
    }

    private fun loadMemberData() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // TODO: Implement actual API call to fetch member data
                // For now, simulate loading with default data
                kotlinx.coroutines.delay(500)
                
                val permission = when (username) {
                    "ops-monitor" -> UserPermission.MODERATOR
                    "bot-watcher" -> UserPermission.USER
                    else -> UserPermission.USER
                }
                
                _ui.update { 
                    it.copy(
                        isLoading = false,
                        selectedPermission = permission
                    ) 
                }
            } catch (e: Exception) {
                _ui.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load member data: ${e.message}"
                    ) 
                }
            }
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

    fun updateMember(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // TODO: Implement actual API call to update member
                // For now, simulate a successful update
                kotlinx.coroutines.delay(1000)
                
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        successMessage = "Member updated successfully"
                    ) 
                }
                
                kotlinx.coroutines.delay(500)
                onSuccess()
            } catch (e: Exception) {
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to update member: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun showDeleteDialog() {
        _ui.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _ui.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteMember(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, showDeleteDialog = false, errorMessage = null) }
            
            try {
                // TODO: Implement actual API call to delete member
                // For now, simulate a successful delete
                kotlinx.coroutines.delay(1000)
                
                onSuccess()
            } catch (e: Exception) {
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Failed to delete member: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _ui.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
