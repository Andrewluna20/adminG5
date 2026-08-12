package com.theextramile.admin.data.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.theextramile.admin.data.api.FlexibleIntAdapter

/**
 * Una reserva (data/reservations.json).
 *
 * Estructura completa del panel web. Los campos que el sitio público no
 * siempre escribe llevan valor por defecto para que Gson no falle con las
 * reservas antiguas.
 *
 * status: "pending" | "confirmed" | "cancelled"
 * paymentStatus: "paid" | "partial" | "arrival" | ausente
 *
 * Ojo con el dinero: no leas `total` directamente, usa paymentInfo() de
 * util/Money.kt — reproduce el fallback precio×pax del panel.
 */
data class Reservation(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String? = null,
    @SerializedName("tourId") val tourId: String = "",
    @SerializedName("tourTitle") val tourTitle: String = "",
    @SerializedName("pax") val pax: Int = 1,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("source") val source: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,

    // ── Fecha y horario ──
    /** Fecha ya formateada en español que arma el sitio público */
    @SerializedName("date") val date: String = "",
    /** ISO 8601 — la que se usa para ordenar y filtrar */
    @SerializedName("dateRaw") val dateRaw: String? = null,
    @SerializedName("horaInicio") val horaInicio: String? = null,
    @SerializedName("horaFin") val horaFin: String? = null,

    // ── Dinero ──
    // El servidor guarda unas veces número y otras "840.000"; el adaptador
    // acepta las dos formas y al guardar escribe siempre un entero (si se
    // escribiera 840000.0, el backend lo leería como 8.400.000).
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("total") val total: Int? = null,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("deposit") val deposit: Int? = null,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("balance") val balance: Int? = null,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("unitPrice") val unitPrice: Int? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("discount") val discount: ReservationDiscount? = null,

    // ── Confirmación ──
    @SerializedName("confirmedAt") val confirmedAt: String? = null,
    @SerializedName("confirmedBy") val confirmedBy: ConfirmedBy? = null,
    @SerializedName("receivedEmailSent") val receivedEmailSent: Boolean = false,
    @SerializedName("confirmationEmailSent") val confirmationEmailSent: Boolean = false,

    // ── Google Calendar ──
    @SerializedName("gcal_event_id") val gcalEventId: String? = null,
    @SerializedName("gcal_account") val gcalAccount: String? = null,
    @SerializedName("gcal_calendar_id") val gcalCalendarId: String? = null,

    // ── Políticas aceptadas por el cliente ──
    @SerializedName("acceptedTerms") val acceptedTerms: Boolean = false,
    @SerializedName("acceptedRefund") val acceptedRefund: Boolean = false,
    @SerializedName("acceptedAt") val acceptedAt: String? = null,
    @SerializedName("acceptedFromIp") val acceptedFromIp: String? = null,

    // ── Reservas en grupo (varios planes en un mismo checkout) ──
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("groupIndex") val groupIndex: Int = 0,
    @SerializedName("groupSize") val groupSize: Int = 0,

    // ── Pasarela Bold ──
    @SerializedName("boldOrderId") val boldOrderId: String? = null,
    @SerializedName("boldPaidAt") val boldPaidAt: String? = null,
    @SerializedName("boldPagoIncompleto") val boldPagoIncompleto: Boolean = false,
    @SerializedName("boldPagandoDesde") val boldPagandoDesde: String? = null,

    // ── Vendedor que trajo la reserva (sellerStamp del backend) ──
    @SerializedName("soldBy") val soldBy: SoldBy? = null
) {
    val isPending: Boolean get() = status == "pending"
    val isConfirmed: Boolean get() = status == "confirmed"
    val isCancelled: Boolean get() = status == "cancelled"

    val statusDisplay: String get() = when (status) {
        "pending" -> "Pendiente"
        "confirmed" -> "Confirmada"
        "cancelled" -> "Cancelada"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    /** Parte de un checkout con varios planes */
    val isGroup: Boolean get() = !groupId.isNullOrBlank() && groupSize > 1

    /** "2 de 3" para las reservas en grupo */
    val groupLabel: String? get() = if (isGroup) "${groupIndex + 1} de $groupSize" else null

    val shortId: String get() = id.takeLast(6).uppercase()

    /** Millis de dateRaw (fecha del plan) para ordenar y agrupar por día */
    val dateMillis: Long get() = parseIsoMillis(dateRaw)

    /** Millis de createdAt (cuándo se reservó) */
    val createdAtMillis: Long get() = parseIsoMillis(createdAt)

    /** "yyyy-MM-dd" local de la fecha del plan — clave del calendario */
    val dayKey: String get() = isoToDayKey(dateRaw)

    private fun parseIsoMillis(iso: String?): Long = try {
        if (iso.isNullOrBlank()) 0L else java.time.Instant.parse(iso).toEpochMilli()
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(iso!!.take(19))
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e2: Exception) {
            iso?.toLongOrNull() ?: 0L
        }
    }
}

/** Descuento tal y como queda congelado dentro de la reserva */
data class ReservationDiscount(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("value") val value: Int = 0,
    /** Pesos que se descontaron en total */
    @SerializedName("off") val off: Int = 0,
    /** Precio por persona antes del descuento */
    @SerializedName("unitBefore") val unitBefore: Int = 0,
    /** Total ya con el descuento aplicado */
    @SerializedName("total") val total: Int = 0
)

/** Quién confirmó la reserva (un admin, o "Bold" si la confirmó la pasarela) */
data class ConfirmedBy(
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = ""
)

/**
 * Vendedor que trajo la reserva. Es una foto del momento (sellerStamp del
 * backend): si luego se renombra o se borra al vendedor, la reserva conserva
 * el nombre con el que entró.
 */
data class SoldBy(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = ""
)

/** "2026-08-12" en hora local a partir de un ISO — mismo criterio que extYmd() */
fun isoToDayKey(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.Instant.parse(iso)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate().toString()
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(iso.take(19)).toLocalDate().toString()
        } catch (e2: Exception) {
            iso.take(10)
        }
    }
}
