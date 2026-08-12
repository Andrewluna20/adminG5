package com.theextramile.admin.ui.extracto

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.payFormatMoney
import com.theextramile.admin.util.paymentInfo

/**
 * Extracto — port de admin-html/extracto.html.
 *
 * Muestra el resumen y una vista previa; el botón de exportar arma el CSV con
 * TODAS las filas filtradas, no solo las de la vista previa (igual que el .xlsx
 * del panel web).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ExtractoScreen(
    viewModel: ExtractoViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val tourOptions by viewModel.tourOptions.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }

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
                title = "Extracto",
                subtitle = "${summary.reservas} reserva(s) · ${summary.pasajeros} pasajero(s)",
                onBack = onBack,
                actionIcon = Icons.Default.FilterList,
                actionLabel = "Filtros",
                onAction = { showFilters = true }
            )

            // ── Resumen ──
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Reservas", summary.reservas.toString(), Modifier.weight(1f), CyanLight)
                    StatCard("Pasajeros", summary.pasajeros.toString(), Modifier.weight(1f), PurpleLight)
                }
                Spacer(Modifier.height(10.dp))
                StatCard("Total vendido", payFormatMoney(summary.total), Modifier.fillMaxWidth(), TextPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Recibido", payFormatMoney(summary.recibido), Modifier.weight(1f), GreenLight)
                    StatCard("Saldo", payFormatMoney(summary.saldo), Modifier.weight(1f), Yellow)
                }
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Exportar ${summary.reservas} fila(s) a CSV",
                    onClick = {
                        viewModel.exportCsv(context) { file ->
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Extracto de reservas")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir extracto"))
                        }
                    },
                    enabled = rows.isNotEmpty(),
                    gradient = Gradients.GreenCyan,
                    icon = Icons.Default.Download,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                if (rows.size > PREVIEW_MAX)
                    "Vista previa — $PREVIEW_MAX de ${rows.size} (el archivo lleva todas)"
                else "Vista previa — ${rows.size} ${if (rows.size == 1) "reserva" else "reservas"}",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                when {
                    uiState.isLoading -> SectionPlaceholder("Cargando…", isLoading = true)

                    rows.isEmpty() -> SectionPlaceholder(
                        message = "No hay reservas con estos filtros",
                        icon = Icons.Default.ReceiptLong,
                        actionLabel = "Quitar filtros",
                        onAction = { viewModel.clearFilters() }
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rows.take(PREVIEW_MAX), key = { it.id }) { ExtractoRow(it) }
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgLight,
                    contentColor = CyanLight
                )
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    if (showFilters) {
        ExtractoFiltersSheet(
            filters = filters,
            tourOptions = tourOptions,
            onChange = viewModel::updateFilters,
            onClear = { viewModel.clearFilters() },
            onDismiss = { showFilters = false }
        )
    }
}

private const val PREVIEW_MAX = 25

@Composable
private fun ExtractoRow(r: Reservation) {
    val pi = paymentInfo(r)
    GlassCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${r.id}",
                    color = TextDim,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(
                    label = r.statusDisplay,
                    backgroundColor = when {
                        r.isConfirmed -> StatusConfirmedBg
                        r.isCancelled -> StatusCancelledBg
                        else -> StatusPendingBg
                    },
                    contentColor = when {
                        r.isConfirmed -> StatusConfirmedText
                        r.isCancelled -> StatusCancelledText
                        else -> StatusPendingText
                    }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                r.tourTitle, color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${r.name} · ${r.pax} pax",
                color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (r.date.isNotBlank()) {
                Text(r.date, color = TextDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                MoneyCell("Total", payFormatMoney(pi.total), TextPrimary, Modifier.weight(1f))
                MoneyCell("Abonado", payFormatMoney(pi.deposit), GreenLight, Modifier.weight(1f))
                MoneyCell("Saldo", payFormatMoney(pi.balance), Yellow, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoneyCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, color = TextMuted, fontSize = 9.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractoFiltersSheet(
    filters: ExtractoViewModel.Filters,
    tourOptions: List<Pair<String, String>>,
    onChange: ((ExtractoViewModel.Filters) -> ExtractoViewModel.Filters) -> Unit,
    onClear: () -> Unit,
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
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Filtros", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            FormSectionTitle("Rango de fechas")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminField(
                    "Desde", filters.from, { v -> onChange { it.copy(from = v) } },
                    placeholder = "AAAA-MM-DD", modifier = Modifier.weight(1f)
                )
                AdminField(
                    "Hasta", filters.to, { v -> onChange { it.copy(to = v) } },
                    placeholder = "AAAA-MM-DD", modifier = Modifier.weight(1f)
                )
            }
            OptionRow(
                "Aplicar el rango a",
                listOf("tour" to "Fecha del plan", "created" to "Fecha de la reserva"),
                filters.dateField
            ) { v -> onChange { it.copy(dateField = v) } }

            FormSectionTitle("Estado")
            OptionRow(
                "Estado de la reserva",
                listOf(
                    "sold" to "Vendidas",
                    "all" to "Todas",
                    "pending" to "Pendientes",
                    "confirmed" to "Confirmadas",
                    "cancelled" to "Canceladas"
                ),
                filters.status
            ) { v -> onChange { it.copy(status = v) } }

            OptionRow(
                "Estado de pago",
                listOf(
                    "all" to "Todos",
                    "paid" to "Pagadas",
                    "partial" to "Con saldo",
                    "arrival" to "Paga al llegar",
                    "none" to "Sin registro"
                ),
                filters.payment
            ) { v -> onChange { it.copy(payment = v) } }

            if (tourOptions.isNotEmpty()) {
                FormSectionTitle("Plan")
                OptionRow("Plan", tourOptions, filters.tour) { v -> onChange { it.copy(tour = v) } }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("Limpiar") }
                GradientButton(
                    text = "Aplicar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }
        }
    }
}

/** Un desplegable del panel se convierte aquí en una fila de chips */
@Composable
private fun OptionRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            label.uppercase(),
            color = TextMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(8.dp))
        FilterChipRow(
            options = options,
            selected = selected,
            onSelect = onSelect,
            horizontalPadding = 0.dp
        )
    }
}
