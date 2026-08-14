package com.theextramile.admin.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.Faq
import com.theextramile.admin.data.model.InfoBlock
import com.theextramile.admin.data.model.PlanTag
import com.theextramile.admin.data.model.ScheduleNote
import com.theextramile.admin.data.model.SiteSettings
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.SettingsRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UploadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ajustes — port de admin-html/settings.html y sus sub-secciones.
 *
 * ⚠️ Todo vive en un único settings.json y saveSettings reemplaza el objeto
 * entero. Por eso hay UN solo borrador para toda la pantalla: se carga el
 * settings completo del servidor, se edita en memoria y se guarda completo.
 * Guardar sub-sección por sub-sección borraría las demás.
 *
 * Los secretos (Bold, reCAPTCHA) van por su propia acción y el servidor nunca
 * los devuelve, así que su campo siempre aparece vacío: escribir algo lo
 * cambia, dejarlo vacío no lo toca.
 */
class SettingsViewModel(
    val repository: SettingsRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    /** Las sub-secciones del menú de Ajustes, en el orden del panel */
    enum class SubSection(val title: String, val description: String) {
        BRAND("Marca", "Nombre, logos, color y fuente del sitio"),
        HERO("Portada", "Título, fondo, imagen o vídeo de la portada"),
        TOURS_TEXT("Textos de planes", "Encabezados de la sección de planes"),
        COLORS("Colores", "Los 9 colores del sitio público"),
        COMPANY("Empresa", "Datos que salen en la factura"),
        WHATSAPP("WhatsApp", "Número y plantillas de mensaje"),
        OPERADOR("Operador", "Mensaje que le llega al operador"),
        GCAL("Google Calendar", "Método y plantillas del evento"),
        BOLD("Pasarela Bold", "Cobros con tarjeta"),
        RECAPTCHA("reCAPTCHA", "Anti-spam del formulario"),
        FAVICON("Favicon", "Icono de la pestaña del navegador"),
        POLICY("Política del tiquete", "Texto de cancelaciones y reembolsos"),
        INTEGRATIONS("Integraciones", "Llave de Google Maps y webhook antiguo"),
        BANKS("FAQ, info y etiquetas", "Bloques reutilizables que marca cada plan")
    }

    data class UiState(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false,
        val isSendingTest: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _draft = MutableStateFlow(SiteSettings())
    val draft: StateFlow<SiteSettings> = _draft.asStateFlow()

    private val _section = MutableStateFlow<SubSection?>(null)
    val section: StateFlow<SubSection?> = _section.asStateFlow()

    /** Secretos que el usuario acaba de escribir (vacío = no tocar) */
    private val _boldSecret = MutableStateFlow("")
    val boldSecret: StateFlow<String> = _boldSecret.asStateFlow()

    private val _recaptchaSecret = MutableStateFlow("")
    val recaptchaSecret: StateFlow<String> = _recaptchaSecret.asStateFlow()

    private var original = SiteSettings()

    val hasChanges: Boolean get() = _draft.value != original

    /** Lo que ya está guardado en el servidor, para las pantallas que lo leen */
    val settings: StateFlow<SiteSettings> = repository.settings

    init {
        refresh()
    }

    fun openSection(s: SubSection?) { _section.value = s }

    fun update(transform: (SiteSettings) -> SiteSettings) {
        _draft.value = transform(_draft.value)
    }

    fun onBoldSecretChange(v: String) { _boldSecret.value = v }
    fun onRecaptchaSecretChange(v: String) { _recaptchaSecret.value = v }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val r = repository.refresh()) {
                is SettingsRepository.Result.Success -> {
                    original = r.data
                    _draft.value = r.data
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsRepository.Result.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = r.message)
                SettingsRepository.Result.NoConnection ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Sin conexión")
            }
        }
    }

    /** Guarda el settings completo y, si se escribieron, los dos secretos */
    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            when (val r = repository.save(_draft.value)) {
                is SettingsRepository.Result.Success -> Unit
                is SettingsRepository.Result.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = r.message)
                    return@launch
                }
                SettingsRepository.Result.NoConnection -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = "Sin conexión")
                    return@launch
                }
            }

            // Los secretos solo se mandan si el usuario escribió algo
            val secretErrors = mutableListOf<String>()
            if (_boldSecret.value.isNotBlank()) {
                val r = repository.saveBoldSecret(_boldSecret.value.trim())
                if (r is ApiResult.Success) _boldSecret.value = "" else secretErrors.add("llave de Bold")
            }
            if (_recaptchaSecret.value.isNotBlank()) {
                val r = repository.saveRecaptchaSecret(_recaptchaSecret.value.trim())
                if (r is ApiResult.Success) _recaptchaSecret.value = ""
                else secretErrors.add("llave de reCAPTCHA")
            }

            original = _draft.value
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = if (secretErrors.isEmpty()) null
                else "Se guardó la configuración, pero falló la " + secretErrors.joinToString(" y la "),
                message = if (secretErrors.isEmpty()) "Configuración guardada" else null
            )
        }
    }

    fun discardChanges() { _draft.value = original }

    // ═══════ Subidas ═══════

    fun uploadImage(uri: Uri, target: ImageTarget) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            when (val r = uploadRepository.upload(uri, target.uploadType)) {
                is ApiResult.Success -> {
                    update { target.apply(it, r.data) }
                    _uiState.value = _uiState.value.copy(isUploading = false, message = "Archivo subido")
                }
                else -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = r.errorMessage ?: "No se pudo subir el archivo"
                )
            }
        }
    }

    /** A dónde va cada archivo que se sube desde Ajustes */
    enum class ImageTarget(val uploadType: String) {
        SITE_LOGO(UploadType.BRAND),
        SITE_LOGO_HERO(UploadType.BRAND),
        COMPANY_LOGO(UploadType.BRAND),
        FAVICON(UploadType.FAVICON),
        HERO_IMAGE(UploadType.HERO),
        HERO_POSTER(UploadType.HERO),
        HERO_VIDEO(UploadType.HERO);

        fun apply(s: SiteSettings, url: String): SiteSettings = when (this) {
            SITE_LOGO -> s.copy(siteLogo = url)
            SITE_LOGO_HERO -> s.copy(siteLogoHero = url)
            COMPANY_LOGO -> s.copy(companyLogo = url)
            FAVICON -> s.copy(faviconUrl = url)
            HERO_IMAGE -> s.copy(heroBgImage = url)
            HERO_POSTER -> s.copy(heroPosterImage = url)
            HERO_VIDEO -> s.copy(heroBgVideo = url)
        }

        /** El selector solo debe ofrecer vídeos cuando toca el MP4 de la portada */
        val mimeFilter: String get() = if (this == HERO_VIDEO) "video/*" else "image/*"
    }

    // ═══════ Correo de prueba ═══════

    fun sendTestEmail(to: String) {
        if (to.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Escribe un correo de destino")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingTest = true, error = null)
            val r = repository.sendTestEmail(to.trim())
            _uiState.value = _uiState.value.copy(
                isSendingTest = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Correo de prueba enviado a $to" else null
            )
        }
    }

    // ═══════ Bancos reutilizables (FAQ, info, horarios, etiquetas) ═══════

    fun addFaq() = update { it.copy(faqs = it.faqs + Faq(id = newId("faq"))) }

    fun updateFaq(id: String, q: String, a: String) = update { s ->
        s.copy(faqs = s.faqs.map { if (it.id == id) it.copy(q = q, a = a) else it })
    }

    fun removeFaq(id: String) = update { s -> s.copy(faqs = s.faqs.filterNot { it.id == id }) }

    fun addInfo() = update { it.copy(infos = it.infos + InfoBlock(id = newId("info"))) }

    fun updateInfo(id: String, title: String, text: String) = update { s ->
        s.copy(infos = s.infos.map { if (it.id == id) it.copy(title = title, text = text) else it })
    }

    fun removeInfo(id: String) = update { s -> s.copy(infos = s.infos.filterNot { it.id == id }) }

    fun addSchedule() = update { it.copy(schedules = it.schedules + ScheduleNote(id = newId("sch"))) }

    fun updateSchedule(id: String, text: String) = update { s ->
        s.copy(schedules = s.schedules.map { if (it.id == id) it.copy(text = text) else it })
    }

    fun removeSchedule(id: String) = update { s ->
        s.copy(schedules = s.schedules.filterNot { it.id == id })
    }

    fun addTag() = update { it.copy(planTags = it.planTags + PlanTag(id = newId("ptag"))) }

    fun updateTag(id: String, name: String, color: String) = update { s ->
        s.copy(planTags = s.planTags.map { if (it.id == id) it.copy(name = name, color = color) else it })
    }

    fun removeTag(id: String) = update { s ->
        s.copy(planTags = s.planTags.filterNot { it.id == id })
    }

    /** Mismo formato de id que el panel: prefijo + base 36 */
    private fun newId(prefix: String) =
        "$prefix-" + System.currentTimeMillis().toString(36) + (100..999).random()
}
