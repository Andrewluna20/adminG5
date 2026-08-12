package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Una línea del historial de actividad del panel (data/activity_log.json).
 * Solo lectura: lo escribe el servidor con appendActivity().
 *
 * El backend ya devuelve la lista invertida (más recientes primero) y
 * recortada a 500 registros.
 */
data class ActivityEntry(
    /** ISO 8601 */
    @SerializedName("at") val at: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("userName") val userName: String = "",
    @SerializedName("userEmail") val userEmail: String = "",
    @SerializedName("action") val action: String = "",
    @SerializedName("summary") val summary: String = "",
    @SerializedName("refId") val refId: String = ""
) {
    val displayUser: String get() = userName.ifBlank { "Desconocido" }

    val meta: ActivityMeta get() = ACTIVITY_META[action] ?: ActivityMeta(action.ifBlank { "—" }, ActivityTone.NEUTRAL)
}

/** Respuesta de data.php?action=getActivityLog */
data class ActivityLogResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("log") val log: List<ActivityEntry> = emptyList()
)

enum class ActivityTone { RESERVA, CANCEL, WARN, PLAN, CONFIG, NEUTRAL }

data class ActivityMeta(val label: String, val tone: ActivityTone)

/** Espejo de ACT_META en admin-js/activity.js */
val ACTIVITY_META: Map<String, ActivityMeta> = mapOf(
    "reserva_confirmada" to ActivityMeta("Confirmó reserva", ActivityTone.RESERVA),
    "reserva_cancelada" to ActivityMeta("Canceló reserva", ActivityTone.CANCEL),
    "reserva_restaurada" to ActivityMeta("Restauró reserva", ActivityTone.WARN),
    "reserva_pendiente" to ActivityMeta("Devolvió a pendiente", ActivityTone.WARN),
    "reserva_pago" to ActivityMeta("Actualizó pago", ActivityTone.WARN),
    "reserva_fecha" to ActivityMeta("Cambió fecha", ActivityTone.WARN),
    "plan_creado" to ActivityMeta("Creó plan", ActivityTone.PLAN),
    "plan_editado" to ActivityMeta("Editó plan", ActivityTone.PLAN),
    "plan_eliminado" to ActivityMeta("Eliminó plan", ActivityTone.CANCEL),
    "vendedor_creado" to ActivityMeta("Creó vendedor", ActivityTone.PLAN),
    "vendedor_eliminado" to ActivityMeta("Eliminó vendedor", ActivityTone.CANCEL),
    "descuento_guardado" to ActivityMeta("Descuentos", ActivityTone.PLAN),
    "beneficio_guardado" to ActivityMeta("Beneficios", ActivityTone.PLAN),
    "config_guardada" to ActivityMeta("Configuración", ActivityTone.CONFIG)
)

/** Tipos que ofrece el desplegable de filtro, en el orden del panel web */
val ACTIVITY_FILTER_TYPES: List<Pair<String, String>> =
    listOf("" to "Todas las acciones") + ACTIVITY_META.map { it.key to it.value.label }
