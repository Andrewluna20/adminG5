package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String  // "super" | "reservations" | "viewer"
) {
    val isSuperAdmin: Boolean get() = role == "super"
    val isReservationsManager: Boolean get() = role == "reservations"
    val isViewer: Boolean get() = role == "viewer"

    val roleDisplay: String get() = when (role) {
        "super" -> "Super Admin"
        "reservations" -> "Gestor de Reservas"
        "viewer" -> "Solo Ver"
        else -> role
    }
}
