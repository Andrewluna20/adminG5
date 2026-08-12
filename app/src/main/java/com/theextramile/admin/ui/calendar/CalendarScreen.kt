package com.theextramile.admin.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import java.time.LocalDate

/**
 * Calendario — port de admin-html/calendar.html + admin-js/calendar.js.
 *
 * En la web se arrastra una reserva a otro día; en el celular se toca la
 * reserva y se elige el día nuevo, que es lo que funciona con un dedo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    canManage: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val month by viewModel.month.collectAsState()
    val days by viewModel.days.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val dayReservations by viewModel.selectedReservations.collectAsState()
    val monthCount by viewModel.monthCount.collectAsState()

    var moving by remember { mutableStateOf<Reservation?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
            SectionHeader(
                title = "Calendario",
                subtitle = "$monthCount reserva(s) este mes",
                onBack = onBack,
                actionIcon = Icons.Default.Today,
                actionLabel = "Hoy",
                onAction = { viewModel.goToToday() }
            )

            // ── Mes ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", tint = CyanLight)
                }
                Text(
                    formatMonthTitle(month),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", tint = CyanLight)
                }
            }

            // ── Cabecera de días ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                    Text(
                        d,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.isLoading) {
                SectionPlaceholder("Cargando…", isLoading = true)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(days) { day ->
                        DayCell(
                            day = day,
                            isSelected = day.date != null && day.date == selectedDay,
                            onClick = { day.date?.let { viewModel.selectDay(it) } }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GlassBorder)

                // ── Detalle del día ──
                if (selectedDay == null) {
                    SectionPlaceholder(
                        "Toca un día para ver sus reservas",
                        icon = Icons.Default.CalendarMonth
                    )
                } else {
                    Text(
                        formatSpanishDate(selectedDay!!),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    if (dayReservations.isEmpty()) {
                        SectionPlaceholder("Sin reservas este día", icon = Icons.Default.EventBusy)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dayReservations, key = { it.id }) { r ->
                                DayReservationCard(
                                    r = r,
                                    canManage = canManage,
                                    onMove = { moving = r }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    moving?.let { r ->
        MoveDateDialog(
            reservation = r,
            isSaving = uiState.isSaving,
            onConfirm = { newDate ->
                viewModel.moveReservation(r, newDate)
                moving = null
            },
            onDismiss = { moving = null }
        )
    }
}

@Composable
private fun DayCell(
    day: CalendarViewModel.Day,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (day.date == null) {
        Box(Modifier.height(38.dp))
        return
    }
    val hasReservations = day.reservations.isNotEmpty()
    Box(
        Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> Purple.copy(alpha = 0.35f)
                    hasReservations -> GlassWhite
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .then(
                if (day.isToday) Modifier.border(1.dp, CyanLight, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.date.dayOfMonth.toString(),
                color = if (hasReservations || isSelected) TextPrimary else TextMuted,
                fontSize = 13.sp,
                fontWeight = if (hasReservations) FontWeight.Bold else FontWeight.Normal
            )
            if (hasReservations) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (day.confirmed > 0) Dot(GreenLight)
                    if (day.pending > 0) Dot(Yellow)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(5.dp).clip(CircleShape).background(color))
}

@Composable
private fun DayReservationCard(
    r: Reservation,
    canManage: Boolean,
    onMove: () -> Unit
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!r.horaInicio.isNullOrBlank()) {
                        TonePill(
                            listOfNotNull(r.horaInicio, r.horaFin).joinToString(" – "),
                            CyanLight
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    StatusChip(
                        label = r.statusDisplay,
                        backgroundColor = if (r.isConfirmed) StatusConfirmedBg else StatusPendingBg,
                        contentColor = if (r.isConfirmed) StatusConfirmedText else StatusPendingText
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
            }
            if (canManage) {
                IconButton(onClick = onMove) {
                    Icon(Icons.Default.EditCalendar, "Cambiar fecha", tint = CyanLight)
                }
            }
        }
    }
}

/**
 * Cambiar la fecha de una reserva. Se escribe el día en AAAA-MM-DD: es lo que
 * el backend espera y evita depender del selector nativo, que cambia mucho
 * entre versiones de Android.
 */
@Composable
private fun MoveDateDialog(
    reservation: Reservation,
    isSaving: Boolean,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(reservation.dayKey.ifBlank { LocalDate.now().toString() }) }
    val parsed = remember(text) { runCatching { LocalDate.parse(text) }.getOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgLight,
        titleContentColor = TextPrimary,
        title = { Text("Cambiar fecha", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "${reservation.tourTitle} · ${reservation.name}",
                    color = TextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nueva fecha", color = TextMuted) },
                    placeholder = { Text("AAAA-MM-DD", color = TextDim) },
                    singleLine = true,
                    isError = parsed == null,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (parsed != null) "Quedará el ${formatSpanishDate(parsed)}"
                    else "Escribe la fecha como 2026-08-25",
                    color = if (parsed != null) GreenLight else TextDim,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Se conserva la hora y se mueve también el evento de Google Calendar.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && !isSaving
            ) {
                Text("Mover", color = if (parsed != null) CyanLight else TextDim, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}
