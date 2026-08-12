package com.theextramile.admin.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.User
import com.theextramile.admin.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsersUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val forbidden: Boolean = false
)

class UsersViewModel(val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    val users: StateFlow<List<User>> = repository.users

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, forbidden = false) }
            when (val r = repository.refresh()) {
                is UserRepository.Result.Success -> _uiState.update { it.copy(isLoading = false) }
                UserRepository.Result.Forbidden -> _uiState.update {
                    it.copy(isLoading = false, forbidden = true)
                }
                UserRepository.Result.NoConnection -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Sin conexión")
                }
                is UserRepository.Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = r.message)
                }
            }
        }
    }

    suspend fun saveUser(
        id: String?, name: String, email: String, password: String?, role: String
    ): Boolean {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val ok = when (val r = repository.saveUser(id, name, email, password, role)) {
            is UserRepository.Result.Success -> {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        infoMessage = if (id == null) "✅ Usuario creado" else "✅ Usuario actualizado"
                    )
                }
                true
            }
            else -> {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "No se pudo guardar")
                }
                false
            }
        }
        return ok
    }

    fun deleteUser(id: String) {
        viewModelScope.launch {
            when (repository.deleteUser(id)) {
                is UserRepository.Result.Success -> _uiState.update {
                    it.copy(infoMessage = "✅ Usuario eliminado")
                }
                else -> _uiState.update {
                    it.copy(errorMessage = "No se pudo eliminar")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
