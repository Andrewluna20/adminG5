package com.theextramile.admin.ui.benefits

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.*
import com.theextramile.admin.ui.blog.absoluteUrl
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

/**
 * Beneficios — port de admin-html/benefits.html + admin-js/benefits.js.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun BenefitsScreen(
    viewModel: BenefitsViewModel,
    siteBaseUrl: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val query by viewModel.query.collectAsState()
    val benefits by viewModel.benefits.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val inEmailCount by viewModel.inEmailCount.collectAsState()

    val editingBenefit by viewModel.editingBenefit.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val editingBooking by viewModel.editingBooking.collectAsState()

    var pendingDelete by remember { mutableStateOf<PendingAction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    val pullState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(
                title = "Beneficios",
                subtitle = when (tab) {
                    BenefitsViewModel.Tab.BOOKINGS -> "${bookings.size} reservado(s)"
                    BenefitsViewModel.Tab.CATALOG -> "${benefits.size} beneficio(s) · $inEmailCount en correos"
                    BenefitsViewModel.Tab.MESSAGES -> "${messages.size} mensaje(s)"
                },
                onBack = onBack
            )

            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = CyanLight
            ) {
                BenefitsViewModel.Tab.entries.forEach { t ->
                    Tab(
                        selected = t == tab,
                        onClick = { viewModel.selectTab(t) },
                        text = {
                            Text(
                                t.title,
                                fontSize = 13.sp,
                                fontWeight = if (t == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (t == tab) TextPrimary else TextMuted
                            )
                        }
                    )
                }
            }

            if (tab == BenefitsViewModel.Tab.BOOKINGS) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SearchField(query, viewModel::onQueryChange, "Buscar por cliente o código…")
                }
            }
            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                if (uiState.isLoading) {
                    SectionPlaceholder("Cargando…", isLoading = true)
                } else when (tab) {
                    BenefitsViewModel.Tab.BOOKINGS -> BookingsList(
                        bookings = bookings,
                        onEdit = viewModel::startEditBooking,
                        onDelete = { b ->
                            pendingDelete = PendingAction(
                                "Se eliminará la reserva de beneficio de ${b.name}."
                            ) { viewModel.deleteBooking(b) }
                        }
                    )

                    BenefitsViewModel.Tab.CATALOG -> CatalogList(
                        benefits = benefits,
                        siteBaseUrl = siteBaseUrl,
                        onEdit = viewModel::startEditBenefit,
                        onToggleActive = viewModel::toggleBenefitActive,
                        onToggleInEmail = viewModel::toggleInEmail,
                        onDelete = { b ->
                            pendingDelete = PendingAction(
                                "Se eliminará «${b.displayTitle}» del catálogo."
                            ) { viewModel.deleteBenefit(b) }
                        },
                        onCreate = viewModel::startNewBenefit
                    )

                    BenefitsViewModel.Tab.MESSAGES -> MessagesList(
                        messages = messages,
                        onEdit = viewModel::startEditMessage,
                        onDelete = { m ->
                            pendingDelete = PendingAction(
                                "Se eliminará el mensaje «${m.name.ifBlank { "sin nombre" }}»."
                            ) { viewModel.deleteMessage(m) }
                        },
                        onCreate = viewModel::startNewMessage
                    )
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgLight,
                    contentColor = CyanLight
                )

                if (tab != BenefitsViewModel.Tab.BOOKINGS) {
                    AddFab(
                        onClick = {
                            if (tab == BenefitsViewModel.Tab.CATALOG) viewModel.startNewBenefit()
                            else viewModel.startNewMessage()
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                    )
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }

    editingBenefit?.let { draft ->
        BenefitEditorSheet(
            benefit = draft,
            messages = messages,
            siteBaseUrl = siteBaseUrl,
            isSaving = uiState.isSaving,
            isUploading = uiState.isUploading,
            onChange = viewModel::updateBenefitDraft,
            onAddImage = viewModel::addBenefitImage,
            onRemoveImage = viewModel::removeBenefitImage,
            onSave = viewModel::saveBenefit,
            onDismiss = viewModel::cancelBenefitEdit
        )
    }

    editingMessage?.let { draft ->
        MessageEditorSheet(
            message = draft,
            isSaving = uiState.isSaving,
            onChange = viewModel::updateMessageDraft,
            onSave = viewModel::saveMessage,
            onDismiss = viewModel::cancelMessageEdit
        )
    }

    editingBooking?.let { draft ->
        BookingEditorSheet(
            booking = draft,
            benefit = benefits.firstOrNull { it.id == draft.benefitId },
            isSaving = uiState.isSaving,
            onChange = viewModel::updateBookingDraft,
            onSave = viewModel::saveBooking,
            onDismiss = viewModel::cancelBookingEdit
        )
    }

    pendingDelete?.let { pending ->
        ConfirmDialog(
            title = "Confirmar",
            message = pending.message,
            onConfirm = { pending.run(); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

// ═══════════════════════════════════════════
// Listas
// ═══════════════════════════════════════════

@Composable
private fun BookingsList(
    bookings: List<BenefitBooking>,
    onEdit: (BenefitBooking) -> Unit,
    onDelete: (BenefitBooking) -> Unit
) {
    if (bookings.isEmpty()) {
        SectionPlaceholder(
            "Todavía ningún cliente ha reservado un beneficio",
            icon = Icons.Default.CardGiftcard
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(bookings, key = { it.id }) { b ->
            GlassCard(onClick = { onEdit(b) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            b.displayBenefit,
                            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onDelete(b) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(b.name, color = TextSecondary, fontSize = 13.sp)
                    if (b.email.isNotBlank()) Text(b.email, color = TextDim, fontSize = 11.sp)
                    if (b.phone.isNotBlank()) Text(b.phone, color = TextDim, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (b.displayDate.isNotBlank()) {
                            TonePill(b.displayDate, CyanLight)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (b.pax > 0) {
                            TonePill("${b.pax} pax", PurpleLight)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (b.reservationId.isNotBlank()) {
                            Text("#${b.reservationId}", color = TextDim, fontSize = 10.sp)
                        }
                    }
                    if (b.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(b.notes, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogList(
    benefits: List<Benefit>,
    siteBaseUrl: String,
    onEdit: (Benefit) -> Unit,
    onToggleActive: (Benefit) -> Unit,
    onToggleInEmail: (Benefit) -> Unit,
    onDelete: (Benefit) -> Unit,
    onCreate: () -> Unit
) {
    if (benefits.isEmpty()) {
        SectionPlaceholder(
            "Aún no hay beneficios en el catálogo",
            icon = Icons.Default.CardGiftcard,
            actionLabel = "Crear el primero",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(benefits, key = { it.id }) { b ->
            GlassCard(onClick = { onEdit(b) }, contentPadding = 0.dp) {
                Column {
                    b.coverImage?.let { cover ->
                        AsyncImage(
                            model = absoluteUrl(cover, siteBaseUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        )
                    }
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TonePill(
                                if (b.active) "Activo" else "Oculto",
                                if (b.active) GreenLight else TextMuted
                            )
                            if (b.inEmail) {
                                Spacer(Modifier.width(6.dp))
                                TonePill("En correos", CyanLight)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${b.images.size} img", color = TextDim, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            b.displayTitle,
                            color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        if (b.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                b.description, color = TextSecondary, fontSize = 12.sp,
                                lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onToggleActive(b) }) {
                                Text(
                                    if (b.active) "Ocultar" else "Activar",
                                    color = if (b.active) Yellow else GreenLight, fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { onToggleInEmail(b) }) {
                                Text(
                                    if (b.inEmail) "Quitar del correo" else "Poner en correo",
                                    color = CyanLight, fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onDelete(b) }) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<BenefitMessage>,
    onEdit: (BenefitMessage) -> Unit,
    onDelete: (BenefitMessage) -> Unit,
    onCreate: () -> Unit
) {
    if (messages.isEmpty()) {
        SectionPlaceholder(
            "No hay mensajes personalizados.\nLos beneficios usarán el texto de siempre.",
            icon = Icons.Default.Email,
            actionLabel = "Crear un mensaje",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { m ->
            GlassCard(onClick = { onEdit(m) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.name.ifBlank { "Sin nombre" },
                            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(m) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        m.text, color = TextSecondary, fontSize = 12.sp,
                        lineHeight = 17.sp, maxLines = 4, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Editores
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BenefitEditorSheet(
    benefit: Benefit,
    messages: List<BenefitMessage>,
    siteBaseUrl: String,
    isSaving: Boolean,
    isUploading: Boolean,
    onChange: ((Benefit) -> Benefit) -> Unit,
    onAddImage: (android.net.Uri) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onAddImage(uri) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (benefit.id.isBlank()) "Nuevo beneficio" else "Editar beneficio",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            AdminField("Título", benefit.title, { v -> onChange { it.copy(title = v) } })
            AdminField(
                "Nombre del sitio", benefit.name, { v -> onChange { it.copy(name = v) } },
                hint = "El restaurante, el bar o el lugar donde se usa el beneficio"
            )
            AdminField(
                "Descripción", benefit.description, { v -> onChange { it.copy(description = v) } },
                singleLine = false, minLines = 3
            )

            FormSectionTitle("Imágenes (${benefit.images.size}/$MAX_BENEFIT_IMAGES)")
            if (benefit.images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(benefit.images, key = { it }) { url ->
                        Box {
                            AsyncImage(
                                model = absoluteUrl(url, siteBaseUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = { onRemoveImage(url) },
                                modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cancel, "Quitar",
                                    tint = OrangeRed, modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            GradientButton(
                text = "Añadir imagen",
                onClick = { picker.launch("image/*") },
                isLoading = isUploading,
                enabled = benefit.images.size < MAX_BENEFIT_IMAGES,
                icon = Icons.Default.Image,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth()
            )

            FormSectionTitle("Qué se le pide al cliente")
            AdminSwitch(
                "Pedir fecha", benefit.askDate, { v -> onChange { it.copy(askDate = v) } },
                hint = "Si lo apagas, el cliente reserva sin elegir día"
            )
            AdminSwitch(
                "Pedir número de personas", benefit.askPax,
                { v -> onChange { it.copy(askPax = v) } }
            )

            FormSectionTitle("Mapa")
            AdminField(
                "Código del mapa", benefit.mapEmbed, { v -> onChange { it.copy(mapEmbed = v) } },
                hint = "Pega aquí el <iframe> de Google Maps. El servidor saca solo la URL.",
                singleLine = false, minLines = 2
            )

            FormSectionTitle("Correo del tiquete")
            AdminSwitch(
                "Sale en los correos al cliente", benefit.inEmail,
                { v -> onChange { it.copy(inEmail = v) } },
                hint = "En cada correo caben $MAX_BENEFITS_IN_EMAIL beneficios como mucho"
            )
            Text(
                "MENSAJE DEL CORREO",
                color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )
            FilterChipRow(
                options = listOf("" to "El de siempre") + messages.map { it.id to it.name.ifBlank { "Sin nombre" } },
                selected = benefit.messageId,
                onSelect = { v -> onChange { it.copy(messageId = v) } },
                horizontalPadding = 0.dp
            )

            FormSectionTitle("Visibilidad")
            AdminSwitch("Activo", benefit.active, { v -> onChange { it.copy(active = v) } })

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar beneficio",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageEditorSheet(
    message: BenefitMessage,
    isSaving: Boolean,
    onChange: ((BenefitMessage) -> BenefitMessage) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (message.id.isBlank()) "Nuevo mensaje" else "Editar mensaje",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            AdminField(
                "Nombre", message.name, { v -> onChange { it.copy(name = v) } },
                hint = "Solo para reconocerlo al asignarlo a un beneficio"
            )
            AdminField(
                "Texto del correo", message.text, { v -> onChange { it.copy(text = v) } },
                hint = "Máximo 900 caracteres. Llevas ${message.text.length}.",
                singleLine = false, minLines = 6
            )
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar mensaje",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingEditorSheet(
    booking: BenefitBooking,
    benefit: Benefit?,
    isSaving: Boolean,
    onChange: ((BenefitBooking) -> BenefitBooking) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    // El backend solo acepta los campos que el beneficio pide
    val pideFecha = benefit?.askDate ?: booking.date.isNotBlank()
    val pidePax = benefit?.askPax ?: (booking.pax > 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Editar reserva", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${booking.displayBenefit} · ${booking.name}",
                color = TextSecondary, fontSize = 13.sp
            )

            if (pideFecha) {
                AdminField(
                    "Fecha", booking.date, { v -> onChange { it.copy(date = v) } },
                    placeholder = "AAAA-MM-DD",
                    hint = "El servidor vuelve a calcular la fecha en palabras"
                )
            }
            if (pidePax) {
                AdminField(
                    "Personas", if (booking.pax > 0) booking.pax.toString() else "",
                    { v -> onChange { it.copy(pax = v.filter(Char::isDigit).toIntOrNull() ?: 0) } },
                    hint = "Entre 1 y 50",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            AdminField(
                "Notas", booking.notes, { v -> onChange { it.copy(notes = v) } },
                singleLine = false, minLines = 3
            )

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar cambios",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
