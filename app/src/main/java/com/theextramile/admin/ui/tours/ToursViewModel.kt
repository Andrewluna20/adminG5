package com.theextramile.admin.ui.tours

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.repository.TourRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ToursUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val updatingIds: Set<String> = emptySet()
)

class ToursViewModel(val repository: TourRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ToursUiState())
    val uiState: StateFlow<ToursUiState> = _uiState.asStateFlow()

    val tours: StateFlow<List<Tour>> = repository.tours

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            when (val r = repository.refresh()) {
                is TourRepository.Result.Success -> _uiState.update { it.copy(isRefreshing = false) }
                TourRepository.Result.NoConnection -> _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = "Sin conexión")
                }
                is TourRepository.Result.Error -> _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = r.message)
                }
            }
        }
    }

    suspend fun saveTour(tour: Tour, isNew: Boolean): Boolean {
        _uiState.update { it.copy(updatingIds = it.updatingIds + tour.id) }
        val ok = when (val r = repository.saveTour(tour, isNew)) {
            is TourRepository.Result.Success -> {
                _uiState.update {
                    it.copy(
                        updatingIds = it.updatingIds - tour.id,
                        infoMessage = if (isNew) "✅ Tour agregado" else "✅ Tour actualizado"
                    )
                }
                true
            }
            else -> {
                _uiState.update {
                    it.copy(
                        updatingIds = it.updatingIds - tour.id,
                        errorMessage = "No se pudo guardar"
                    )
                }
                false
            }
        }
        return ok
    }

    fun toggleActive(tourId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingIds = it.updatingIds + tourId) }
            when (repository.toggleActive(tourId)) {
                is TourRepository.Result.Success -> _uiState.update { it.copy(updatingIds = it.updatingIds - tourId) }
                else -> _uiState.update {
                    it.copy(updatingIds = it.updatingIds - tourId, errorMessage = "No se pudo actualizar")
                }
            }
        }
    }

    fun deleteTour(tourId: String) {
        viewModelScope.launch {
            when (repository.deleteTour(tourId)) {
                is TourRepository.Result.Success -> _uiState.update { it.copy(infoMessage = "✅ Tour eliminado") }
                else -> _uiState.update { it.copy(errorMessage = "No se pudo eliminar") }
            }
        }
    }

    suspend fun uploadImage(file: File): String? {
        return when (val r = repository.uploadImage(file, "tours")) {
            is TourRepository.Result.Success -> r.data
            else -> {
                _uiState.update { it.copy(errorMessage = "No se pudo subir la imagen") }
                null
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
