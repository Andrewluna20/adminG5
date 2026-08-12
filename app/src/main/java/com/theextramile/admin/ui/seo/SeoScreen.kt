package com.theextramile.admin.ui.seo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.SiteSettings
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.SettingsRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UploadType
import com.theextramile.admin.ui.blog.absoluteUrl
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SEO — port de admin-html/seo.html + admin-js/seo.js.
 *
 * Todos los campos viven dentro de settings.json, así que se guardan con
 * patch() para no pisar el resto de la configuración.
 */
class SeoViewModel(
    private val settingsRepository: SettingsRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false,
        val isRegenerating: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Copia editable; se compara con la original para saber si hay cambios */
    private val _draft = MutableStateFlow(SeoDraft())
    val draft: StateFlow<SeoDraft> = _draft.asStateFlow()

    private var original = SeoDraft()

    val hasChanges: Boolean get() = _draft.value != original

    init {
        load()
    }

    fun update(transform: (SeoDraft) -> SeoDraft) {
        _draft.value = transform(_draft.value)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun uploadSocialImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            when (val r = uploadRepository.upload(uri, UploadType.BLOG)) {
                is ApiResult.Success -> {
                    update { it.copy(seoImage = r.data) }
                    _uiState.value = _uiState.value.copy(isUploading = false, message = "Imagen subida")
                }
                else -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = r.errorMessage ?: "No se pudo subir la imagen"
                )
            }
        }
    }

    fun save() {
        val d = _draft.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = settingsRepository.patch { s ->
                s.copy(
                    seoTitle = d.seoTitle,
                    seoDescription = d.seoDescription,
                    seoKeywords = d.seoKeywords,
                    seoImage = d.seoImage,
                    seoTwitter = d.seoTwitter,
                    siteUrl = d.siteUrl,
                    seoSitemapEnabled = d.seoSitemapEnabled,
                    smartSearch = (s.smartSearch ?: com.theextramile.admin.data.model.SmartSearch())
                        .copy(
                            enabled = d.smartSearchEnabled,
                            title = d.smartSearchTitle,
                            placeholder = d.smartSearchPlaceholder
                        )
                )
            }
            when (result) {
                is SettingsRepository.Result.Success -> {
                    original = d
                    _uiState.value = _uiState.value.copy(isSaving = false, message = "SEO guardado")
                }
                is SettingsRepository.Result.Error ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = result.message)
                SettingsRepository.Result.NoConnection ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = "Sin conexión")
            }
        }
    }

    /** Reconstruye index.html + sitemap.xml con lo que hay guardado */
    fun regenerate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRegenerating = true, error = null)
            val r = settingsRepository.regenerateIndex()
            _uiState.value = _uiState.value.copy(
                isRegenerating = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Sitio regenerado" else null
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val r = settingsRepository.refresh()) {
                is SettingsRepository.Result.Success -> {
                    val d = SeoDraft.from(r.data)
                    original = d
                    _draft.value = d
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsRepository.Result.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = r.message)
                SettingsRepository.Result.NoConnection ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Sin conexión")
            }
        }
    }
}

/** Solo los campos de settings.json que toca esta pantalla */
data class SeoDraft(
    val seoTitle: String = "",
    val seoDescription: String = "",
    val seoKeywords: String = "",
    val seoImage: String = "",
    val seoTwitter: String = "",
    val siteUrl: String = "",
    val seoSitemapEnabled: Boolean = true,
    val smartSearchEnabled: Boolean = false,
    val smartSearchTitle: String = "",
    val smartSearchPlaceholder: String = ""
) {
    companion object {
        fun from(s: SiteSettings) = SeoDraft(
            seoTitle = s.seoTitle,
            seoDescription = s.seoDescription,
            seoKeywords = s.seoKeywords,
            seoImage = s.seoImage,
            seoTwitter = s.seoTwitter,
            siteUrl = s.siteUrl,
            seoSitemapEnabled = s.seoSitemapEnabled,
            smartSearchEnabled = s.smartSearch?.enabled ?: false,
            smartSearchTitle = s.smartSearch?.title.orEmpty(),
            smartSearchPlaceholder = s.smartSearch?.placeholder.orEmpty()
        )
    }
}

@Composable
fun SeoScreen(
    viewModel: SeoViewModel,
    siteBaseUrl: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.uploadSocialImage(uri) }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(title = "SEO", subtitle = "Cómo se ve tu sitio en Google", onBack = onBack)

            if (uiState.isLoading) {
                SectionPlaceholder("Cargando…", isLoading = true)
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // ── Vista previa del resultado de Google ──
                    GooglePreview(draft)

                    FormSectionTitle("Metadatos")
                    AdminField(
                        "Título", draft.seoTitle,
                        { v -> viewModel.update { it.copy(seoTitle = v) } },
                        hint = "Lo ideal son 50–60 caracteres. Llevas ${draft.seoTitle.length}."
                    )
                    AdminField(
                        "Descripción", draft.seoDescription,
                        { v -> viewModel.update { it.copy(seoDescription = v) } },
                        hint = "Lo ideal son 150–160 caracteres. Llevas ${draft.seoDescription.length}.",
                        singleLine = false, minLines = 3
                    )
                    AdminField(
                        "Palabras clave", draft.seoKeywords,
                        { v -> viewModel.update { it.copy(seoKeywords = v) } },
                        hint = "Sepáralas con comas"
                    )
                    AdminField(
                        "Dominio del sitio", draft.siteUrl,
                        { v -> viewModel.update { it.copy(siteUrl = v) } },
                        placeholder = "kalaoz.com",
                        hint = "Se usa para las URLs del sitemap y de los enlaces sociales"
                    )

                    FormSectionTitle("Imagen para redes")
                    if (draft.seoImage.isNotBlank()) {
                        AsyncImage(
                            model = absoluteUrl(draft.seoImage, siteBaseUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    GradientButton(
                        text = if (draft.seoImage.isBlank()) "Subir imagen" else "Cambiar imagen",
                        onClick = { picker.launch("image/*") },
                        isLoading = uiState.isUploading,
                        icon = Icons.Default.Image,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Es la que sale cuando alguien comparte tu sitio en WhatsApp o Facebook. Lo mejor es 1200×630.",
                        color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    AdminField(
                        "Usuario de Twitter/X", draft.seoTwitter,
                        { v -> viewModel.update { it.copy(seoTwitter = v) } },
                        placeholder = "@tucuenta"
                    )

                    FormSectionTitle("Sitemap")
                    AdminSwitch(
                        "Generar sitemap.xml", draft.seoSitemapEnabled,
                        { v -> viewModel.update { it.copy(seoSitemapEnabled = v) } },
                        hint = "Se actualiza solo cada vez que guardas planes o entradas del blog"
                    )

                    FormSectionTitle("Buscador inteligente")
                    AdminSwitch(
                        "Activado", draft.smartSearchEnabled,
                        { v -> viewModel.update { it.copy(smartSearchEnabled = v) } },
                        hint = "El buscador del sitio público que sugiere planes"
                    )
                    if (draft.smartSearchEnabled) {
                        AdminField(
                            "Título", draft.smartSearchTitle,
                            { v -> viewModel.update { it.copy(smartSearchTitle = v) } }
                        )
                        AdminField(
                            "Texto del campo", draft.smartSearchPlaceholder,
                            { v -> viewModel.update { it.copy(smartSearchPlaceholder = v) } }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Las frases de búsqueda de cada plan se siguen editando desde el panel web: " +
                                "son cientos de líneas por plan y no caben bien en el celular.",
                            color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
                        )
                    }

                    FormSectionTitle("Sitio público")
                    GlassCard {
                        Column {
                            Text(
                                "Regenerar index.html y sitemap.xml",
                                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "El sitio se regenera solo al guardar planes, descuentos o beneficios. " +
                                    "Usa esto si algo se quedó desactualizado.",
                                color = TextDim, fontSize = 11.sp, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            GradientButton(
                                text = "Regenerar ahora",
                                onClick = { viewModel.regenerate() },
                                isLoading = uiState.isRegenerating,
                                gradient = Gradients.GreenCyan,
                                icon = Icons.Default.Refresh,
                                height = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }

                SaveBar(
                    visible = viewModel.hasChanges,
                    isSaving = uiState.isSaving,
                    onSave = { viewModel.save() }
                )
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }
}

/** Vista previa del resultado en Google, igual que la del panel */
@Composable
private fun GooglePreview(draft: SeoDraft) {
    GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column {
            Text(
                "VISTA PREVIA EN GOOGLE",
                color = TextMuted, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                draft.siteUrl.ifBlank { "tusitio.com" },
                color = GreenLight, fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                draft.seoTitle.ifBlank { "Título de tu sitio" },
                color = CyanBright, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                draft.seoDescription.ifBlank { "La descripción que verá la gente en los resultados de búsqueda." },
                color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
                maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
