package com.theextramile.admin.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.UpdateDateRequest
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.apiAction
import com.theextramile.admin.data.repository.apiCall
import com.theextramile.admin.util.formatFechaConHora
import com.theextramile.admin.util.isoMediodia
import com.theextramile.admin.util.payParse
import com.theextramile.admin.data.model.Discount
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.model.Seller
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.repository.PlanConfigRepository
import com.theextramile.admin.data.repository.ReservationRepository
import com.theextramile.admin.data.repository.TourRepository
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

/** Los tres modos de cobro del panel web */
enum class PaymentMode(val label: String) {
    PARTIAL("Abono"),
    PAID("Pagada"),
    ARRIVAL("Paga al llegar")
}

data class ReservationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val filter: ReservationFilter = ReservationFilter.PENDING,
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet(),
    val updatingIds: Set<String> = emptySet(),  // IDs en proceso de actualización
    /** id del vendedor por el que se filtra; vacío = todos */
    val sellerFilter: String = "",
    /** groupId cuando se está viendo un solo carrito; vacío = todos */
    val tripFilter: String = "",
    val isCreating: Boolean = false
)

/**
 * Un carrito: los planes que un cliente reservó en el mismo envío.
 *
 * Las reservas sueltas (las que no vienen de un carrito) quedan en un
 * grupo de una sola, igual que groupReservations() en el panel web.
 */
data class ReservationGroup(
    val groupId: String,
    val items: List<Reservation>
) {
    val esCarrito: Boolean get() = groupId.isNotBlank() && items.size > 1
}

class ReservationsViewModel(
    val repository: ReservationRepository,
    private val tourRepository: TourRepository,
    private val planConfigRepository: PlanConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    /** Planes: para el selector de "nueva reserva" y para sus horarios */
    val tours: StateFlow<List<Tour>> = tourRepository.tours

    /** Vendedores: para el "vendido por" y para el filtro */
    val sellers: StateFlow<List<Seller>> = planConfigRepository.sellers

    private val discounts: StateFlow<List<Discount>> = planConfigRepository.discounts

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
        val byText = if (q.isBlank()) byStatus else byStatus.filter {
            it.name.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.tourTitle.lowercase().contains(q) ||
            it.id.lowercase().contains(q)
        }
        val bySeller = if (state.sellerFilter.isBlank()) byText
            else byText.filter { it.soldBy?.id == state.sellerFilter }
        val byTrip = if (state.tripFilter.isBlank()) bySeller
            else bySeller.filter { it.groupId == state.tripFilter }
        // Más recientes primero
        byTrip.sortedByDescending { it.createdAtMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * La misma lista pero agrupada por carrito, que es como la pinta el
     * panel. Se agrupa DESPUÉS de filtrar y conservando el orden de
     * llegada, así que un carrito aparece donde entró su primera reserva.
     */
    val groupedReservations: StateFlow<List<ReservationGroup>> = filteredReservations
        .map { list ->
            val grupos = mutableListOf<MutableList<Reservation>>()
            val posicionDe = mutableMapOf<String, Int>()
            list.forEach { r ->
                val gid = r.groupId.orEmpty()
                val pos = if (gid.isBlank()) null else posicionDe[gid]
                if (pos == null) {
                    if (gid.isNotBlank()) posicionDe[gid] = grupos.size
                    grupos.add(mutableListOf(r))
                } else {
                    grupos[pos].add(r)
                }
            }
            grupos.map { items ->
                ReservationGroup(items.first().groupId.orEmpty(), items.toList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        // Planes, vendedores y descuentos: los necesitan el filtro por
        // vendedor y la hoja de "nueva reserva". Van sin tocar uiState
        // porque que falten no impide gestionar las reservas de siempre.
        viewModelScope.launch { tourRepository.refresh() }
        viewModelScope.launch { planConfigRepository.refreshSellers() }
        viewModelScope.launch { planConfigRepository.refreshDiscounts() }
    }

    fun setSellerFilter(sellerId: String) {
        _uiState.update { it.copy(sellerFilter = sellerId, selectedIds = emptySet()) }
    }

    /** Ver solo los planes que un cliente reservó en el mismo envío */
    fun setTripFilter(groupId: String) {
        _uiState.update { it.copy(tripFilter = groupId, selectedIds = emptySet()) }
    }

    /** Nombre del vendedor a partir del id que guarda la reserva */
    fun sellerName(sellerId: String): String =
        sellers.value.firstOrNull { it.id == sellerId }?.name ?: sellerId

    /**
     * El descuento que le toca a un plan, si tiene alguno configurado.
     *
     * Espejo de discountForTourId() en admin-js/discounts.js:50 — de todos
     * los descuentos activos que incluyen ese plan, gana el que más rebaja.
     *
     * ⚠️ Exige que el plan esté LISTADO en `tourIds`. Un descuento con la
     * lista vacía (que en el sitio público vale para todos) NO se rellena
     * solo aquí, y es a propósito: es lo que hace el panel, y de otro modo
     * la app propondría descuentos que el panel no propone.
     */
    fun discountForTour(tour: Tour): Discount? {
        val unit = payParse(tour.price)
        if (tour.id.isBlank() || unit <= 0) return null
        return discounts.value
            .filter { it.active && it.value > 0 && tour.id in it.tourIds }
            .maxByOrNull { descuentoPorPersona(it, unit) }
            ?.takeIf { descuentoPorPersona(it, unit) > 0 }
    }

    /** Lo que se le quita a CADA persona; nunca deja el precio en negativo */
    fun descuentoPorPersona(discount: Discount?, unitPrice: Int): Int {
        if (discount == null || unitPrice <= 0 || discount.value <= 0) return 0
        val off = if (discount.isPercent) {
            Math.round(unitPrice * minOf(discount.value, 100) / 100.0).toInt()
        } else discount.value
        return minOf(off, unitPrice)
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

    // ═══════════════════════════════════════════
    // PAGO — port de submitPayment() del panel web
    // ═══════════════════════════════════════════

    /**
     * Calcula abono, saldo y estado a partir del modo elegido, con las
     * mismas reglas del panel:
     *  PAGADA        → el abono es el total y no queda saldo
     *  ABONO         → se escribe el abono; el saldo es lo que falta, y si
     *                  no falta nada la reserva queda como pagada
     *  PAGA AL LLEGAR→ no hay abono; se escribe lo que pagará al llegar
     *
     * Tanto el abono como el saldo se recortan al total, porque el panel
     * tampoco deja cobrar más de lo que vale la reserva.
     */
    fun savePayment(id: String, mode: PaymentMode, totalText: String, amountText: String) {
        val total = payParse(totalText)
        val monto = payParse(amountText)

        val deposit: Int
        val balance: Int
        val estado: String
        when (mode) {
            PaymentMode.PAID -> {
                deposit = total; balance = 0; estado = "paid"
            }
            PaymentMode.ARRIVAL -> {
                deposit = 0
                balance = if (total > 0 && monto > total) total else monto
                estado = "arrival"
            }
            PaymentMode.PARTIAL -> {
                deposit = if (total > 0 && monto > total) total else monto
                balance = (total - deposit).coerceAtLeast(0)
                estado = if (balance > 0) "partial" else "paid"
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(updatingIds = it.updatingIds + id) }
            val r = repository.updatePayment(id, total, deposit, balance, estado)
            _uiState.update {
                it.copy(
                    updatingIds = it.updatingIds - id,
                    infoMessage = if (r is ReservationRepository.Result.Success)
                        "Pago guardado · reserva confirmada" else null,
                    errorMessage = when (r) {
                        is ReservationRepository.Result.Error -> r.message
                        ReservationRepository.Result.NoConnection -> "Sin conexión"
                        else -> null
                    }
                )
            }
        }
    }

    /**
     * Cambia la fecha de una reserva. Va por su propia acción del API y no
     * por saveReservations, porque el servidor aprovecha para mover también
     * el evento de Google Calendar.
     */
    fun changeDate(
        reservation: Reservation,
        newDate: java.time.LocalDate,
        /** null = conservar el horario que ya tiene la reserva */
        horaInicio: String? = null,
        horaFin: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingIds = it.updatingIds + reservation.id) }

            val hi = horaInicio ?: reservation.horaInicio.orEmpty()
            val hf = horaFin ?: reservation.horaFin.orEmpty()
            val fechaTexto = formatFechaConHora(newDate, hi)

            val r = apiAction {
                ApiClient.service.updateReservationDate(
                    request = UpdateDateRequest(
                        id = reservation.id,
                        date = fechaTexto,
                        // Mediodía local: la hora real va en horaInicio/horaFin
                        dateRaw = isoMediodia(newDate),
                        horaInicio = hi,
                        horaFin = hf
                    )
                )
            }
            if (r is ApiResult.Success) repository.refresh()
            _uiState.update {
                it.copy(
                    updatingIds = it.updatingIds - reservation.id,
                    infoMessage = if (r is ApiResult.Success)
                        "Fecha cambiada al $fechaTexto" else null,
                    errorMessage = r.errorMessage
                )
            }
        }
    }

    /**
     * Registra una reserva a mano: la que entró por teléfono, WhatsApp o en
     * punto de venta.
     *
     * Nace **pendiente** a propósito, igual que en el panel: eso dispara el
     * correo de "reserva recibida", y el pago que se registre aquí queda
     * guardado para que al pulsar "Confirmar" salga ya relleno (y sea ese
     * botón el que dispare el correo de "reserva confirmada").
     *
     * `unitPrice` y `discount` quedan estampados en la reserva para que la
     * factura y el saldo usen el precio real de ESTA venta aunque el plan
     * cambie de precio o el descuento se acabe después.
     */
    fun createReservation(
        tour: Tour,
        name: String,
        phone: String,
        email: String,
        pax: Int,
        date: java.time.LocalDate,
        horaInicio: String,
        horaFin: String,
        notes: String,
        sellerId: String,
        total: Int,
        deposit: Int,
        balance: Int,
        paymentStatus: String,
        discountType: String,
        discountValue: Int,
        /** El descuento que venía del plan, para saber si lo tocaron a mano */
        autoDiscount: Discount?,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }

            val unitPrice = payParse(tour.price)
            val off = descuentoPorPersona(
                Discount(type = discountType, value = discountValue), unitPrice
            ).takeIf { discountType.isNotBlank() && discountValue > 0 } ?: 0

            // ¿Sigue siendo el descuento que trajo el plan, o lo cambiaron?
            val esAuto = autoDiscount != null &&
                autoDiscount.type == discountType &&
                autoDiscount.value == discountValue

            val payload = mutableMapOf<String, Any?>(
                "tourId" to tour.id,
                "tourTitle" to tour.title,
                "name" to name,
                "pax" to pax,
                "email" to email,
                "phone" to phone,
                "notes" to notes,
                "date" to formatFechaConHora(date, horaInicio),
                "dateRaw" to isoMediodia(date),
                "horaInicio" to horaInicio,
                "horaFin" to horaFin,
                "status" to "pending",
                "total" to total,
                "deposit" to deposit,
                "balance" to balance,
                "paymentStatus" to paymentStatus,
                // El servidor valida el vendedor contra su propia lista
                "sellerId" to sellerId
            )
            if (unitPrice > 0) payload["unitPrice"] = unitPrice - off
            if (off > 0) {
                payload["discount"] = mapOf(
                    "id" to if (esAuto) autoDiscount!!.id else "",
                    "name" to if (esAuto) autoDiscount!!.name else "Descuento aplicado a mano",
                    "type" to discountType,
                    "value" to discountValue,
                    "off" to off,
                    "unitBefore" to unitPrice
                )
            }

            /*
             * Va por apiCall y no por apiAction porque createReservation no
             * devuelve {"success":bool} a secas: trae también la reserva
             * creada con su id. Un 200 con success=false sigue siendo un
             * fallo, así que se comprueba a mano.
             */
            val r = apiCall { ApiClient.service.createReservation(reservation = payload) }
            val cuerpo = (r as? ApiResult.Success)?.data
            val ok = cuerpo?.success == true
            if (ok) repository.refresh()

            val codigo = cuerpo?.id ?: cuerpo?.reservation?.id
            _uiState.update {
                it.copy(
                    isCreating = false,
                    infoMessage = if (ok) {
                        val ref = if (codigo.isNullOrBlank()) "" else " $codigo"
                        "✓ Reserva$ref creada como pendiente. Pulsa «Confirmar» cuando pague."
                    } else null,
                    errorMessage = if (ok) null else
                        (cuerpo?.error ?: r.errorMessage ?: "No se pudo crear la reserva")
                )
            }
            onDone(ok)
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
