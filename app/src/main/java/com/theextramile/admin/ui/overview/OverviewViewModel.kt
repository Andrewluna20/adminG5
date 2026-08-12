package com.theextramile.admin.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.repository.ReservationRepository
import com.theextramile.admin.util.paymentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Resumen — port de updateStats() (admin-js/reservations.js).
 *
 * El panel muestra 4 contadores y las 5 reservas más recientes. Aquí se añade
 * además el resumen de dinero, que en la web vive en la cabecera de Reservas
 * (updatePaymentSummary): en el celular tiene más sentido verlo de entrada.
 */
class OverviewViewModel(private val repository: ReservationRepository) : ViewModel() {

    data class Stats(
        val total: Int = 0,
        val confirmed: Int = 0,
        val pending: Int = 0,
        val cancelled: Int = 0,
        val vendido: Int = 0,
        val recibido: Int = 0,
        val saldo: Int = 0
    )

    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val stats: StateFlow<Stats> = repository.reservations
        .map { list ->
            // El dinero se cuenta sobre lo vendido: las canceladas no suman
            val vendidas = list.filterNot { it.isCancelled }
            var vendido = 0
            var recibido = 0
            var saldo = 0
            vendidas.forEach { r ->
                val pi = paymentInfo(r)
                vendido += pi.total
                recibido += pi.deposit
                saldo += pi.balance
            }
            Stats(
                total = list.size,
                confirmed = list.count { it.isConfirmed },
                pending = list.count { it.isPending },
                cancelled = list.count { it.isCancelled },
                vendido = vendido,
                recibido = recibido,
                saldo = saldo
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())

    /** Las 5 más recientes, como el panel */
    val recent: StateFlow<List<Reservation>> = repository.reservations
        .map { list -> list.sortedByDescending { it.createdAtMillis }.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        load(initial = true)
    }

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial,
                isRefreshing = !initial,
                error = null
            )
            val result = repository.refresh()
            _uiState.value = UiState(
                isLoading = false,
                isRefreshing = false,
                error = when (result) {
                    is ReservationRepository.Result.Error -> result.message
                    ReservationRepository.Result.NoConnection -> "Sin conexión"
                    else -> null
                }
            )
        }
    }
}
