package com.theextramile.admin.ui.reservations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Horario
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.ui.components.AdminDropdown
import com.theextramile.admin.ui.components.AdminField
import com.theextramile.admin.ui.components.DatePickerField
import com.theextramile.admin.ui.components.FilterChipRow
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.fmtHora
import com.theextramile.admin.util.payFormat
import com.theextramile.admin.util.payFormatMoney
import com.theextramile.admin.util.payParse
import com.theextramile.admin.util.paymentInfo
import java.time.LocalDate

/**
 * Registrar el pago de una reserva — port del modal de pago del panel
 * (submitPayment en admin-js/reservations.js).
 *
 * ⚠️ Guardar aquí también CONFIRMA la reserva, igual que en la web: es
 * lo que hace que el servidor mande el correo con el tiquete. Por eso el
 * botón lo dice claramente en vez de un "Guardar" a secas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    reservation: Reservation,
    isSaving: Boolean,
    onSave: (PaymentMode, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val actual = remember(reservation) { paymentInfo(reservation) }

    var mode by remember {
        mutableStateOf(
            when (reservation.paymentStatus) {
                "paid" -> PaymentMode.PAID
                "arrival" -> PaymentMode.ARRIVAL
                else -> PaymentMode.PARTIAL
            }
        )
    }
    var total by remember { mutableStateOf(payFormat(actual.total)) }
    var monto by remember {
        mutableStateOf(
            payFormat(if (reservation.paymentStatus == "arrival") actual.balance else actual.deposit)
        )
    }

    // Se recalcula al vuelo para que se vea el saldo antes de guardar
    val totalNum = payParse(total)
    val montoNum = payParse(monto)
    val saldo = when (mode) {
        PaymentMode.PAID -> 0
        PaymentMode.ARRIVAL -> if (totalNum > 0 && montoNum > totalNum) totalNum else montoNum
        PaymentMode.PARTIAL -> (totalNum - montoNum).coerceAtLeast(0)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
            Text("Pago de la reserva", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                "${reservation.tourTitle} · ${reservation.name}",
                color = TextSecondary, fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))
            FilterChipRow(
                options = PaymentMode.entries.map { it.name to it.label },
                selected = mode.name,
                onSelect = { v -> mode = PaymentMode.valueOf(v) },
                horizontalPadding = 0.dp
            )

            AdminField(
                "Total de la reserva", total, { total = payFormat(it) },
                hint = "Lo que vale en total, sin descontar el abono",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            when (mode) {
                PaymentMode.PARTIAL -> AdminField(
                    "Abono recibido", monto, { monto = payFormat(it) },
                    hint = "Si iguala al total, la reserva queda como pagada",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                PaymentMode.ARRIVAL -> AdminField(
                    "Pagará al llegar", monto, { monto = payFormat(it) },
                    hint = "No se registra abono; queda todo pendiente de cobro",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                PaymentMode.PAID -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Se registra el total como pagado y no queda saldo.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            }

            // ── Resumen antes de guardar ──
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (saldo > 0) StatusPendingBg else StatusConfirmedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    ResumenFila(
                        "Abonado",
                        payFormatMoney(if (mode == PaymentMode.PAID) totalNum else if (mode == PaymentMode.ARRIVAL) 0 else montoNum)
                    )
                    Spacer(Modifier.height(4.dp))
                    ResumenFila("Saldo pendiente", payFormatMoney(saldo))
                }
            }

            if (reservation.isPending) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Al guardar, la reserva pasa a CONFIRMADA y el servidor le manda " +
                        "al cliente el correo con su tiquete.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = if (reservation.isPending) "GUARDAR Y CONFIRMAR" else "GUARDAR PAGO",
                onClick = { onSave(mode, total, monto) },
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ResumenFila(label: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(valor, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Cambiar la fecha de una reserva y, si el plan tiene varias salidas,
 * también su horario.
 *
 * El horario importa: si el cliente se pasa a la salida de la tarde y solo
 * se cambiara el día, la reserva se quedaría con la hora vieja y el evento
 * de Google Calendar también. Cuando el plan no maneja horarios, el
 * desplegable no aparece y se conserva el que tenga.
 *
 * La fecha va con el selector de Compose (no el nativo del sistema), que
 * viaja dentro de la app y por tanto se ve igual en cualquier Android.
 */
@Composable
fun ChangeDateDialog(
    reservation: Reservation,
    /** Horarios del plan de esta reserva; vacío = el plan no maneja horarios */
    horarios: List<Horario>,
    isSaving: Boolean,
    onConfirm: (LocalDate, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var dateIso by remember {
        mutableStateOf(reservation.dayKey.ifBlank { LocalDate.now().toString() })
    }
    val fecha = remember(dateIso) { runCatching { LocalDate.parse(dateIso) }.getOrNull() }

    val disponibles = horarios.filter { it.inicio.isNotBlank() }
    // Arranca en el horario que ya tiene la reserva, si sigue existiendo
    var horarioValue by remember {
        val actual = disponibles.firstOrNull { it.inicio == reservation.horaInicio }
        mutableStateOf(if (actual != null) "${actual.inicio}|${actual.fin}" else "")
    }

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
                DatePickerField(
                    label = "Elegir la nueva fecha",
                    value = dateIso,
                    onPick = { dateIso = it },
                    enabled = !isSaving
                )
                if (disponibles.isNotEmpty()) {
                    AdminDropdown(
                        "Horario",
                        listOf("" to "— Sin horario —") + disponibles.map { h ->
                            val fin = if (h.fin.isBlank()) "" else " – ${fmtHora(h.fin)}"
                            "${h.inicio}|${h.fin}" to "${fmtHora(h.inicio)}$fin"
                        },
                        horarioValue,
                        { horarioValue = it },
                        enabled = !isSaving
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (disponibles.isEmpty())
                        "Se conserva la hora y se mueve también el evento de Google Calendar."
                    else
                        "El evento de Google Calendar se vuelve a crear con la fecha y la hora nuevas.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val f = fecha ?: return@TextButton
                    if (disponibles.isEmpty()) {
                        // Sin horarios: se conserva el de la reserva (null)
                        onConfirm(f, reservation.horaInicio.orEmpty(), reservation.horaFin.orEmpty())
                    } else {
                        val partes = horarioValue.split("|")
                        onConfirm(f, partes.getOrNull(0).orEmpty(), partes.getOrNull(1).orEmpty())
                    }
                },
                enabled = fecha != null && !isSaving
            ) {
                Text("Mover", color = if (fecha != null) BlueElectric else TextDim, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}
