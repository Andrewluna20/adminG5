package com.theextramile.admin.ui.tours

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.BlockedSlot
import com.theextramile.admin.data.model.Horario
import com.theextramile.admin.data.model.Testimonial
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.data.model.TourTranslation
import com.theextramile.admin.ui.blog.absoluteUrl
import com.theextramile.admin.ui.components.AdminDropdown
import com.theextramile.admin.ui.components.AdminField
import com.theextramile.admin.ui.components.AdminSwitch
import com.theextramile.admin.ui.components.DatePickerField
import com.theextramile.admin.ui.components.FlowRowSimple
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.components.RemovablePill
import com.theextramile.admin.ui.components.TimePickerField
import com.theextramile.admin.ui.components.prettyDate
import com.theextramile.admin.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Las mismas cinco del panel web (admin-html/modals.html) */
private val CATEGORIAS = listOf(
    "Pasadías Familiares", "Beach Club", "Pasadías Relax", "Atardeceres", "Experiencias"
)

/**
 * Editor de un plan — todos los campos del modal del panel web.
 *
 * ⚠️ Se parte SIEMPRE del plan cargado con copy(), nunca de un Tour()
 * nuevo: el sitio guarda campos que este editor no toca (metaDescription,
 * por ejemplo, que se edita en SEO) y construirlo desde cero los dejaría
 * en su valor por defecto, borrándolos al guardar.
 *
 * Los selectores de muelle, calendario y bloques de Ajustes salen de
 * ToursViewModel, que los pide al abrir la sección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourEditSheet(
    tour: Tour?,
    siteBaseUrl: String,
    canEditNet: Boolean,
    viewModel: ToursViewModel,
    onDismiss: () -> Unit
) {
    val isNew = tour == null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()
    val muelles by viewModel.muelles.collectAsState()
    val gcalAccounts by viewModel.gcalCalendars.collectAsState()
    val uploadedImages by viewModel.tourImages.collectAsState()
    val loadingImages by viewModel.loadingImages.collectAsState()

    /** "main" o "gallery" mientras el selector de imágenes ya subidas está abierto */
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    // ── Datos ──
    var title by remember { mutableStateOf(tour?.title ?: "") }
    var shortDesc by remember { mutableStateOf(tour?.shortDesc ?: "") }
    var description by remember { mutableStateOf(tour?.description ?: "") }
    var category by remember { mutableStateOf(tour?.category?.ifBlank { CATEGORIAS[2] } ?: CATEGORIAS[2]) }
    var active by remember { mutableStateOf(tour?.active ?: true) }
    var popular by remember { mutableStateOf(tour?.popular ?: false) }

    // ── Precio ──
    var price by remember { mutableStateOf(tour?.price ?: "") }
    var priceBefore by remember { mutableStateOf(tour?.priceBefore ?: "") }
    var priceLabel by remember { mutableStateOf(tour?.priceLabel ?: "") }
    var priceNet by remember { mutableStateOf(tour?.priceNet ?: "") }

    // ── Imágenes y vídeo ──
    var imageUrl by remember { mutableStateOf(tour?.img ?: "") }
    var gallery by remember { mutableStateOf(tour?.gallery ?: emptyList()) }
    var videoWeb by remember { mutableStateOf(tour?.videoWeb ?: "") }
    var videoMobile by remember { mutableStateOf(tour?.videoMobile ?: "") }

    // ── Incluye ──
    var includes by remember { mutableStateOf(tour?.includes ?: emptyList()) }
    var newInclude by remember { mutableStateOf("") }

    // ── Cupos y horarios ──
    var cupos by remember { mutableStateOf(tour?.cupos ?: "") }
    var showCupos by remember { mutableStateOf(tour?.showCupos ?: false) }
    var horarios by remember { mutableStateOf(tour?.horarios ?: emptyList()) }
    var nuevoInicio by remember { mutableStateOf("") }
    var nuevoFin by remember { mutableStateOf("") }
    var unavailableDates by remember { mutableStateOf(tour?.unavailableDates ?: emptyList()) }
    var blockedSlots by remember { mutableStateOf(tour?.blockedSlots ?: emptyList()) }
    var blockDate by remember { mutableStateOf("") }
    var blockHorario by remember { mutableStateOf("") }

    // ── Bloques de Ajustes ──
    var tagIds by remember { mutableStateOf(tour?.tagIds ?: emptyList()) }
    var faqIds by remember { mutableStateOf(tour?.faqIds ?: emptyList()) }
    var infoIds by remember { mutableStateOf(tour?.infoIds ?: emptyList()) }
    var scheduleIds by remember { mutableStateOf(tour?.scheduleIds ?: emptyList()) }

    // ── Reseñas ──
    var testimonials by remember { mutableStateOf(tour?.testimonials ?: emptyList()) }

    // ── Mensajes ──
    var whatsappMessage by remember { mutableStateOf(tour?.whatsappMessage ?: "") }
    var operatorMessage by remember { mutableStateOf(tour?.operatorMessage ?: "") }

    // ── Encuentro y calendario ──
    var muelleId by remember { mutableStateOf(tour?.muelleId ?: "") }
    var mapEmbed by remember { mutableStateOf(tour?.mapEmbed ?: "") }
    var gcalValue by remember {
        mutableStateOf(
            if (tour?.gcalCalendarId.isNullOrBlank()) ""
            else "${tour?.gcalAccount}||${tour?.gcalCalendarId}"
        )
    }

    // ── Traducción ──
    var enTitle by remember { mutableStateOf(tour?.en?.title ?: "") }
    var enShort by remember { mutableStateOf(tour?.en?.shortDesc ?: "") }
    var enDescription by remember { mutableStateOf(tour?.en?.description ?: "") }
    var enPriceLabel by remember { mutableStateOf(tour?.en?.priceLabel ?: "") }
    var enIncludes by remember { mutableStateOf(tour?.en?.includes?.joinToString("\n") ?: "") }

    /** Qué se está subiendo ahora mismo, para bloquear solo ese botón */
    var uploading by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    /** Reseña a la que se le está eligiendo foto */
    var testimonialImgIndex by remember { mutableStateOf(-1) }

    /** Sube un archivo y devuelve su URL, marcando qué se está subiendo */
    suspend fun upload(uri: Uri, what: String, extension: String): String? {
        uploading = what
        error = null
        val tmp = uriToTempFile(context, uri, extension)
        val url = if (tmp != null) {
            val u = viewModel.uploadFile(tmp)
            tmp.delete()
            if (u == null) error = "No se pudo subir el archivo"
            // Que la recién subida aparezca también en "mis imágenes"
            else viewModel.loadTourImages(force = true)
            u
        } else {
            error = "No se pudo leer el archivo"
            null
        }
        uploading = null
        return url
    }

    val mainImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) scope.launch { upload(uri, "main", ".jpg")?.let { imageUrl = it } } }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            uris.forEach { uri -> upload(uri, "gallery", ".jpg")?.let { gallery = gallery + it } }
        }
    }

    val videoWebPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) scope.launch { upload(uri, "videoWeb", ".mp4")?.let { videoWeb = it } } }

    val videoMobilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) scope.launch { upload(uri, "videoMobile", ".mp4")?.let { videoMobile = it } } }

    val testimonialImgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val idx = testimonialImgIndex
        if (uri != null && idx >= 0) scope.launch {
            upload(uri, "testi", ".jpg")?.let { url ->
                testimonials = testimonials.toMutableList().also { it[idx] = it[idx].copy(img = url) }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassBorderStrong) }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isNew) "Nuevo plan" else "Editar plan",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .clickable(enabled = !isSaving) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // ═══════ DATOS DEL PLAN ═══════
            EditorSection("Datos del plan", Icons.Default.Description, initiallyOpen = true) {
                AdminField("Nombre del plan *", title, { title = it }, enabled = !isSaving)
                AdminField(
                    "Descripción corta", shortDesc, { shortDesc = it },
                    hint = "Resumen que sale en la tarjeta",
                    singleLine = false, minLines = 2, enabled = !isSaving
                )
                AdminField(
                    "Descripción completa", description, { description = it },
                    hint = "Se muestra en la página del plan",
                    placeholder = "Qué vivirá el cliente, recomendaciones, qué llevar…",
                    singleLine = false, minLines = 4, enabled = !isSaving
                )
                AdminDropdown(
                    "Categoría",
                    CATEGORIAS.map { it to it },
                    category,
                    { category = it },
                    enabled = !isSaving
                )
                AdminSwitch(
                    "Plan visible al público", active, { active = it },
                    hint = if (active) "Se muestra en el sitio web" else "Oculto del sitio",
                    enabled = !isSaving
                )
                AdminSwitch(
                    "★ Destinos populares", popular, { popular = it },
                    hint = "Sale en el carrusel de la página de inicio",
                    enabled = !isSaving
                )
            }

            // ═══════ PRECIO ═══════
            EditorSection(
                "Precio", Icons.Default.Sell,
                summary = if (price.isBlank()) "sin precio" else "$$price"
            ) {
                AdminField(
                    "Precio", price, { price = it.filter { c -> c.isDigit() } },
                    hint = "Solo números, sin puntos: 430000 se ve como $430.000",
                    placeholder = "430000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
                AdminField(
                    "Precio anterior", priceBefore, { priceBefore = it.filter { c -> c.isDigit() } },
                    hint = "Opcional. Si lo llenas, sale tachado al lado del precio actual",
                    placeholder = "500000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
                AdminField(
                    "Etiqueta de precio", priceLabel, { priceLabel = it },
                    placeholder = "COP por persona", enabled = !isSaving
                )
                if (canEditNet) {
                    AdminField(
                        "Precio neto (privado)", priceNet, { priceNet = it.filter { c -> c.isDigit() } },
                        hint = "Lo que te cuesta el plan. Nunca sale en la web, ni en el correo, ni en la factura",
                        placeholder = "350000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isSaving
                    )
                }
            }

            // ═══════ IMAGEN Y GALERÍA ═══════
            EditorSection(
                "Imagen y galería", Icons.Default.Image,
                summary = if (gallery.isEmpty()) null else "${gallery.size} en galería"
            ) {
                Text("IMAGEN PRINCIPAL", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassWhite2)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = uploading == null) { mainImagePicker.launch("image/*") }
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = absoluteUrl(imageUrl, siteBaseUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (uploading == "main") {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = TextOnAccent)
                                Spacer(Modifier.height(8.dp))
                                Text("Subiendo…", color = TextOnAccent, fontSize = 12.sp)
                            }
                        }
                    } else if (imageUrl.isBlank()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null,
                                tint = TextMuted, modifier = Modifier.size(38.dp))
                            Text("Toca para subir imagen", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            modifier = Modifier.padding(10.dp).align(Alignment.BottomEnd),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.7f),
                            contentColor = TextOnAccent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cambiar", fontSize = 11.sp)
                            }
                        }
                    }
                }

                PickFromUploadsButton {
                    pickerTarget = "main"
                    viewModel.loadTourImages()
                }

                Spacer(Modifier.height(14.dp))
                Text("GALERÍA", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text(
                    "Varias fotos: se ven al pulsar “Ver más” en la web",
                    color = TextDim, fontSize = 11.sp
                )
                ImageStrip(
                    urls = gallery,
                    siteBaseUrl = siteBaseUrl,
                    onAdd = { galleryPicker.launch("image/*") },
                    onRemove = { i -> gallery = gallery.toMutableList().also { it.removeAt(i) } },
                    isBusy = uploading == "gallery"
                )
                PickFromUploadsButton {
                    pickerTarget = "gallery"
                    viewModel.loadTourImages()
                }
            }

            // ═══════ VÍDEOS ═══════
            EditorSection("Vídeos del plan", Icons.Default.Videocam) {
                Text(
                    "Se muestran arriba en la página del plan. Lo más rápido es pegar " +
                        "el enlace de YouTube; para que no salga ninguna marca, sube el MP4.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                VideoSlot(
                    label = "Web · horizontal",
                    url = videoWeb,
                    onUrlChange = { videoWeb = it },
                    onPick = { videoWebPicker.launch("video/*") },
                    isBusy = uploading == "videoWeb",
                    enabled = !isSaving
                )
                VideoSlot(
                    label = "Móvil · reel vertical",
                    url = videoMobile,
                    onUrlChange = { videoMobile = it },
                    onPick = { videoMobilePicker.launch("video/*") },
                    isBusy = uploading == "videoMobile",
                    enabled = !isSaving
                )
            }

            // ═══════ INCLUYE ═══════
            EditorSection(
                "Incluye", Icons.Default.Checklist,
                summary = if (includes.isEmpty()) null else "${includes.size}"
            ) {
                includes.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassWhite2)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Purple))
                        Spacer(Modifier.width(10.dp))
                        Text(item, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { includes = includes.toMutableList().also { it.removeAt(idx) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Eliminar", tint = TextMuted,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        AdminField("", newInclude, { newInclude = it },
                            placeholder = "Ej: Transporte incluido", enabled = !isSaving)
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Purple)
                            .clickable {
                                if (newInclude.isNotBlank()) {
                                    includes = includes + newInclude.trim()
                                    newInclude = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Agregar", tint = TextOnAccent)
                    }
                }
            }

            // ═══════ CUPOS Y HORARIOS ═══════
            EditorSection(
                "Cupos y horarios", Icons.Default.EventAvailable,
                summary = if (horarios.isEmpty()) "sin horarios" else "${horarios.size} horarios"
            ) {
                AdminField(
                    "Cantidad de cupos", cupos, { cupos = it.filter { c -> c.isDigit() } },
                    placeholder = "20",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
                AdminSwitch(
                    "Mostrar los cupos en la web", showCupos, { showCupos = it },
                    enabled = !isSaving
                )

                Spacer(Modifier.height(10.dp))
                Text("HORARIOS DEL PLAN", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text(
                    "Al cliente solo se le muestra la hora de inicio",
                    color = TextDim, fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerField("Inicio", nuevoInicio, { nuevoInicio = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    TimePickerField("Fin", nuevoFin, { nuevoFin = it }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Purple)
                            .clickable {
                                if (nuevoInicio.isNotBlank()) {
                                    val h = Horario(nuevoInicio, nuevoFin)
                                    if (horarios.none { it.inicio == h.inicio && it.fin == h.fin }) {
                                        horarios = horarios + h
                                    }
                                    nuevoInicio = ""; nuevoFin = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, "Agregar horario", tint = TextOnAccent) }
                }
                FlowRowSimple(Modifier.padding(top = 10.dp)) {
                    horarios.forEach { h ->
                        RemovablePill(h.label, {
                            horarios = horarios - h
                            // Un bloqueo apunta a un horario; si el horario se va,
                            // su bloqueo deja de tener sentido (y el web lo descarta
                            // igual al guardar).
                            blockedSlots = blockedSlots.filterNot {
                                it.inicio == h.inicio && it.fin == h.fin
                            }
                        })
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("FECHAS NO DISPONIBLES", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text("El día entero se bloquea en el calendario de reservas",
                    color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                DatePickerField("Añadir una fecha", "", { date ->
                    if (date !in unavailableDates) unavailableDates = (unavailableDates + date).sorted()
                }, enabled = !isSaving)
                FlowRowSimple(Modifier.padding(top = 10.dp)) {
                    unavailableDates.forEach { d ->
                        RemovablePill(prettyDate(d), { unavailableDates = unavailableDates - d },
                            tone = OrangeRed)
                    }
                }

                if (horarios.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("BLOQUEAR UN HORARIO EN UNA FECHA", color = TextMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Text("Solo ese horario de ese día; los demás siguen disponibles",
                        color = TextDim, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    DatePickerField("Fecha", blockDate, { blockDate = it }, enabled = !isSaving)
                    AdminDropdown(
                        "Horario",
                        horarios.map { "${it.inicio}|${it.fin}" to it.label },
                        blockHorario,
                        { blockHorario = it },
                        enabled = !isSaving
                    )
                    GradientButton(
                        text = "BLOQUEAR",
                        onClick = {
                            val parts = blockHorario.split("|")
                            if (blockDate.isNotBlank() && parts.size == 2) {
                                val slot = BlockedSlot(blockDate, parts[0], parts[1])
                                if (blockedSlots.none {
                                        it.date == slot.date && it.inicio == slot.inicio && it.fin == slot.fin
                                    }) {
                                    blockedSlots = blockedSlots + slot
                                }
                                blockDate = ""; blockHorario = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Gradients.PurpleBlue,
                        icon = Icons.Default.Block
                    )
                    FlowRowSimple(Modifier.padding(top = 10.dp)) {
                        blockedSlots.forEach { b ->
                            val fin = if (b.fin.isBlank()) "" else " – ${b.fin}"
                            RemovablePill(
                                "${prettyDate(b.date)} · ${b.inicio}$fin",
                                { blockedSlots = blockedSlots - b },
                                tone = OrangeWarm
                            )
                        }
                    }
                }
            }

            // ═══════ BLOQUES DE AJUSTES ═══════
            EditorSection("Etiquetas, FAQ e información", Icons.Default.Style) {
                Text("ETIQUETAS DEL PLAN", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                ChipMultiSelect(
                    options = settings.planTags.map { it.id to it.name },
                    selected = tagIds,
                    onToggle = { id -> tagIds = if (id in tagIds) tagIds - id else tagIds + id },
                    emptyHint = "Todavía no hay etiquetas. Se crean en Config. de planes → Etiquetas."
                )

                Spacer(Modifier.height(10.dp))
                Text("PREGUNTAS FRECUENTES", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                ChipMultiSelect(
                    options = settings.faqs.map { it.id to it.q },
                    selected = faqIds,
                    onToggle = { id -> faqIds = if (id in faqIds) faqIds - id else faqIds + id },
                    emptyHint = "Todavía no hay preguntas. Se crean en Config. de planes."
                )

                Spacer(Modifier.height(10.dp))
                Text("INFORMACIÓN IMPORTANTE", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                ChipMultiSelect(
                    options = settings.infos.map { it.id to it.title },
                    selected = infoIds,
                    onToggle = { id -> infoIds = if (id in infoIds) infoIds - id else infoIds + id },
                    emptyHint = "Todavía no hay bloques de información. Se crean en Config. de planes."
                )

                Spacer(Modifier.height(10.dp))
                Text("HORARIOS INFORMATIVOS", color = TextMuted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text("Solo se muestran; no se pueden elegir al reservar",
                    color = TextDim, fontSize = 11.sp)
                ChipMultiSelect(
                    options = settings.schedules.map { it.id to it.text },
                    selected = scheduleIds,
                    onToggle = { id ->
                        scheduleIds = if (id in scheduleIds) scheduleIds - id else scheduleIds + id
                    },
                    emptyHint = "Todavía no hay horarios informativos. Se crean en Config. de planes."
                )
            }

            // ═══════ RESEÑAS ═══════
            EditorSection(
                "Testimonios", Icons.Default.Star,
                summary = if (testimonials.isEmpty()) null else "${testimonials.size}"
            ) {
                Text(
                    "Reseñas que se muestran en la página del plan; aquí puedes pegar las de Google.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp
                )
                testimonials.forEachIndexed { idx, t ->
                    TestimonialCard(
                        testimonial = t,
                        siteBaseUrl = siteBaseUrl,
                        isUploadingImg = uploading == "testi" && testimonialImgIndex == idx,
                        onChange = { updated ->
                            testimonials = testimonials.toMutableList().also { it[idx] = updated }
                        },
                        onPickImage = {
                            testimonialImgIndex = idx
                            testimonialImgPicker.launch("image/*")
                        },
                        onDelete = {
                            testimonials = testimonials.toMutableList().also { it.removeAt(idx) }
                        },
                        enabled = !isSaving
                    )
                }
                GradientButton(
                    text = "AGREGAR TESTIMONIO",
                    onClick = { testimonials = testimonials + Testimonial() },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    gradient = Gradients.PurpleBlue,
                    icon = Icons.Default.Add
                )
            }

            // ═══════ MENSAJES ═══════
            EditorSection("Mensajes", Icons.Default.Chat) {
                AdminField(
                    "Mensaje de WhatsApp para este plan", whatsappMessage, { whatsappMessage = it },
                    hint = "Déjalo vacío para usar el mensaje predeterminado de Ajustes",
                    singleLine = false, minLines = 4, enabled = !isSaving
                )
                AdminField(
                    "Nota para el operador de este plan", operatorMessage, { operatorMessage = it },
                    hint = "Cada operador pide los datos a su manera. Vacío = nota predeterminada",
                    singleLine = false, minLines = 4, enabled = !isSaving
                )
            }

            // ═══════ PUNTO DE ENCUENTRO ═══════
            EditorSection("Punto de encuentro y mapa", Icons.Default.Place) {
                AdminDropdown(
                    "Muelle",
                    listOf("" to "— Ninguno —") + muelles.map { it.id to it.name },
                    muelleId,
                    { muelleId = it },
                    hint = "Sale en el correo de confirmación, con su foto y su enlace a Google Maps. " +
                        "Los muelles se crean en Config. de planes.",
                    enabled = !isSaving
                )
                AdminField(
                    "Mapa para la página web", mapEmbed, { mapEmbed = it },
                    hint = "En Google Maps: Compartir → Insertar un mapa → Copiar HTML, y pégalo aquí",
                    placeholder = "<iframe src=\"https://www.google.com/maps/embed?…\"></iframe>",
                    singleLine = false, minLines = 3, enabled = !isSaving
                )
            }

            // ═══════ CALENDARIO ═══════
            EditorSection("Calendario de las reservas", Icons.Default.CalendarMonth) {
                val gcalOptions = buildList {
                    add("" to "— Calendario general (cuenta activa) —")
                    gcalAccounts.forEach { acc ->
                        acc.calendars.forEach { c ->
                            val principal = if (c.primary) " · principal" else ""
                            add("${acc.email}||${c.id}" to "${acc.email} — ${c.summary}$principal")
                        }
                    }
                }
                AdminDropdown(
                    "Calendario",
                    gcalOptions,
                    gcalValue,
                    { gcalValue = it },
                    hint = if (gcalAccounts.isEmpty()) {
                        "No hay cuentas de Google vinculadas, o no se pudieron cargar sus calendarios."
                    } else {
                        "Al confirmar una reserva de este plan, el evento se crea en este calendario."
                    },
                    enabled = !isSaving
                )
                gcalAccounts.filter { !it.error.isNullOrBlank() }.forEach { acc ->
                    Text(
                        "⚠ ${acc.email}: ${acc.error} — reconéctala en Google Calendar.",
                        color = OrangeWarm, fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ═══════ TRADUCCIÓN ═══════
            EditorSection(
                "Traducción al inglés", Icons.Default.Translate,
                summary = if (tour?.en != null) "✓ traducido" else null
            ) {
                Text(
                    "Lo que escribas aquí es lo que ve el visitante en theextramille.online/en/. " +
                        "Lo que dejes vacío se muestra en español.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp
                )
                AdminField("Plan name", enTitle, { enTitle = it }, enabled = !isSaving)
                AdminField("Short description", enShort, { enShort = it },
                    singleLine = false, minLines = 2, enabled = !isSaving)
                AdminField("Full description", enDescription, { enDescription = it },
                    singleLine = false, minLines = 4, enabled = !isSaving)
                AdminField("Price label", enPriceLabel, { enPriceLabel = it },
                    placeholder = "COP per person", enabled = !isSaving)
                AdminField("What's included", enIncludes, { enIncludes = it },
                    hint = "Una línea por ítem, igual que en español",
                    singleLine = false, minLines = 4, enabled = !isSaving)
            }

            if (pickerTarget != null) {
                val esGaleria = pickerTarget == "gallery"
                UploadedImagePicker(
                    images = uploadedImages,
                    isLoading = loadingImages,
                    siteBaseUrl = siteBaseUrl,
                    selected = if (esGaleria) gallery else listOfNotNull(imageUrl.ifBlank { null }),
                    multi = esGaleria,
                    onPick = { url ->
                        if (esGaleria) {
                            // Volver a tocarla la quita: así se corrige sin cerrar
                            gallery = if (url in gallery) gallery - url else gallery + url
                        } else {
                            imageUrl = url
                            pickerTarget = null
                        }
                    },
                    onDismiss = { pickerTarget = null }
                )
            }

            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusCancelledBg,
                    contentColor = OrangeRed
                ) { Text(error!!, modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            }

            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = if (isNew) "CREAR PLAN" else "GUARDAR CAMBIOS",
                onClick = {
                    if (title.isBlank()) { error = "El nombre del plan es obligatorio"; return@GradientButton }
                    error = null
                    scope.launch {
                        isSaving = true

                        val en = TourTranslation(
                            title = enTitle.trim(),
                            shortDesc = enShort.trim(),
                            description = enDescription.trim(),
                            priceLabel = enPriceLabel.trim(),
                            includes = enIncludes.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        )
                        // Igual que collectTourEn() en el panel: si no se escribió
                        // nada, el plan se queda SIN traducción en vez de con una vacía.
                        val enOrNull = if (
                            en.title.isBlank() && en.shortDesc.isBlank() && en.description.isBlank() &&
                            en.priceLabel.isBlank() && en.includes.isEmpty()
                        ) null else en

                        val (gcalAcc, gcalCal) = gcalValue.split("||").let {
                            if (it.size == 2) it[0] to it[1] else "" to ""
                        }

                        val base = tour ?: Tour()
                        val saved = base.copy(
                            id = tour?.id ?: title.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-").trim('-')
                                .ifBlank { "plan" } + "-" + UUID.randomUUID().toString().take(6),
                            title = title.trim(),
                            shortDesc = shortDesc.trim(),
                            description = description.trim(),
                            category = category,
                            price = price.trim(),
                            priceBefore = priceBefore.trim(),
                            priceLabel = priceLabel.trim(),
                            // El precio neto solo lo manda quien puede verlo; si no,
                            // se conserva el que ya tenía el plan.
                            priceNet = if (canEditNet) priceNet.trim() else base.priceNet,
                            img = imageUrl,
                            gallery = gallery,
                            videoWeb = videoWeb.trim(),
                            videoMobile = videoMobile.trim(),
                            active = active,
                            popular = popular,
                            includes = includes,
                            testimonials = testimonials.filter {
                                it.text.isNotBlank() || it.name.isNotBlank()
                            },
                            cupos = cupos.trim(),
                            showCupos = showCupos,
                            horarios = horarios,
                            // Un bloqueo cuyo horario ya no existe no se guarda
                            blockedSlots = blockedSlots.filter { b ->
                                horarios.any { it.inicio == b.inicio && it.fin == b.fin }
                            },
                            unavailableDates = unavailableDates,
                            muelleId = muelleId,
                            mapEmbed = mapEmbed.trim(),
                            gcalAccount = gcalAcc,
                            gcalCalendarId = gcalCal,
                            whatsappMessage = whatsappMessage.trim(),
                            operatorMessage = operatorMessage.trim(),
                            faqIds = faqIds,
                            infoIds = infoIds,
                            scheduleIds = scheduleIds,
                            tagIds = tagIds,
                            en = enOrNull
                        )
                        val ok = viewModel.saveTour(saved, isNew = isNew)
                        isSaving = false
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = Gradients.PurplePink,
                icon = Icons.Default.Save,
                isLoading = isSaving,
                enabled = uploading == null
            )
        }
    }
}

/**
 * "Elegir de mis imágenes" — el mismo atajo que el panel web.
 *
 * Reutilizar una foto que ya está subida evita duplicar el archivo en el
 * servidor y es mucho más rápido que volver a subirla desde el teléfono.
 */
@Composable
private fun PickFromUploadsButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = Purple, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Elegir de mis imágenes", color = Purple, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Una de las dos ranuras de vídeo: enlace pegado o MP4 subido del teléfono */
@Composable
private fun VideoSlot(
    label: String,
    url: String,
    onUrlChange: (String) -> Unit,
    onPick: () -> Unit,
    isBusy: Boolean,
    enabled: Boolean
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite2)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (url.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onUrlChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, "Quitar vídeo", tint = OrangeRed,
                        modifier = Modifier.size(16.dp))
                }
            }
        }
        AdminField(
            "", url, onUrlChange,
            placeholder = "Pega el enlace de YouTube o Google Drive",
            enabled = enabled && !isBusy
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && !isBusy, onClick = onPick),
            shape = RoundedCornerShape(12.dp),
            color = GlassWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Purple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Subiendo vídeo…", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.UploadFile, null, tint = Purple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Subir MP4 del teléfono", color = Purple, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Ficha editable de una reseña */
@Composable
private fun TestimonialCard(
    testimonial: Testimonial,
    siteBaseUrl: String,
    isUploadingImg: Boolean,
    onChange: (Testimonial) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite2)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassWhite)
                    .clickable(enabled = enabled && !isUploadingImg, onClick = onPickImage),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploadingImg -> CircularProgressIndicator(
                        color = Purple, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                    )
                    testimonial.img.isNotBlank() -> AsyncImage(
                        model = absoluteUrl(testimonial.img, siteBaseUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Icon(Icons.Default.AddAPhoto, null, tint = TextMuted,
                        modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                AdminField("", testimonial.name, { onChange(testimonial.copy(name = it)) },
                    placeholder = "Nombre", enabled = enabled)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(17.dp))
            }
        }

        // Estrellas
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            (1..5).forEach { star ->
                Icon(
                    if (star <= testimonial.rating) Icons.Default.Star else Icons.Default.StarBorder,
                    "$star estrellas",
                    tint = if (star <= testimonial.rating) Pink else TextDim,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(enabled = enabled) { onChange(testimonial.copy(rating = star)) }
                        .padding(2.dp)
                )
            }
        }

        AdminField("", testimonial.text, { onChange(testimonial.copy(text = it)) },
            placeholder = "Texto de la reseña", singleLine = false, minLines = 2, enabled = enabled)
        AdminField("", testimonial.textEn, { onChange(testimonial.copy(textEn = it)) },
            placeholder = "Review in English (opcional)", singleLine = false, minLines = 2,
            enabled = enabled)
        AdminField("", testimonial.date, { onChange(testimonial.copy(date = it)) },
            placeholder = "Hace un mes", enabled = enabled)
    }
}

/**
 * Copia el contenido de un Uri a un archivo temporal para poder subirlo.
 *
 * La extensión es solo orientativa: upload.php mira el contenido real del
 * archivo (finfo) y de ahí saca el nombre definitivo, así que un MP4 no se
 * queda en .jpg por llamarse así.
 */
private fun uriToTempFile(context: Context, uri: Uri, extension: String = ".jpg"): File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val tmpFile = File.createTempFile("upload_", extension, context.cacheDir)
        FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
        input.close()
        tmpFile
    } catch (e: Exception) { null }
}
