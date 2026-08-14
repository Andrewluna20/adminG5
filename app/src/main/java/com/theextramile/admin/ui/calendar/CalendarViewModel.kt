package com.theextramile.admin.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.UpdateDateRequest
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.ReservationRepository
import com.theextramile.admin.data.repository.apiAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Calendario — port de admin-js/calendar.js.
 *
 * Agrupa las reservas por día y deja mover una reserva de fecha con
 * updateReservationDate, que además mueve el evento de Google Calendar.
 */
class CalendarViewModel(private val repository: ReservationRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    /** Un día del mes con sus reservas */
    data class Day(
        val date: LocalDate?,          // null = hueco antes del día 1
        val reservations: List<Reservation> = emptyList()
    ) {
        val confirmed: Int get() = reservations.count { it.isConfirmed }
        val pending: Int get() = reservations.count { it.isPending }
        val isToday: Boolean get() = date == LocalDate.now()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _selectedDay = MutableStateFlow<LocalDate?>(null)
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()

    /** Las canceladas no se pintan en el calendario, igual que en el panel */
    private val activeReservations = repository.reservations
        .map { list -> list.filterNot { it.isCancelled } }

    val days: StateFlow<List<Day>> = combine(activeReservations, _month) { list, ym ->
        val byDay = list.groupBy { it.dayKey }
        val first = ym.atDay(1)
        // La rejilla empieza en lunes, como el calendario del panel
        val leading = (first.dayOfWeek.value - 1).coerceAtLeast(0)
        val cells = mutableListOf<Day>()
        repeat(leading) { cells.add(Day(null)) }
        (1..ym.lengthOfMonth()).forEach { d ->
            val date = ym.atDay(d)
            cells.add(Day(date, byDay[date.toString()].orEmpty()))
        }
        cells
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedReservations: StateFlow<List<Reservation>> =
        combine(activeReservations, _selectedDay) { list, day ->
            if (day == null) emptyList()
            else list.filter { it.dayKey == day.toString() }
                .sortedBy { it.horaInicio.orEmpty() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reservas del mes que se está viendo, para el contador de la cabecera */
    val monthCount: StateFlow<Int> = days
        .map { cells -> cells.sumOf { it.reservations.size } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        load(initial = true)
    }

    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
    fun goToToday() {
        _month.value = YearMonth.now()
        _selectedDay.value = LocalDate.now()
    }

    fun selectDay(date: LocalDate?) { _selectedDay.value = date }

    fun refresh() = load(initial = false)

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    /**
     * Mueve una reserva a otro día.
     *
     * Manda las dos formas de la fecha que espera el backend: `dateRaw` en ISO
     * (la que manda de verdad) y `date` ya escrita en español, que es la que
     * ve el cliente en el tiquete y en el correo.
     */
    fun moveReservation(reservation: Reservation, newDate: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            /*
             * Arrastrar en el calendario cambia el DÍA y nada más: el horario
             * de la reserva se reenvía tal cual. Si no se mandara, el servidor
             * lo entendería como "sin horario" y recrearía el evento de Google
             * Calendar sin hora. Elegir otro horario se hace desde Reservas.
             */
            val hi = reservation.horaInicio.orEmpty()
            val hf = reservation.horaFin.orEmpty()

            val result = apiAction {
                ApiClient.service.updateReservationDate(
                    request = UpdateDateRequest(
                        id = reservation.id,
                        date = com.theextramile.admin.util.formatFechaConHora(newDate, hi),
                        // Mediodía local; la hora real va en horaInicio/horaFin
                        dateRaw = com.theextramile.admin.util.isoMediodia(newDate),
                        horaInicio = hi,
                        horaFin = hf
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    repository.refresh()
                    _selectedDay.value = newDate
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, message = "Reserva movida al ${formatSpanishDate(newDate)}"
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.errorMessage ?: "No se pudo cambiar la fecha"
                )
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial, isRefreshing = !initial, error = null
            )
            val r = repository.refresh()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = when (r) {
                    is ReservationRepository.Result.Error -> r.message
                    ReservationRepository.Result.NoConnection -> "Sin conexión"
                    else -> null
                }
            )
        }
    }
}

/**
 * "12 de Agosto de 2026" — el formato que usa el sitio público.
 *
 * Delega en util/Fechas.kt para que la app y el panel web escriban la
 * fecha de una reserva exactamente igual; antes salía con el mes en
 * minúscula y no coincidía.
 */
fun formatSpanishDate(date: LocalDate): String =
    com.theextramile.admin.util.formatFechaEspanol(date)

/** "Agosto 2026" para la cabecera del calendario */
fun formatMonthTitle(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO")))
        .replaceFirstChar { it.uppercase() }
