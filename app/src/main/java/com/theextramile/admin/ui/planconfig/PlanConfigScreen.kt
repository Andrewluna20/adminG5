package com.theextramile.admin.ui.planconfig

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.theextramile.admin.util.payFormat
import com.theextramile.admin.util.payParse

/**
 * Configuración de planes — descuentos, muelles y vendedores.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun PlanConfigScreen(
    viewModel: PlanConfigViewModel,
    siteBaseUrl: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val discounts by viewModel.discounts.collectAsState()
    val muelles by viewModel.muelles.collectAsState()
    val sellers by viewModel.sellers.collectAsState()
    val sellerBase by viewModel.sellerBase.collectAsState()
    val tours by viewModel.tours.collectAsState()

    val editingDiscount by viewModel.editingDiscount.collectAsState()
    val editingMuelle by viewModel.editingMuelle.collectAsState()
    val editingSeller by viewModel.editingSeller.collectAsState()

    var pendingDelete by remember { mutableStateOf<PendingAction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                title = "Config. de planes",
                subtitle = when (tab) {
                    PlanConfigViewModel.Tab.DISCOUNTS -> "${discounts.count { it.active }} activo(s) de ${discounts.size}"
                    PlanConfigViewModel.Tab.MUELLES -> "${muelles.size} muelle(s)"
                    PlanConfigViewModel.Tab.SELLERS -> "${sellers.size} vendedor(es)"
                },
                onBack = onBack
            )

            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = Color.Transparent,
                contentColor = CyanLight
            ) {
                PlanConfigViewModel.Tab.entries.forEach { t ->
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
            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                if (uiState.isLoading) {
                    SectionPlaceholder("Cargando…", isLoading = true)
                } else when (tab) {
                    PlanConfigViewModel.Tab.DISCOUNTS -> DiscountsList(
                        discounts = discounts,
                        tours = tours,
                        onEdit = viewModel::startEditDiscount,
                        onToggle = viewModel::toggleDiscount,
                        onDelete = { d ->
                            pendingDelete = PendingAction(
                                "Se eliminará el descuento «${d.name}»."
                            ) { viewModel.deleteDiscount(d) }
                        },
                        onCreate = viewModel::startNewDiscount
                    )

                    PlanConfigViewModel.Tab.MUELLES -> MuellesList(
                        muelles = muelles,
                        siteBaseUrl = siteBaseUrl,
                        onEdit = viewModel::startEditMuelle,
                        onDelete = { m ->
                            pendingDelete = PendingAction(
                                "Se eliminará el muelle «${m.name}». Los planes que lo usen " +
                                    "se quedarán sin punto de encuentro."
                            ) { viewModel.deleteMuelle(m) }
                        },
                        onCreate = viewModel::startNewMuelle
                    )

                    PlanConfigViewModel.Tab.SELLERS -> SellersList(
                        sellers = sellers,
                        base = sellerBase,
                        onEdit = viewModel::startEditSeller,
                        onCopyLink = { url -> copyToClipboard(context, url) },
                        onDelete = { s ->
                            pendingDelete = PendingAction(
                                "Se eliminará al vendedor «${s.name}». Su enlace dejará de funcionar."
                            ) { viewModel.deleteSeller(s) }
                        },
                        onCreate = viewModel::startNewSeller
                    )
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgLight,
                    contentColor = CyanLight
                )

                AddFab(
                    onClick = {
                        when (tab) {
                            PlanConfigViewModel.Tab.DISCOUNTS -> viewModel.startNewDiscount()
                            PlanConfigViewModel.Tab.MUELLES -> viewModel.startNewMuelle()
                            PlanConfigViewModel.Tab.SELLERS -> viewModel.startNewSeller()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                )
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
    }

    editingDiscount?.let { draft ->
        DiscountEditorSheet(
            discount = draft,
            tours = tours,
            isSaving = uiState.isSaving,
            onChange = viewModel::updateDiscountDraft,
            onSave = viewModel::saveDiscount,
            onDismiss = viewModel::cancelDiscountEdit
        )
    }

    editingMuelle?.let { draft ->
        MuelleEditorSheet(
            muelle = draft,
            siteBaseUrl = siteBaseUrl,
            isSaving = uiState.isSaving,
            isUploading = uiState.isUploading,
            onChange = viewModel::updateMuelleDraft,
            onPickImage = viewModel::uploadMuelleImage,
            onSave = viewModel::saveMuelle,
            onDismiss = viewModel::cancelMuelleEdit
        )
    }

    editingSeller?.let { draft ->
        SellerEditorSheet(
            seller = draft,
            isSaving = uiState.isSaving,
            onChange = viewModel::updateSellerDraft,
            onSave = viewModel::saveSeller,
            onDismiss = viewModel::cancelSellerEdit
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
private fun DiscountsList(
    discounts: List<Discount>,
    tours: List<Tour>,
    onEdit: (Discount) -> Unit,
    onToggle: (Discount) -> Unit,
    onDelete: (Discount) -> Unit,
    onCreate: () -> Unit
) {
    if (discounts.isEmpty()) {
        SectionPlaceholder(
            "No hay descuentos configurados",
            icon = Icons.Default.LocalOffer,
            actionLabel = "Crear el primero",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(discounts, key = { it.id }) { d ->
            GlassCard(onClick = { onEdit(d) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TonePill(d.displayValue, if (d.active) GreenLight else TextMuted)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            d.name, color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (d.appliesToAll) "Se aplica a todos los planes"
                        else "Se aplica a ${d.tourIds.size} plan(es): " +
                            d.tourIds.mapNotNull { id -> tours.firstOrNull { it.id == id }?.title }
                                .joinToString(", ").ifBlank { d.tourIds.joinToString(", ") },
                        color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onToggle(d) }) {
                            Text(
                                if (d.active) "Desactivar" else "Activar",
                                color = if (d.active) Yellow else GreenLight, fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onDelete(d) }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuellesList(
    muelles: List<Muelle>,
    siteBaseUrl: String,
    onEdit: (Muelle) -> Unit,
    onDelete: (Muelle) -> Unit,
    onCreate: () -> Unit
) {
    if (muelles.isEmpty()) {
        SectionPlaceholder(
            "No hay muelles configurados",
            icon = Icons.Default.Place,
            actionLabel = "Crear el primero",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(muelles, key = { it.id }) { m ->
            GlassCard(onClick = { onEdit(m) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (m.hasImage) {
                        AsyncImage(
                            model = absoluteUrl(m.image, siteBaseUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.name, color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (m.hasMap) "Con enlace de Google Maps" else "Sin enlace de mapa",
                            color = if (m.hasMap) TextSecondary else TextDim, fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onDelete(m) }) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SellersList(
    sellers: List<Seller>,
    base: String,
    onEdit: (Seller) -> Unit,
    onCopyLink: (String) -> Unit,
    onDelete: (Seller) -> Unit,
    onCreate: () -> Unit
) {
    if (sellers.isEmpty()) {
        SectionPlaceholder(
            "No hay vendedores.\nCada vendedor tiene su propio enlace y sus reservas quedan marcadas.",
            icon = Icons.Default.Groups,
            actionLabel = "Crear el primero",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sellers, key = { it.id }) { s ->
            val link = s.url.ifBlank { base.trimEnd('/') + "/" + s.slug }
            GlassCard(onClick = { onEdit(s) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientAvatar(s.name, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.name, color = TextPrimary, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (s.phone.isNotBlank()) {
                                Text(s.phone, color = TextDim, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onDelete(s) }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = OrangeRed, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            link, color = CyanLight, fontSize = 11.sp,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = { onCopyLink(link) }) {
                            Text("Copiar", color = CyanLight, fontSize = 12.sp)
                        }
                    }
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
private fun DiscountEditorSheet(
    discount: Discount,
    tours: List<Tour>,
    isSaving: Boolean,
    onChange: ((Discount) -> Discount) -> Unit,
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
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (discount.id.isBlank()) "Nuevo descuento" else "Editar descuento",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            AdminField("Nombre", discount.name, { v -> onChange { it.copy(name = v) } })

            Text(
                "TIPO", color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
            )
            FilterChipRow(
                options = listOf(
                    Discount.TYPE_PERCENT to "Porcentaje",
                    Discount.TYPE_AMOUNT to "Monto fijo"
                ),
                selected = discount.type,
                onSelect = { v -> onChange { it.copy(type = v) } },
                horizontalPadding = 0.dp
            )

            AdminField(
                if (discount.isPercent) "Porcentaje" else "Monto en pesos",
                if (discount.isPercent) discount.value.toString() else payFormat(discount.value),
                { v ->
                    val n = payParse(v)
                    onChange { it.copy(value = if (it.isPercent) n.coerceAtMost(100) else n) }
                },
                hint = if (discount.isPercent) "El servidor lo recorta a 100 como máximo"
                else "Se resta del total de la reserva",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            FormSectionTitle("Planes")
            AdminSwitch(
                "Aplicar a todos los planes", discount.appliesToAll,
                { checked -> onChange { if (checked) it.copy(tourIds = emptyList()) else it.copy(tourIds = tours.take(1).map { t -> t.id }) } }
            )
            if (!discount.appliesToAll) {
                Spacer(Modifier.height(4.dp))
                tours.forEach { t ->
                    val checked = t.id in discount.tourIds
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                onChange {
                                    it.copy(
                                        tourIds = if (on) it.tourIds + t.id
                                        else it.tourIds.filterNot { id -> id == t.id }
                                    )
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Purple,
                                uncheckedColor = TextDim,
                                checkmarkColor = TextPrimary
                            )
                        )
                        Text(
                            t.title, color = TextSecondary, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FormSectionTitle("Visibilidad")
            AdminSwitch("Activo", discount.active, { v -> onChange { it.copy(active = v) } })

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar descuento",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuelleEditorSheet(
    muelle: Muelle,
    siteBaseUrl: String,
    isSaving: Boolean,
    isUploading: Boolean,
    onChange: ((Muelle) -> Muelle) -> Unit,
    onPickImage: (android.net.Uri) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onPickImage(uri) }

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
                if (muelle.id.isBlank()) "Nuevo muelle" else "Editar muelle",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            AdminField("Nombre", muelle.name, { v -> onChange { it.copy(name = v) } })
            AdminField(
                "Enlace de Google Maps", muelle.mapsUrl, { v -> onChange { it.copy(mapsUrl = v) } },
                hint = "Es el botón que abre el mapa dentro del tiquete del cliente"
            )

            FormSectionTitle("Foto del punto de encuentro")
            if (muelle.hasImage) {
                AsyncImage(
                    model = absoluteUrl(muelle.image, siteBaseUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(8.dp))
            }
            GradientButton(
                text = if (muelle.hasImage) "Cambiar foto" else "Subir foto",
                onClick = { picker.launch("image/*") },
                isLoading = isUploading,
                icon = Icons.Default.Image,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar muelle",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellerEditorSheet(
    seller: Seller,
    isSaving: Boolean,
    onChange: ((Seller) -> Seller) -> Unit,
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
                if (seller.id.isBlank()) "Nuevo vendedor" else "Editar vendedor",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            AdminField("Nombre", seller.name, { v -> onChange { it.copy(name = v) } })
            AdminField(
                "Enlace personalizado", seller.slug, { v -> onChange { it.copy(slug = v) } },
                hint = "Si lo dejas vacío se genera del nombre. Ojo: cambiarlo rompe los " +
                    "enlaces que ese vendedor ya haya repartido."
            )
            AdminField("Teléfono", seller.phone, { v -> onChange { it.copy(phone = v) } })
            AdminField("Correo", seller.email, { v -> onChange { it.copy(email = v) } })

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Guardar vendedor",
                onClick = onSave,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Enlace del vendedor", text))
}
