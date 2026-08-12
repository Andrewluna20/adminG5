package com.theextramile.admin.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

enum class ReservationFilter { ALL, PENDING, CONFIRMED, CANCELLED }

data class ReservationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val filter: ReservationFilter = ReservationFilter.PENDING,
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet(),
    val updatingIds: Set<String> = emptySet()  // IDs en proceso de actualización
)

class ReservationsViewModel(
    val repository: ReservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    /** Lista filtrada y buscada en tiempo real */
    val filteredReservations: StateFlow<List<Reservation>> = combine(
        repository.reservations,
        _uiState
    ) { all, state ->
        val byStatus = when (state.filter) {
            ReservationFilter.ALL -> all
            ReservationFilter.PENDING -> all.filter { it.isPending }
            ReservationFilter.CONFIRMED -> all.filter { it.isConfirmed }
            ReservationFilter.CANCELLED -> all.filter { it.isCancelled }
        }
        val q = state.searchQuery.trim().lowercase()
        val filtered = if (q.isBlank()) byStatus else byStatus.filter {
            it.name.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.tourTitle.lowercase().contains(q) ||
            it.id.lowercase().contains(q)
        }
        // Más recientes primero
        filtered.sortedByDescending { it.createdAtMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Contadores por estado */
    val statusCounts: StateFlow<Map<String, Int>> = repository.reservations
        .map { list ->
            mapOf(
                "all" to list.size,
                "pending" to list.count { it.isPending },
                "confirmed" to list.count { it.isConfirmed },
                "cancelled" to list.count { it.isCancelled }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            when (val result = repository.refresh()) {
                is ReservationRepository.Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                ReservationRepository.Result.NoConnection -> {
                    _uiState.update { it.copy(isRefreshing = false, errorMessage = "Sin conexión") }
                }
                is ReservationRepository.Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun setFilter(filter: ReservationFilter) {
        _uiState.update { it.copy(filter = filter, selectedIds = emptySet()) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun confirmReservation(id: String) = updateStatus(id, "confirmed", "✅ Reserva confirmada")
    fun cancelReservation(id: String) = updateStatus(id, "cancelled", "❌ Reserva cancelada")
    fun restoreToPending(id: String) = updateStatus(id, "pending", "↩ Reserva restaurada a pendiente")

    private fun updateStatus(id: String, newStatus: String, successMsg: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingIds = it.updatingIds + id) }
            when (val r = repository.updateStatus(id, newStatus)) {
                is ReservationRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            updatingIds = it.updatingIds - id,
                            infoMessage = successMsg
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            updatingIds = it.updatingIds - id,
                            errorMessage = "No se pudo actualizar. Reintenta."
                        )
                    }
                }
            }
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update {
            val newSel = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = newSel)
        }
    }

    fun selectAll(allIds: List<String>) {
        _uiState.update { it.copy(selectedIds = allIds.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when (repository.deleteReservations(ids)) {
                is ReservationRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedIds = emptySet(),
                            infoMessage = "Eliminadas ${ids.size} reserva(s)"
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(errorMessage = "No se pudieron eliminar") }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
