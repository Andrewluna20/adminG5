package com.theextramile.admin.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/* ═══════════════════════════════════════════════════════
   Fechas y horas tal como las escribe el panel web.

   Estos textos NO son de adorno: el campo `date` de una reserva es lo
   que se le muestra al cliente en el correo y en la factura, así que
   tiene que salir igual desde la app y desde el panel. Espejo de
   MONTHS (admin-js/core.js:344) y fmtHora (admin-js/tours.js:524).
   ═══════════════════════════════════════════════════════ */

/**
 * Los meses EN MAYÚSCULA inicial, como el array MONTHS del panel.
 *
 * No se usa DateTimeFormatter con locale es-CO porque ese da el mes en
 * minúscula ("julio") según la versión de Android, y entonces la misma
 * reserva se vería escrita de dos maneras distintas según quién la tocó.
 */
private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** "25 de Julio de 2026" */
fun formatFechaEspanol(date: LocalDate): String =
    "${date.dayOfMonth} de ${MESES[date.monthValue - 1]} de ${date.year}"

/**
 * "14:30" → "2:30 PM". Si no reconoce la hora la devuelve tal cual,
 * igual que fmtHora en el panel.
 */
fun fmtHora(hhmm: String?): String {
    val m = Regex("""^(\d{1,2}):(\d{2})""").find(hhmm.orEmpty()) ?: return hhmm.orEmpty()
    var h = m.groupValues[1].toInt()
    val min = m.groupValues[2]
    val ap = if (h < 12) "AM" else "PM"
    h %= 12
    if (h == 0) h = 12
    return "$h:$min $ap"
}

/** "25 de Julio de 2026 · 2:30 PM" — sin la hora si el plan no tiene horarios */
fun formatFechaConHora(date: LocalDate, horaInicio: String?): String {
    val base = formatFechaEspanol(date)
    return if (horaInicio.isNullOrBlank()) base else "$base · ${fmtHora(horaInicio)}"
}

/**
 * El `dateRaw` que espera el servidor: ISO 8601 del **mediodía local**
 * del día elegido.
 *
 * ⚠️ La hora de la salida NO va aquí, va en horaInicio/horaFin. Poner la
 * hora real en dateRaw parece más correcto pero rompe el evento de
 * Google Calendar, porque el servidor lo recrea leyendo horaInicio. El
 * mediodía es solo un ancla que evita que el día se corra al pasar a UTC.
 */
fun isoMediodia(date: LocalDate): String =
    date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toString()
