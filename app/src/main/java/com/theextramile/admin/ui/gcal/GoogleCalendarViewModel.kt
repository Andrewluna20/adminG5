package com.theextramile.admin.ui.gcal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.model.GcalConfigStatus
import com.theextramile.admin.data.model.GoogleCalendarAccount
import com.theextramile.admin.data.repository.GoogleCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GcalUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class GoogleCalendarViewModel(
    val repository: GoogleCalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GcalUiState())
    val uiState: StateFlow<GcalUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<GoogleCalendarAccount>> = repository.accounts
    val activeEmail: StateFlow<String?> = repository.activeEmail
    val configStatus: StateFlow<GcalConfigStatus> = repository.configStatus

    /** URL para abrir el flujo OAuth en el navegador */
    val authUrl: String
        get() = "${BuildConfig.API_BASE_URL}gcal.php?action=start_auth"

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // 1) Estado de configuración
            repository.checkConfig()
            // 2) Lista de cuentas
            when (val r = repository.refresh()) {
                is GoogleCalendarRepository.Result.Success -> _uiState.update {
                    it.copy(isLoading = false)
                }
                GoogleCalendarRepository.Result.NoConnection -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Sin conexión")
                }
                is GoogleCalendarRepository.Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = r.message)
                }
            }
        }
    }

    fun switchAccount(email: String) {
        viewModelScope.launch {
            when (repository.switchAccount(email)) {
                is GoogleCalendarRepository.Result.Success -> _uiState.update {
                    it.copy(infoMessage = "✅ Cuenta activa: $email")
                }
                else -> _uiState.update {
                    it.copy(errorMessage = "No se pudo cambiar la cuenta")
                }
            }
        }
    }

    fun deleteAccount(email: String) {
        viewModelScope.launch {
            when (repository.deleteAccount(email)) {
                is GoogleCalendarRepository.Result.Success -> _uiState.update {
                    it.copy(infoMessage = "✅ Cuenta eliminada")
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
