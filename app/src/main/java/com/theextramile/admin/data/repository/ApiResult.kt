package com.theextramile.admin.data.repository

import retrofit2.Response
import java.io.IOException

/**
 * Resultado compartido por los repositorios nuevos.
 *
 * Los repositorios de la primera versión (Reservation, Tour, Settings, User,
 * GoogleCalendar) llevan su propio `Result` anidado; se han dejado como
 * estaban para no tocar sus pantallas, que ya funcionan.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object NoConnection : ApiResult<Nothing>()
    data class Error(val message: String) : ApiResult<Nothing>()

    val dataOrNull: T? get() = (this as? Success)?.data

    val errorMessage: String?
        get() = when (this) {
            is Error -> message
            NoConnection -> "Sin conexión"
            is Success -> null
        }
}

/**
 * Envuelve una llamada de Retrofit: distingue "no hay internet" de "el
 * servidor respondió mal", y saca el mensaje de error del backend cuando lo
 * manda en el cuerpo (data.php devuelve {"error": "..."} con código 4xx).
 */
suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> = try {
    val response = block()
    val body = response.body()
    if (response.isSuccessful && body != null) {
        ApiResult.Success(body)
    } else {
        ApiResult.Error(extractError(response))
    }
} catch (e: IOException) {
    ApiResult.NoConnection
} catch (e: Exception) {
    ApiResult.Error(e.message ?: "Error desconocido")
}

/**
 * Igual que apiCall pero para las acciones que devuelven {"success": bool}:
 * un 200 con success=false también es un fallo.
 */
suspend fun apiAction(block: suspend () -> Response<com.theextramile.admin.data.api.SuccessResponse>): ApiResult<Unit> =
    when (val r = apiCall(block)) {
        is ApiResult.Success ->
            if (r.data.success) ApiResult.Success(Unit)
            else ApiResult.Error(r.data.error ?: "No se pudo guardar")
        is ApiResult.Error -> r
        ApiResult.NoConnection -> ApiResult.NoConnection
    }

private fun extractError(response: Response<*>): String {
    val raw = try {
        response.errorBody()?.string().orEmpty()
    } catch (e: Exception) {
        ""
    }
    // {"error":"no autorizado"} → no autorizado
    val fromJson = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1)
    return when {
        !fromJson.isNullOrBlank() -> fromJson
        response.code() == 401 || response.code() == 403 -> "No tienes permiso para esta acción"
        response.code() == 404 -> "No se encontró"
        else -> "Error ${response.code()}"
    }
}
