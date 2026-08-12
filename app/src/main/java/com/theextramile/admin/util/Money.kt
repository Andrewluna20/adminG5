package com.theextramile.admin.util

import com.theextramile.admin.data.model.Reservation

/**
 * Dinero y estado de pago — espejo exacto de las funciones payParse/paymentInfo
 * de admin-js/reservations.js. Está aparte porque lo usan Reservas, Extracto,
 * Resumen y Calendario, igual que en el panel web.
 */

/**
 * payParse(): acepta número, "840.000", "$840.000" o null y devuelve un entero
 * no negativo. Se queda solo con los dígitos, como el panel.
 */
fun payParse(value: Any?): Int = when (value) {
    null -> 0
    is Int -> value.coerceAtLeast(0)
    is Long -> value.coerceAtLeast(0L).toInt()
    is Double -> Math.round(value).toInt().coerceAtLeast(0)
    is Number -> Math.round(value.toDouble()).toInt().coerceAtLeast(0)
    else -> value.toString().filter { it.isDigit() }.let { if (it.isEmpty()) 0 else it.toIntOrNull() ?: 0 }
}

/** payFormat(): 840000 → "840.000" (vacío si es 0, como en los inputs del panel) */
fun payFormat(value: Any?): String {
    val n = payParse(value)
    return if (n == 0) "" else groupThousands(n)
}

/** payFormatMoney(): 840000 → "$840.000" */
fun payFormatMoney(value: Any?): String = "$" + groupThousands(payParse(value))

/** Separador de miles con punto (es-CO) */
fun groupThousands(n: Int): String =
    n.toString().reversed().chunked(3).joinToString(".").reversed()

/** Estado de pago de una reserva */
enum class PaymentStatus(val key: String, val label: String) {
    PAID("paid", "Pagada"),
    PARTIAL("partial", "Con saldo pendiente"),
    ARRIVAL("arrival", "Paga al llegar"),
    NONE("none", "Sin registro de pago");

    companion object {
        fun from(key: String?): PaymentStatus? = entries.firstOrNull { it.key == key }
    }
}

data class PaymentInfo(
    val total: Int,
    val deposit: Int,
    val balance: Int,
    val status: PaymentStatus
)

/**
 * paymentInfo(): total, abonado, saldo y estado de una reserva.
 *
 * Reproduce el fallback del panel: las reservas que entran por la web no
 * siempre guardan `total`, así que se calcula precio unitario × pasajeros —
 * si no, el total sale en blanco y el saldo por cobrar queda mal.
 */
fun paymentInfo(r: Reservation): PaymentInfo {
    var total = payParse(r.total)
    if (total == 0) {
        val unit = payParse(r.unitPrice)
        val pax = r.pax
        if (unit > 0 && pax > 0) total = unit * pax
    }

    val deposit = payParse(r.deposit)
    val balance = if (r.balance != null) payParse(r.balance) else (total - deposit).coerceAtLeast(0)

    val declared = PaymentStatus.from(r.paymentStatus)
    val status = declared ?: run {
        // Reserva sin ningún registro de pago
        val sinRegistro = payParse(r.total) == 0 && payParse(r.deposit) == 0 && r.paymentStatus.isNullOrBlank()
        when {
            sinRegistro -> PaymentStatus.NONE
            balance > 0 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.PAID
        }
    }

    return PaymentInfo(total, deposit, balance, status)
}

/**
 * paymentSummaryLine(): línea lista para plantillas (evento de calendario,
 * WhatsApp…). Cadena vacía si la reserva no tiene registro de pago.
 */
fun paymentSummaryLine(r: Reservation): String {
    val pi = paymentInfo(r)
    return when (pi.status) {
        PaymentStatus.PAID -> "Pagado: ${payFormatMoney(pi.total)}"
        PaymentStatus.PARTIAL -> "Abono: ${payFormatMoney(pi.deposit)} · Saldo: ${payFormatMoney(pi.balance)}"
        PaymentStatus.ARRIVAL -> "Paga al llegar: ${payFormatMoney(if (pi.balance > 0) pi.balance else pi.total)}"
        PaymentStatus.NONE -> ""
    }
}
