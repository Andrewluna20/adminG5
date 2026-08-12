package com.theextramile.admin.ui.extracto

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Muelle
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.model.isoToDayKey
import com.theextramile.admin.data.repository.PlanConfigRepository
import com.theextramile.admin.data.repository.ReservationRepository
import com.theextramile.admin.data.repository.TourRepository
import com.theextramile.admin.util.PaymentStatus
import com.theextramile.admin.util.paymentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Extracto — port de admin-js/extracto.js.
 *
 * El panel web exporta un .xlsx armado a mano; en el celular se exporta CSV
 * con las mismas 26 columnas y se comparte con el selector de Android, que es
 * lo que un teléfono sabe hacer bien. Los filtros, el orden y los totales son
 * los mismos.
 */
class ExtractoViewModel(
    private val reservationRepository: ReservationRepository,
    private val tourRepository: TourRepository,
    private val planConfigRepository: PlanConfigRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    /** Espejo de los desplegables de ext-* del panel */
    data class Filters(
        val from: String = "",
        val to: String = "",
        /** "tour" = fecha del plan · "created" = fecha en que se reservó */
        val dateField: String = "tour",
        /** "sold" (todo menos canceladas) · "all" · pending · confirmed · cancelled */
        val status: String = "sold",
        /** "all" · paid · partial · arrival · none */
        val payment: String = "all",
        /** id o título del plan; "" = todos */
        val tour: String = ""
    )

    data class Summary(
        val reservas: Int = 0,
        val pasajeros: Int = 0,
        val total: Int = 0,
        val recibido: Int = 0,
        val saldo: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    val rows: StateFlow<List<Reservation>> = combine(
        reservationRepository.reservations,
        _filters
    ) { list, f -> applyFilters(list, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<Summary> = rows
        .map { list ->
            var pax = 0
            var total = 0
            var dep = 0
            var bal = 0
            list.forEach { r ->
                val pi = paymentInfo(r)
                pax += r.pax
                total += pi.total
                dep += pi.deposit
                bal += pi.balance
            }
            Summary(list.size, pax, total, dep, bal)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Summary())

    /** Planes que aparecen en las reservas, para el desplegable */
    val tourOptions: StateFlow<List<Pair<String, String>>> = reservationRepository.reservations
        .map { list ->
            val seen = LinkedHashMap<String, String>()
            list.forEach { r ->
                val key = tourKey(r)
                if (key.isNotBlank() && !seen.containsKey(key)) {
                    seen[key] = r.tourTitle.ifBlank { key }
                }
            }
            listOf("" to "Todos los planes") +
                seen.entries.sortedBy { it.value.lowercase() }.map { it.key to it.value }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        load(initial = true)
    }

    fun updateFilters(transform: (Filters) -> Filters) {
        _filters.value = transform(_filters.value)
    }

    fun clearFilters() { _filters.value = Filters() }

    fun refresh() = load(initial = false)

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    /**
     * Genera el CSV completo (no solo la vista previa) en la caché y devuelve
     * el archivo para compartirlo.
     */
    fun exportCsv(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val tours = tourRepository.tours.value
                val muelles = planConfigRepository.muelles.value
                val csv = buildCsv(rows.value, tours, muelles)
                val dir = File(context.cacheDir, "extractos").apply { mkdirs() }
                val file = File(dir, "extracto_${System.currentTimeMillis()}.csv")
                // BOM para que Excel abra las tildes bien
                file.writeText("﻿$csv", Charsets.UTF_8)
                onReady(file)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "No se pudo generar el archivo: ${e.message}"
                )
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial, isRefreshing = !initial, error = null
            )
            // Las tres fuentes que usa el extracto del panel
            val res = reservationRepository.refresh()
            tourRepository.refresh()
            planConfigRepository.refreshMuelles()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = when (res) {
                    is ReservationRepository.Result.Error -> res.message
                    ReservationRepository.Result.NoConnection -> "Sin conexión"
                    else -> null
                }
            )
        }
    }

    /** extFiltered() */
    private fun applyFilters(list: List<Reservation>, f: Filters): List<Reservation> {
        val filtered = list.filter { r ->
            if (f.status == "sold") {
                if (r.isCancelled) return@filter false
            } else if (f.status != "all" && r.status != f.status) {
                return@filter false
            }
            if (f.tour.isNotBlank() && tourKey(r) != f.tour) return@filter false
            if (f.payment != "all" && paymentInfo(r).status.key != f.payment) return@filter false
            if (f.from.isNotBlank() || f.to.isNotBlank()) {
                val ymd = dayKeyFor(r, f.dateField)
                if (ymd.isBlank()) return@filter false
                if (f.from.isNotBlank() && ymd < f.from) return@filter false
                if (f.to.isNotBlank() && ymd > f.to) return@filter false
            }
            true
        }
        // Orden cronológico por la fecha elegida en el filtro
        return filtered.sortedBy { dayKeyFor(it, f.dateField) }
    }

    private fun dayKeyFor(r: Reservation, field: String): String =
        if (field == "created") isoToDayKey(r.createdAt) else r.dayKey

    /** extTourKey() */
    private fun tourKey(r: Reservation): String = r.tourId.ifBlank { r.tourTitle }

    /** Las 26 columnas de EXT_COLS, en el mismo orden */
    private fun buildCsv(
        list: List<Reservation>,
        tours: List<Tour>,
        muelles: List<Muelle>
    ): String {
        val header = EXTRACTO_COLUMNS.joinToString(";") { it.header }
        val body = list.joinToString("\n") { r ->
            val tour = tours.firstOrNull { it.id == r.tourId }
            val muelle = tour?.muelleId?.takeIf { it.isNotBlank() }
                ?.let { id -> muelles.firstOrNull { it.id == id } }
            val ctx = ExtractoRowContext(r, paymentInfo(r), tour, muelle)
            EXTRACTO_COLUMNS.joinToString(";") { col -> csvCell(col.value(ctx)) }
        }
        return "$header\n$body"
    }

    /** Punto y coma como separador (es lo que espera Excel en es-CO) */
    private fun csvCell(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(';') || clean.contains('"')) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }
}

/** Datos derivados de una reserva que usan las columnas — extRowCtx() */
data class ExtractoRowContext(
    val r: Reservation,
    val pi: com.theextramile.admin.util.PaymentInfo,
    val tour: Tour?,
    val muelle: Muelle?
)

/**
 * Una columna del extracto: su título y cómo se saca su valor de la reserva.
 *
 * Va como data class y no como Pair porque Kotlin no infiere el tipo del
 * parámetro de un lambda escrito con `to`.
 */
data class ExtractoColumn(
    val header: String,
    val value: (ExtractoRowContext) -> String
)

/** EXT_COLS de extracto.js, con los mismos títulos y el mismo orden */
val EXTRACTO_COLUMNS: List<ExtractoColumn> = listOf(
    ExtractoColumn("Código") { c -> c.r.id },
    ExtractoColumn("Estado") { c -> c.r.statusDisplay },
    ExtractoColumn("Plan") { c -> c.r.tourTitle },
    ExtractoColumn("Categoría") { c -> c.tour?.category.orEmpty() },
    ExtractoColumn("ID del plan") { c -> c.r.tourId },
    ExtractoColumn("Cliente") { c -> c.r.name },
    ExtractoColumn("Correo") { c -> c.r.email.orEmpty() },
    ExtractoColumn("Teléfono") { c -> c.r.phone },
    ExtractoColumn("Pasajeros") { c -> c.r.pax.toString() },
    ExtractoColumn("Fecha del plan") { c -> c.r.date },
    ExtractoColumn("Fecha (AAAA-MM-DD)") { c -> c.r.dayKey },
    ExtractoColumn("Hora inicio") { c -> c.r.horaInicio.orEmpty() },
    ExtractoColumn("Hora fin") { c -> c.r.horaFin.orEmpty() },
    ExtractoColumn("Punto de encuentro") { c -> c.muelle?.name.orEmpty() },
    ExtractoColumn("Total") { c -> c.pi.total.toString() },
    ExtractoColumn("Abonado / Pagado") { c -> c.pi.deposit.toString() },
    ExtractoColumn("Saldo pendiente") { c -> c.pi.balance.toString() },
    ExtractoColumn("Estado de pago") { c ->
        if (c.pi.status == PaymentStatus.NONE) "Sin registro de pago" else c.pi.status.label
    },
    ExtractoColumn("Valor por pasajero") { c ->
        if (c.r.pax > 0) (c.pi.total / c.r.pax).toString() else "0"
    },
    ExtractoColumn("Notas") { c -> c.r.notes.orEmpty() },
    ExtractoColumn("Fecha de la reserva") { c -> formatDateTime(c.r.createdAt) },
    ExtractoColumn("Confirmada el") { c -> formatDateTime(c.r.confirmedAt) },
    ExtractoColumn("Correo \"recibida\"") { c -> yesNo(c.r.receivedEmailSent) },
    ExtractoColumn("Correo confirmación") { c -> yesNo(c.r.confirmationEmailSent) },
    ExtractoColumn("Google Calendar") { c ->
        if (!c.r.gcalEventId.isNullOrBlank()) "Sí · ${c.r.gcalAccount.orEmpty()}" else "No"
    },
    ExtractoColumn("Aceptó políticas") { c ->
        if (c.r.acceptedTerms) "Sí · ${formatDateTime(c.r.acceptedAt)}" else ""
    }
)

private fun yesNo(v: Boolean) = if (v) "Sí" else "No"

/** extDateTime(): "12/08/2026 14:30" */
fun formatDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.Instant.parse(iso)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(iso.take(19))
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e2: Exception) {
            ""
        }
    }
}
