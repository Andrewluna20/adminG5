package com.theextramile.admin.ui.reservations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Discount
import com.theextramile.admin.data.model.Horario
import com.theextramile.admin.ui.components.AdminDropdown
import com.theextramile.admin.ui.components.AdminField
import com.theextramile.admin.ui.components.DatePickerField
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.fmtHora
import com.theextramile.admin.util.payFormat
import com.theextramile.admin.util.payFormatMoney
import com.theextramile.admin.util.payParse
import java.time.LocalDate

/**
 * Registrar una reserva a mano: la que entró por teléfono, WhatsApp o en
 * punto de venta.
 *
 * Es el equivalente del modal "Nueva reserva" del panel web, con la misma
 * cuenta de precios: el total que se propone es
 * (precio del plan − descuento) × pasajeros, y se puede corregir a mano.
 *
 * La reserva nace PENDIENTE a propósito; ver createReservation() en
 * ReservationsViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReservationSheet(
    viewModel: ReservationsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tours by viewModel.tours.collectAsState()
    val sellers by viewModel.sellers.collectAsState()

    // Solo se pueden vender los planes visibles
    val vendibles = remember(tours) { tours.filter { it.active } }

    var tourId by remember { mutableStateOf("") }
    var horarioValue by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var paxText by remember { mutableStateOf("1") }
    var dateIso by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sellerId by remember { mutableStateOf("") }

    var discType by remember { mutableStateOf("") }
    var discValueText by remember { mutableStateOf("") }
    /** El descuento que trajo el plan, para saber si se tocó a mano */
    var autoDiscount by remember { mutableStateOf<Discount?>(null) }

    var payMode by remember { mutableStateOf(PaymentMode.PARTIAL) }
    var totalText by remember { mutableStateOf("") }
    var depositText by remember { mutableStateOf("") }
    var arrivalText by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }

    val tour = vendibles.firstOrNull { it.id == tourId }
    val horarios: List<Horario> = tour?.horarios?.filter { it.inicio.isNotBlank() } ?: emptyList()
    val pax = paxText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val unitPrice = payParse(tour?.price)
    val discValue = payParse(discValueText)
    val off = viewModel.descuentoPorPersona(
        Discount(type = discType, value = discValue), unitPrice
    ).takeIf { discType.isNotBlank() && discValue > 0 } ?: 0

    /* Al cambiar de plan se rellena el descuento que ese plan tenga
       configurado en Descuentos, y se limpia el horario porque los
       horarios son de cada plan. Después se puede cambiar a mano: esto
       solo corre cuando cambia el plan, igual que en el panel. */
    LaunchedEffect(tourId) {
        horarioValue = ""
        val d = tour?.let { viewModel.discountForTour(it) }
        autoDiscount = d
        discType = d?.type ?: ""
        discValueText = if (d != null) d.value.toString() else ""
    }

    /* El total se propone solo. Se recalcula al cambiar plan, pasajeros o
       descuento; si se corrige a mano, se respeta hasta el siguiente cambio. */
    LaunchedEffect(tourId, pax, discType, discValueText) {
        if (unitPrice > 0) totalText = payFormat((unitPrice - off) * pax)
    }

    val total = payParse(totalText)
    val deposit: Int
    val balance: Int
    when (payMode) {
        PaymentMode.PAID -> { deposit = total; balance = 0 }
        PaymentMode.ARRIVAL -> {
            deposit = 0
            balance = payParse(arrivalText).let { if (total > 0) minOf(it, total) else it }
        }
        PaymentMode.PARTIAL -> {
            deposit = payParse(depositText).let { if (total > 0) minOf(it, total) else it }
            balance = (total - deposit).coerceAtLeast(0)
        }
    }
    val paymentStatus = when (payMode) {
        PaymentMode.PAID -> "paid"
        PaymentMode.ARRIVAL -> "arrival"
        PaymentMode.PARTIAL -> if (balance > 0) "partial" else "paid"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!uiState.isCreating) onDismiss() },
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
                Column(Modifier.weight(1f)) {
                    Text("Nueva reserva", color = TextPrimary, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold)
                    Text(
                        "Para la que entró por teléfono, WhatsApp o en punto de venta",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .clickable(enabled = !uiState.isCreating) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary,
                        modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ═══════ Plan ═══════
            AdminDropdown(
                "Plan *",
                listOf("" to "— Elige un plan —") + vendibles.map { it.id to it.title },
                tourId,
                { tourId = it },
                hint = if (vendibles.isEmpty()) "No se pudieron cargar los planes." else null,
                enabled = !uiState.isCreating
            )

            if (horarios.isNotEmpty()) {
                AdminDropdown(
                    "Horario",
                    listOf("" to "— Sin horario —") + horarios.map { h ->
                        val fin = if (h.fin.isBlank()) "" else " – ${fmtHora(h.fin)}"
                        "${h.inicio}|${h.fin}" to "${fmtHora(h.inicio)}$fin"
                    },
                    horarioValue,
                    { horarioValue = it },
                    enabled = !uiState.isCreating
                )
            }

            // ═══════ Cliente ═══════
            AdminField("Nombre del cliente *", name, { name = it }, enabled = !uiState.isCreating)
            AdminField(
                "Teléfono *", phone, { phone = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !uiState.isCreating
            )
            AdminField(
                "Correo", email, { email = it },
                hint = "Si lo pones, le llega el correo de «reserva recibida»",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isCreating
            )
            Row {
                Box(Modifier.weight(1f).padding(end = 6.dp)) {
                    AdminField(
                        "Pasajeros", paxText, { paxText = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isCreating
                    )
                }
                Box(Modifier.weight(1f).padding(start = 6.dp)) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("FECHA *", color = TextMuted, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Spacer(Modifier.height(6.dp))
                        DatePickerField("Elegir fecha", dateIso, { dateIso = it },
                            enabled = !uiState.isCreating)
                    }
                }
            }
            AdminField(
                "Notas", notes, { notes = it },
                singleLine = false, minLines = 2, enabled = !uiState.isCreating
            )
            AdminDropdown(
                "Vendido por",
                listOf("" to "— Nadie —") + sellers.map { it.id to it.name },
                sellerId,
                { sellerId = it },
                hint = "Queda estampado en la reserva. Los vendedores se crean en Config. de planes.",
                enabled = !uiState.isCreating
            )

            // ═══════ Descuento ═══════
            Text("DESCUENTO", color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1.3f).padding(end = 6.dp)) {
                    AdminDropdown(
                        "",
                        listOf(
                            "" to "Sin descuento",
                            Discount.TYPE_PERCENT to "Porcentaje",
                            Discount.TYPE_AMOUNT to "Monto por persona"
                        ),
                        discType,
                        { discType = it; if (it.isBlank()) discValueText = "" },
                        enabled = !uiState.isCreating
                    )
                }
                Box(Modifier.weight(1f).padding(start = 6.dp)) {
                    AdminField(
                        "", discValueText, { discValueText = it.filter { c -> c.isDigit() } },
                        placeholder = if (discType == Discount.TYPE_PERCENT) "15" else "50000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isCreating && discType.isNotBlank()
                    )
                }
            }
            if (unitPrice > 0) {
                DiscountInfo(
                    unitPrice = unitPrice,
                    off = off,
                    pax = pax,
                    autoName = autoDiscount
                        ?.takeIf { it.type == discType && it.value == discValue }
                        ?.name
                )
            }

            // ═══════ Pago ═══════
            Text("PAGO", color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                PaymentMode.entries.forEach { mode ->
                    val on = payMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .clickable(enabled = !uiState.isCreating) { payMode = mode },
                        shape = RoundedCornerShape(12.dp),
                        color = if (on) Purple.copy(alpha = 0.12f) else GlassWhite2,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, if (on) Purple else GlassBorder
                        )
                    ) {
                        Text(
                            mode.label,
                            color = if (on) Purple else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            AdminField(
                "Total", totalText, { totalText = it.filter { c -> c.isDigit() } },
                hint = "Se propone solo a partir del plan; puedes corregirlo",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !uiState.isCreating
            )
            when (payMode) {
                PaymentMode.PARTIAL -> AdminField(
                    "Abono recibido", depositText, { depositText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !uiState.isCreating
                )
                PaymentMode.ARRIVAL -> AdminField(
                    "Monto que pagará al llegar", arrivalText,
                    { arrivalText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !uiState.isCreating
                )
                PaymentMode.PAID -> {}
            }

            PaymentSummary(payMode, total, deposit, balance)

            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusCancelledBg,
                    contentColor = OrangeRed
                ) { Text(error!!, modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            }

            Spacer(Modifier.height(18.dp))

            GradientButton(
                text = "CREAR RESERVA",
                onClick = {
                    val t = tour
                    val correo = email.trim().lowercase()
                    error = when {
                        t == null -> "Elige un plan."
                        name.isBlank() -> "Escribe el nombre del cliente."
                        phone.isBlank() -> "Escribe el teléfono del cliente."
                        dateIso.isBlank() -> "Elige la fecha del plan."
                        correo.isNotBlank() && !EMAIL_RE.matches(correo) -> "El correo no es válido."
                        else -> null
                    }
                    if (error != null || t == null) return@GradientButton

                    val partes = horarioValue.split("|")
                    viewModel.createReservation(
                        tour = t,
                        name = name.trim(),
                        phone = phone.trim(),
                        email = correo,
                        pax = pax,
                        date = LocalDate.parse(dateIso),
                        horaInicio = partes.getOrNull(0).orEmpty(),
                        horaFin = partes.getOrNull(1).orEmpty(),
                        notes = notes.trim(),
                        sellerId = sellerId,
                        total = total,
                        deposit = deposit,
                        balance = balance,
                        paymentStatus = paymentStatus,
                        discountType = discType,
                        discountValue = discValue,
                        autoDiscount = autoDiscount,
                        onDone = { ok -> if (ok) onDismiss() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = Gradients.PurplePink,
                icon = Icons.Default.Save,
                isLoading = uiState.isCreating
            )
        }
    }
}

private val EMAIL_RE = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/** Explica la cuenta debajo del descuento, como el panel */
@Composable
private fun DiscountInfo(unitPrice: Int, off: Int, pax: Int, autoName: String?) {
    val texto = if (off <= 0) {
        "Precio del plan: ${payFormatMoney(unitPrice)} × $pax = ${payFormatMoney(unitPrice * pax)}"
    } else {
        val origen = if (autoName.isNullOrBlank()) "" else " · descuento «$autoName» del plan"
        "${payFormatMoney(unitPrice)} → ${payFormatMoney(unitPrice - off)} por persona × $pax = " +
            "${payFormatMoney((unitPrice - off) * pax)}\n" +
            "Ahorro del cliente: −${payFormatMoney(off * pax)}$origen"
    }
    Text(
        texto,
        color = if (off > 0) Purple else TextDim,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** El resumen del cobro, con el mismo criterio de colores que las etiquetas de estado */
@Composable
private fun PaymentSummary(mode: PaymentMode, total: Int, deposit: Int, balance: Int) {
    val (fondo, texto, contenido) = when {
        mode == PaymentMode.ARRIVAL ->
            Triple(StatusPendingBg, StatusPendingText, "Paga al llegar: ${payFormatMoney(if (balance > 0) balance else total)}")
        mode == PaymentMode.PAID || balance <= 0 ->
            Triple(StatusConfirmedBg, StatusConfirmedText, "Pagado en su totalidad: ${payFormatMoney(total)}")
        else ->
            Triple(GlassWhite2, TextSecondary,
                "Abono recibido: ${payFormatMoney(deposit)}\nSaldo pendiente: ${payFormatMoney(balance)}")
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = fondo
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode == PaymentMode.PAID || (mode == PaymentMode.PARTIAL && balance <= 0)) {
                Icon(Icons.Default.Check, null, tint = texto, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(contenido, color = texto, fontSize = 13.sp, lineHeight = 19.sp,
                fontWeight = FontWeight.Medium)
        }
    }
}
