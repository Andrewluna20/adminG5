package com.theextramile.admin.ui.reservations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.data.repository.InvoiceRepository
import com.theextramile.admin.TEMApplication
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.PhoneUtil

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel,
    onBack: () -> Unit,
    adminWhatsApp: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    val reservations by viewModel.filteredReservations.collectAsState()
    val counts by viewModel.statusCounts.collectAsState()
    val context = LocalContext.current

    // ★ Acceso al SettingsRepository (para enviar facturas con datos de empresa)
    val app = context.applicationContext as TEMApplication
    val settings by app.settingsRepository.settings.collectAsState()
    val invoiceRepo = remember { InvoiceRepository() }

    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it); viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it); viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        // Orbes decorativos
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset((-80).dp, (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Purple.copy(alpha = 0.2f), Color.Transparent))
                )
                .blur(60.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    // Header con back y delete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassWhite)
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Atrás",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reservas",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                "${reservations.size} resultado(s)",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (uiState.selectedIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33EF4444))
                                    .clickable { viewModel.deleteSelected() },
                                contentAlignment = Alignment.Center
                            ) {
                                Box {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Eliminar",
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset(x = 12.dp, y = (-8).dp)
                                            .clip(CircleShape)
                                            .background(OrangeRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${uiState.selectedIds.size}",
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buscador glassmorphism
                    SearchBar(
                        query = uiState.searchQuery,
                        onChange = viewModel::setSearchQuery
                    )

                    Spacer(Modifier.height(14.dp))

                    // Filtros tabs
                    FilterChipsRow(
                        current = uiState.filter,
                        counts = counts,
                        onChange = viewModel::setFilter
                    )

                    Spacer(Modifier.height(12.dp))
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullRefresh(pullRefreshState)
            ) {
                if (reservations.isEmpty() && !uiState.isRefreshing) {
                    EmptyState(filter = uiState.filter)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reservations, key = { it.id }) { res ->
                            ReservationCard(
                                reservation = res,
                                isSelected = res.id in uiState.selectedIds,
                                isUpdating = res.id in uiState.updatingIds,
                                onClick = {
                                    if (uiState.selectedIds.isNotEmpty() && res.isCancelled) {
                                        viewModel.toggleSelection(res.id)
                                    } else {
                                        selectedReservation = res
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgMid,
                    contentColor = Purple
                )
            }
        }
    }

    selectedReservation?.let { res ->
        ReservationDetailSheet(
            reservation = res,
            isUpdating = res.id in uiState.updatingIds,
            onDismiss = { selectedReservation = null },
            onConfirm = {
                viewModel.confirmReservation(res.id)
                selectedReservation = null
            },
            onCancel = {
                viewModel.cancelReservation(res.id)
                selectedReservation = null
            },
            onRestore = {
                viewModel.restoreToPending(res.id)
                selectedReservation = null
            },
            onWhatsAppClient = {
                val msg = PhoneUtil.defaultWaMessage(res)
                PhoneUtil.openWhatsApp(context, res.phone, msg)
            },
            onWhatsAppForward = {
                val msg = PhoneUtil.forwardWaMessage(res)
                PhoneUtil.openWhatsApp(context, adminWhatsApp, msg)
            },
            onCall = {
                PhoneUtil.openDialer(context, res.phone)
            },
            onSendInvoice = {
                invoiceRepo.sendInvoiceViaWhatsApp(context, res, settings)
            },
            onPreviewInvoice = {
                invoiceRepo.previewInvoice(context, res.id)
            },
            hasAdminWhatsApp = adminWhatsApp.isNotBlank()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, "Limpiar", tint = TextSecondary)
                }
            }
        },
        placeholder = {
            Text("Buscar reservas…", color = TextMuted, fontSize = 14.sp)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = Color(0x0FFFFFFF),
            unfocusedContainerColor = Color(0x0AFFFFFF),
            focusedBorderColor = Purple,
            unfocusedBorderColor = GlassBorder,
            cursorColor = Purple
        )
    )
}

@Composable
private fun FilterChipsRow(
    current: ReservationFilter,
    counts: Map<String, Int>,
    onChange: (ReservationFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                "Pendientes",
                counts["pending"] ?: 0,
                current == ReservationFilter.PENDING,
                Gradients.OrangePink,
                StatusPendingText
            ) { onChange(ReservationFilter.PENDING) }
        }
        item {
            FilterChip(
                "Confirmadas",
                counts["confirmed"] ?: 0,
                current == ReservationFilter.CONFIRMED,
                Gradients.GreenCyan,
                StatusConfirmedText
            ) { onChange(ReservationFilter.CONFIRMED) }
        }
        item {
            FilterChip(
                "Canceladas",
                counts["cancelled"] ?: 0,
                current == ReservationFilter.CANCELLED,
                Brush.linearGradient(listOf(OrangeRed, Pink)),
                StatusCancelledText
            ) { onChange(ReservationFilter.CANCELLED) }
        }
        item {
            FilterChip(
                "Todas",
                counts["all"] ?: 0,
                current == ReservationFilter.ALL,
                Gradients.PurplePink,
                TextSecondary
            ) { onChange(ReservationFilter.ALL) }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    active: Boolean,
    activeGradient: Brush,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (active) Modifier.background(activeGradient)
                else Modifier.background(GlassWhite)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (active) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) Color.White.copy(alpha = 0.25f)
                            else GlassBorderStrong
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        "$count",
                        color = if (active) TextPrimary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    isSelected: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit
) {
    val (statusBg, statusText, statusLabel) = when {
        reservation.isConfirmed -> Triple(StatusConfirmedBg, StatusConfirmedText, "CONFIRMADA")
        reservation.isCancelled -> Triple(StatusCancelledBg, StatusCancelledText, "CANCELADA")
        else -> Triple(StatusPendingBg, StatusPendingText, "PENDIENTE")
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        contentPadding = 14.dp,
        backgroundColor = if (isSelected) Color(0x33EF4444) else GlassWhite,
        borderColor = if (isSelected) OrangeRed.copy(alpha = 0.5f) else GlassBorder
    ) {
        Row {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Gradients.forAvatar(reservation.name)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUpdating -> CircularProgressIndicator(
                        color = TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    isSelected -> Icon(Icons.Default.Check, null, tint = TextPrimary)
                    else -> Text(
                        reservation.name.take(1).uppercase(),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        reservation.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(statusLabel, statusBg, statusText)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${reservation.tourTitle} · ${reservation.pax} pax",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(reservation.date, color = TextMuted, fontSize = 10.sp)
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Phone,
                        null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        reservation.phone.take(15),
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "#${reservation.shortId}",
                        color = TextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(filter: ReservationFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(GlassWhite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (filter) {
                    ReservationFilter.PENDING -> "⏳"
                    ReservationFilter.CONFIRMED -> "✓"
                    ReservationFilter.CANCELLED -> "✕"
                    ReservationFilter.ALL -> "📭"
                },
                fontSize = 36.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            when (filter) {
                ReservationFilter.PENDING -> "No hay reservas pendientes"
                ReservationFilter.CONFIRMED -> "No hay reservas confirmadas"
                ReservationFilter.CANCELLED -> "No hay reservas canceladas"
                ReservationFilter.ALL -> "No hay reservas aún"
            },
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Desliza hacia abajo para refrescar",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
