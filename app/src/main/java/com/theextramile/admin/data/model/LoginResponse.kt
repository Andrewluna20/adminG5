package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint POST /usuarios.php?action=login
 *
 * Ejemplo:
 * {
 *   "token": "tk_abc123...",
 *   "user": { "id": "admin-1", "name": "Admin", ... },
 *   "expires_in": 2592000
 * }
 */
data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User,
    @SerializedName("expires_in") val expiresIn: Long = 2592000,  // 30 días
    /**
     * El servidor pide que cree su contraseña definitiva antes de usar el
     * panel. Se mira junto con `user.temp_password`, igual que adminLogin()
     * en admin-js/auth.js:48 — según el caso viene uno u otro.
     */
    @SerializedName("must_change_password") val mustChangePassword: Boolean = false
)

/**
 * Respuesta del endpoint GET /usuarios.php?action=verify
 */
data class VerifyResponse(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("user") val user: User?,
    @SerializedName("error") val error: String?
)
