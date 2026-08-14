package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cuenta de Google Calendar vinculada.
 */
data class GoogleCalendarAccount(
    @SerializedName("email") val email: String,
    @SerializedName("added_at") val addedAt: Long = 0,
    @SerializedName("has_refresh_token") val hasRefreshToken: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = false,
    @SerializedName("token_expires_at") val tokenExpiresAt: Long = 0
)

data class GcalListResponse(
    @SerializedName("accounts") val accounts: List<GoogleCalendarAccount> = emptyList(),
    @SerializedName("active_email") val activeEmail: String? = null
)

data class GcalConfigStatus(
    @SerializedName("configured") val configured: Boolean = false,
    @SerializedName("redirect_uri") val redirectUri: String? = null
)

/**
 * Calendarios de cada cuenta vinculada (gcal.php?action=list_calendars).
 *
 * Lo usa el editor de planes: cada plan puede mandar sus reservas
 * confirmadas a un calendario concreto. Listar esto llama a Google una vez
 * por cuenta, así que el ViewModel lo pide una sola vez y lo guarda.
 *
 * Si una cuenta perdió el permiso, viene con `error` y sin calendarios.
 */
data class GcalCalendarsResponse(
    @SerializedName("accounts") val accounts: List<GcalAccountCalendars> = emptyList()
)

data class GcalAccountCalendars(
    @SerializedName("email") val email: String = "",
    @SerializedName("is_active") val isActive: Boolean = false,
    /** Mensaje de Google si la cuenta hay que reconectarla */
    @SerializedName("error") val error: String? = null,
    @SerializedName("calendars") val calendars: List<GcalCalendar> = emptyList()
)

data class GcalCalendar(
    @SerializedName("id") val id: String = "",
    @SerializedName("summary") val summary: String = "",
    @SerializedName("primary") val primary: Boolean = false
)
