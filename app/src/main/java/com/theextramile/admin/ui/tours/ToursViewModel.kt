package com.theextramile.admin.ui.tours

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.api.TourImage
import com.theextramile.admin.data.model.GcalAccountCalendars
import com.theextramile.admin.data.model.Muelle
import com.theextramile.admin.data.model.SiteSettings
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.repository.GoogleCalendarRepository
import com.theextramile.admin.data.repository.PlanConfigRepository
import com.theextramile.admin.data.repository.SettingsRepository
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

/**
 * Planes.
 *
 * Además de la lista carga lo que el editor necesita para sus selectores:
 * los bancos de FAQ / información / horarios / etiquetas viven en Ajustes,
 * los muelles en Configuración de Planes y los calendarios en Google
 * Calendar. Son tres sitios distintos del panel, pero el editor de un plan
 * los usa todos, así que se piden aquí una vez al abrir la sección.
 *
 * Si alguna de esas cargas falla, el editor simplemente enseña esa lista
 * vacía: no se bloquea la edición del plan por no poder pintar un selector.
 */
class ToursViewModel(
    val repository: TourRepository,
    private val settingsRepository: SettingsRepository,
    private val planConfigRepository: PlanConfigRepository,
    private val gcalRepository: GoogleCalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToursUiState())
    val uiState: StateFlow<ToursUiState> = _uiState.asStateFlow()

    val tours: StateFlow<List<Tour>> = repository.tours

    /** Bancos de FAQ, información, horarios informativos y etiquetas */
    val settings: StateFlow<SiteSettings> = settingsRepository.settings

    /** Puntos de encuentro que se pueden asignar al plan */
    val muelles: StateFlow<List<Muelle>> = planConfigRepository.muelles

    private val _gcalCalendars = MutableStateFlow<List<GcalAccountCalendars>>(emptyList())

    /** Calendarios por cuenta. Listarlos llama a Google, así que se pide una vez. */
    val gcalCalendars: StateFlow<List<GcalAccountCalendars>> = _gcalCalendars.asStateFlow()

    private val _tourImages = MutableStateFlow<List<TourImage>>(emptyList())

    /** Imágenes ya subidas, para reutilizarlas en otro plan */
    val tourImages: StateFlow<List<TourImage>> = _tourImages.asStateFlow()

    private val _loadingImages = MutableStateFlow(false)
    val loadingImages: StateFlow<Boolean> = _loadingImages.asStateFlow()

    init {
        refresh()
        loadEditorOptions()
    }

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

    /**
     * Las tres listas de los selectores. Van en corrutinas separadas para que
     * una lenta (la de Google) no retrase a las otras, y sin tocar uiState:
     * que falten no es un error que haya que enseñarle a nadie.
     */
    private fun loadEditorOptions() {
        viewModelScope.launch { settingsRepository.refresh() }
        viewModelScope.launch { planConfigRepository.refreshMuelles() }
        viewModelScope.launch {
            val r = gcalRepository.listCalendars()
            if (r is GoogleCalendarRepository.Result.Success) _gcalCalendars.value = r.data
        }
    }

    /**
     * Carga la lista de imágenes ya subidas. Se pide cuando se abre el
     * selector, no al entrar en la sección: son muchas y casi nunca hacen
     * falta. Con `force` se vuelve a pedir tras subir una nueva.
     */
    fun loadTourImages(force: Boolean = false) {
        if (!force && (_tourImages.value.isNotEmpty() || _loadingImages.value)) return
        viewModelScope.launch {
            _loadingImages.value = true
            val r = repository.listImages()
            if (r is TourRepository.Result.Success) _tourImages.value = r.data
            _loadingImages.value = false
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

    /**
     * Sube una imagen o un vídeo del plan y devuelve su URL.
     *
     * Es el mismo upload.php que usa el panel web para las dos cosas: acepta
     * jpg, png, webp y mp4, y decide por el contenido real del archivo, no
     * por su nombre. La carpeta "tours" es una de las cinco permitidas.
     */
    suspend fun uploadFile(file: File): String? {
        return when (val r = repository.uploadImage(file, "tours")) {
            is TourRepository.Result.Success -> r.data
            else -> {
                _uiState.update { it.copy(errorMessage = "No se pudo subir el archivo") }
                null
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
