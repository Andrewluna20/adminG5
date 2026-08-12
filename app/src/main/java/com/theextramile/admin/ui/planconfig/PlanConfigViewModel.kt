package com.theextramile.admin.ui.planconfig

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Discount
import com.theextramile.admin.data.model.Muelle
import com.theextramile.admin.data.model.Seller
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.PlanConfigRepository
import com.theextramile.admin.data.repository.TourRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UploadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Configuración de planes — port de admin-html/plan-config.html
 * + muelles.js + discounts.js + operador.js (parte de vendedores).
 *
 * Tres pestañas sobre tres archivos JSON distintos del servidor.
 */
class PlanConfigViewModel(
    private val repository: PlanConfigRepository,
    private val tourRepository: TourRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    enum class Tab(val title: String) {
        DISCOUNTS("Descuentos"),
        MUELLES("Muelles"),
        SELLERS("Vendedores")
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

    private val _tab = MutableStateFlow(Tab.DISCOUNTS)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _editingDiscount = MutableStateFlow<Discount?>(null)
    val editingDiscount: StateFlow<Discount?> = _editingDiscount.asStateFlow()

    private val _editingMuelle = MutableStateFlow<Muelle?>(null)
    val editingMuelle: StateFlow<Muelle?> = _editingMuelle.asStateFlow()

    private val _editingSeller = MutableStateFlow<Seller?>(null)
    val editingSeller: StateFlow<Seller?> = _editingSeller.asStateFlow()

    val discounts: StateFlow<List<Discount>> = repository.discounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val muelles: StateFlow<List<Muelle>> = repository.muelles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sellers: StateFlow<List<Seller>> = repository.sellers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sellerBase: StateFlow<String> = repository.sellerBase
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** Para elegir a qué planes se aplica un descuento */
    val tours: StateFlow<List<Tour>> = tourRepository.tours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAll(initial = true)
    }

    fun selectTab(t: Tab) { _tab.value = t }
    fun refresh() = loadAll(initial = false)

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    // ═══════ Descuentos ═══════

    fun startNewDiscount() { _editingDiscount.value = Discount() }
    fun startEditDiscount(d: Discount) { _editingDiscount.value = d }
    fun cancelDiscountEdit() { _editingDiscount.value = null }

    fun updateDiscountDraft(transform: (Discount) -> Discount) {
        _editingDiscount.value = _editingDiscount.value?.let(transform)
    }

    fun saveDiscount() {
        val draft = _editingDiscount.value ?: return
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El descuento necesita un nombre")
            return
        }
        if (draft.value <= 0) {
            _uiState.value = _uiState.value.copy(error = "El descuento tiene que ser mayor que cero")
            return
        }
        saveAndClose("Descuentos guardados", _editingDiscount) { repository.upsertDiscount(draft) }
    }

    fun deleteDiscount(d: Discount) =
        runAction("Descuento eliminado") { repository.deleteDiscount(d.id) }

    fun toggleDiscount(d: Discount) = runAction(null) { repository.toggleDiscount(d.id) }

    // ═══════ Muelles ═══════

    fun startNewMuelle() { _editingMuelle.value = Muelle() }
    fun startEditMuelle(m: Muelle) { _editingMuelle.value = m }
    fun cancelMuelleEdit() { _editingMuelle.value = null }

    fun updateMuelleDraft(transform: (Muelle) -> Muelle) {
        _editingMuelle.value = _editingMuelle.value?.let(transform)
    }

    fun uploadMuelleImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            // Las fotos de muelles van a la carpeta de planes, igual que en la web
            when (val r = uploadRepository.upload(uri, UploadType.TOURS)) {
                is ApiResult.Success -> {
                    updateMuelleDraft { it.copy(image = r.data) }
                    _uiState.value = _uiState.value.copy(isUploading = false)
                }
                else -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = r.errorMessage ?: "No se pudo subir la imagen"
                )
            }
        }
    }

    fun saveMuelle() {
        val draft = _editingMuelle.value ?: return
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El muelle necesita un nombre")
            return
        }
        saveAndClose("Muelle guardado", _editingMuelle) { repository.upsertMuelle(draft) }
    }

    fun deleteMuelle(m: Muelle) = runAction("Muelle eliminado") { repository.deleteMuelle(m.id) }

    // ═══════ Vendedores ═══════

    fun startNewSeller() { _editingSeller.value = Seller() }
    fun startEditSeller(s: Seller) { _editingSeller.value = s }
    fun cancelSellerEdit() { _editingSeller.value = null }

    fun updateSellerDraft(transform: (Seller) -> Seller) {
        _editingSeller.value = _editingSeller.value?.let(transform)
    }

    fun saveSeller() {
        val draft = _editingSeller.value ?: return
        if (draft.name.isBlank()) {
            // El backend descarta sin avisar los vendedores sin nombre
            _uiState.value = _uiState.value.copy(error = "El vendedor necesita un nombre")
            return
        }
        saveAndClose("Vendedor guardado", _editingSeller) { repository.upsertSeller(draft) }
    }

    fun deleteSeller(s: Seller) = runAction("Vendedor eliminado") { repository.deleteSeller(s.id) }

    // ═══════ Interno ═══════

    /**
     * Guarda y cierra el editor SOLO si el servidor aceptó: si falla, la hoja
     * sigue abierta con lo que el usuario escribió para que pueda reintentar.
     */
    private fun <T> saveAndClose(
        successMessage: String,
        editor: MutableStateFlow<T?>,
        block: suspend () -> ApiResult<*>
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        val r = block()
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            error = r.errorMessage,
            message = if (r is ApiResult.Success) successMessage else null
        )
        if (r is ApiResult.Success) editor.value = null
    }

    private fun runAction(successMessage: String?, block: suspend () -> ApiResult<*>) =
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = block()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) successMessage else null
            )
        }

    private fun loadAll(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial, isRefreshing = !initial, error = null
            )
            val d = repository.refreshDiscounts()
            repository.refreshMuelles()
            repository.refreshSellers()
            tourRepository.refresh()
            _uiState.value = _uiState.value.copy(
                isLoading = false, isRefreshing = false, error = d.errorMessage
            )
        }
    }
}
