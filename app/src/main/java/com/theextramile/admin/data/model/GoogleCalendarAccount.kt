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
