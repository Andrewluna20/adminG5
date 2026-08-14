package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.ChangeOwnPasswordRequest
import com.theextramile.admin.data.api.ErrorResponse
import com.theextramile.admin.data.api.LoginRequest
import com.theextramile.admin.data.local.SessionManager
import com.theextramile.admin.data.model.User
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Maneja toda la lógica de autenticación.
 *
 * - login(): valida credenciales, guarda token + user
 * - logout(): notifica al servidor + borra sesión local
 * - verifySession(): valida que el token sigue activo
 */
class AuthRepository(private val sessionManager: SessionManager) {

    sealed class LoginResult {
        data class Success(
            val user: User,
            /** Entró con la contraseña temporal: no puede usar el panel hasta cambiarla */
            val mustChangePassword: Boolean = false
        ) : LoginResult()
        object InvalidCredentials : LoginResult()
        object NoConnection : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    suspend fun login(email: String, password: String): LoginResult {
        return try {
            val response = ApiClient.service.login(
                credentials = LoginRequest(email.trim(), password)
            )
            when {
                response.isSuccessful && response.body() != null -> {
                    val body = response.body()!!
                    // La sesión se guarda igual aunque toque cambiar la
                    // contraseña: change_own_password EXIGE estar dentro.
                    sessionManager.saveSession(body.user, body.token)
                    LoginResult.Success(
                        user = body.user,
                        // Según el caso el servidor manda uno u otro
                        mustChangePassword = body.mustChangePassword || body.user.tempPassword
                    )
                }
                response.code() == 401 -> LoginResult.InvalidCredentials
                else -> {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        ApiClient.gson.fromJson(errorBody, ErrorResponse::class.java).error
                    } catch (e: Exception) { null }
                    LoginResult.Error(errorMessage ?: "Error ${response.code()}")
                }
            }
        } catch (e: IOException) {
            LoginResult.NoConnection
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * El usuario guardado si todavía arrastra la contraseña temporal.
     *
     * Sirve para que, al reabrir la app, el login vuelva a poner delante el
     * cambio obligatorio en vez de dejar pasar.
     */
    suspend fun pendingPasswordChange(): User? =
        sessionManager.currentUser.first()?.takeIf { it.tempPassword }

    /**
     * Cambia la contraseña del usuario que está dentro.
     *
     * Exige sesión (por eso el login la guarda antes de saber si toca
     * cambiarla). Al terminar bien, el usuario deja de tener contraseña
     * temporal, así que se refresca el dato guardado.
     */
    suspend fun changeOwnPassword(newPassword: String): ApiResult<Unit> {
        val r = apiAction {
            ApiClient.service.changeOwnPassword(
                request = ChangeOwnPasswordRequest(newPassword)
            )
        }
        if (r is ApiResult.Success) verifySession()
        return r
    }

    suspend fun logout() {
        // 1) Avisar al servidor (best effort, no falla si está offline)
        try { ApiClient.service.logout() } catch (_: Exception) { }
        // 2) Borrar sesión local siempre
        sessionManager.logout()
    }

    /**
     * Verifica que el token guardado siga siendo válido.
     * Útil al abrir la app: si el token expiró, redirigir a login.
     */
    suspend fun verifySession(): Boolean {
        return try {
            val response = ApiClient.service.verify()
            if (response.isSuccessful && response.body()?.valid == true) {
                // Actualizar datos del usuario por si cambiaron
                response.body()?.user?.let { user ->
                    val currentToken = sessionManager.getTokenBlocking()
                    if (currentToken != null) {
                        sessionManager.saveSession(user, currentToken)
                    }
                }
                true
            } else {
                // Token inválido → forzar logout local
                sessionManager.logout()
                false
            }
        } catch (e: IOException) {
            // Sin internet → asumimos que la sesión sigue válida (offline-friendly)
            true
        } catch (e: Exception) {
            false
        }
    }
}
