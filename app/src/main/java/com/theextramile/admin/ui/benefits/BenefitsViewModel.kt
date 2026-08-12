package com.theextramile.admin.ui.benefits

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Benefit
import com.theextramile.admin.data.model.BenefitBooking
import com.theextramile.admin.data.model.BenefitMessage
import com.theextramile.admin.data.model.MAX_BENEFITS_IN_EMAIL
import com.theextramile.admin.data.model.MAX_BENEFIT_IMAGES
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.BenefitRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UploadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Beneficios — port de admin-js/benefits.js.
 *
 * La sección tiene tres pestañas, igual que el panel: el catálogo, las
 * reservas de beneficios que ya hicieron los clientes y los mensajes que
 * salen en el correo del tiquete.
 */
class BenefitsViewModel(
    private val repository: BenefitRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    enum class Tab(val title: String) {
        BOOKINGS("Reservados"),
        CATALOG("Catálogo"),
        MESSAGES("Mensajes")
    }

    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tab = MutableStateFlow(Tab.BOOKINGS)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _editingBenefit = MutableStateFlow<Benefit?>(null)
    val editingBenefit: StateFlow<Benefit?> = _editingBenefit.asStateFlow()

    private val _editingMessage = MutableStateFlow<BenefitMessage?>(null)
    val editingMessage: StateFlow<BenefitMessage?> = _editingMessage.asStateFlow()

    private val _editingBooking = MutableStateFlow<BenefitBooking?>(null)
    val editingBooking: StateFlow<BenefitBooking?> = _editingBooking.asStateFlow()

    val benefits: StateFlow<List<Benefit>> = repository.benefits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<BenefitMessage>> = repository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BenefitBooking>> =
        combine(repository.bookings, _query) { list, q ->
            val needle = q.trim().lowercase()
            if (needle.isBlank()) list
            else list.filter {
                it.name.lowercase().contains(needle) ||
                    it.email.lowercase().contains(needle) ||
                    it.phone.contains(needle) ||
                    it.reservationId.lowercase().contains(needle) ||
                    it.displayBenefit.lowercase().contains(needle)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Cuántos beneficios activos salen en los correos (el tope son 4) */
    val inEmailCount: StateFlow<Int> = repository.benefits
        .map { list -> list.count { it.active && it.inEmail } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadAll(initial = true)
    }

    fun selectTab(t: Tab) { _tab.value = t }
    fun onQueryChange(v: String) { _query.value = v }
    fun refresh() = loadAll(initial = false)

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    // ═══════ Catálogo ═══════

    fun startNewBenefit() { _editingBenefit.value = Benefit() }
    fun startEditBenefit(b: Benefit) { _editingBenefit.value = b }
    fun cancelBenefitEdit() { _editingBenefit.value = null }

    fun updateBenefitDraft(transform: (Benefit) -> Benefit) {
        _editingBenefit.value = _editingBenefit.value?.let(transform)
    }

    fun addBenefitImage(uri: Uri) {
        val draft = _editingBenefit.value ?: return
        if (draft.images.size >= MAX_BENEFIT_IMAGES) {
            _uiState.value = _uiState.value.copy(
                error = "Un beneficio admite $MAX_BENEFIT_IMAGES imágenes como mucho"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            // Las imágenes de beneficios van a la carpeta de planes, igual que en la web
            when (val r = uploadRepository.upload(uri, UploadType.TOURS)) {
                is ApiResult.Success -> {
                    updateBenefitDraft { it.copy(images = it.images + r.data) }
                    _uiState.value = _uiState.value.copy(isUploading = false)
                }
                else -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = r.errorMessage ?: "No se pudo subir la imagen"
                )
            }
        }
    }

    fun removeBenefitImage(url: String) {
        updateBenefitDraft { it.copy(images = it.images.filterNot { u -> u == url }) }
    }

    fun saveBenefit() {
        val draft = _editingBenefit.value ?: return
        if (draft.title.isBlank() && draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El beneficio necesita un título")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = repository.upsertBenefit(draft)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Beneficios guardados" else null
            )
            if (r is ApiResult.Success) _editingBenefit.value = null
        }
    }

    fun deleteBenefit(b: Benefit) = runAction("Beneficio eliminado") { repository.deleteBenefit(b.id) }

    fun toggleBenefitActive(b: Benefit) = runAction(null) { repository.toggleActive(b.id) }

    /**
     * Marcar/desmarcar "sale en los correos". El backend no lo impide, pero el
     * correo solo pinta 4, así que aquí se avisa antes de dejar un quinto.
     */
    fun toggleInEmail(b: Benefit) {
        if (!b.inEmail && inEmailCount.value >= MAX_BENEFITS_IN_EMAIL) {
            _uiState.value = _uiState.value.copy(
                error = "En los correos solo caben $MAX_BENEFITS_IN_EMAIL beneficios. Quita otro primero."
            )
            return
        }
        runAction(null) {
            repository.saveBenefits(
                benefits.value.map { if (it.id == b.id) it.copy(inEmail = !it.inEmail) else it }
            )
        }
    }

    // ═══════ Mensajes del correo ═══════

    fun startNewMessage() { _editingMessage.value = BenefitMessage() }
    fun startEditMessage(m: BenefitMessage) { _editingMessage.value = m }
    fun cancelMessageEdit() { _editingMessage.value = null }

    fun updateMessageDraft(transform: (BenefitMessage) -> BenefitMessage) {
        _editingMessage.value = _editingMessage.value?.let(transform)
    }

    fun saveMessage() {
        val draft = _editingMessage.value ?: return
        if (draft.text.isBlank()) {
            // El backend descarta los mensajes vacíos sin avisar
            _uiState.value = _uiState.value.copy(error = "Escribe el texto del mensaje")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = repository.upsertMessage(draft)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Mensaje guardado" else null
            )
            if (r is ApiResult.Success) _editingMessage.value = null
        }
    }

    fun deleteMessage(m: BenefitMessage) =
        runAction("Mensaje eliminado") { repository.deleteMessage(m.id) }

    // ═══════ Reservas de beneficios ═══════

    fun startEditBooking(b: BenefitBooking) { _editingBooking.value = b }
    fun cancelBookingEdit() { _editingBooking.value = null }

    fun updateBookingDraft(transform: (BenefitBooking) -> BenefitBooking) {
        _editingBooking.value = _editingBooking.value?.let(transform)
    }

    /**
     * El backend solo acepta fecha o pasajeros si el beneficio los pide
     * (askDate / askPax), así que se mandan solo esos.
     */
    fun saveBooking() {
        val draft = _editingBooking.value ?: return
        val benefit = benefits.value.firstOrNull { it.id == draft.benefitId }
        val pideFecha = benefit?.askDate ?: draft.date.isNotBlank()
        val pidePax = benefit?.askPax ?: (draft.pax > 0)

        if (pideFecha && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(draft.date)) {
            _uiState.value = _uiState.value.copy(error = "Elige la fecha en formato AAAA-MM-DD")
            return
        }
        if (pidePax && draft.pax < 1) {
            _uiState.value = _uiState.value.copy(error = "Indica cuántas personas van")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = repository.updateBooking(
                id = draft.id,
                date = if (pideFecha) draft.date else null,
                pax = if (pidePax) draft.pax else null,
                notes = draft.notes
            )
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Reserva actualizada" else null
            )
            if (r is ApiResult.Success) _editingBooking.value = null
        }
    }

    fun deleteBooking(b: BenefitBooking) =
        runAction("Reserva eliminada") { repository.deleteBooking(b.id) }

    // ═══════ Interno ═══════

    private fun runAction(successMessage: String?, block: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = block()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) successMessage else null
            )
        }
    }

    private fun loadAll(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial, isRefreshing = !initial, error = null
            )
            val bookings = repository.refreshBookings()
            repository.refreshBenefits()
            repository.refreshMessages()
            _uiState.value = _uiState.value.copy(
                isLoading = false, isRefreshing = false, error = bookings.errorMessage
            )
        }
    }
}
