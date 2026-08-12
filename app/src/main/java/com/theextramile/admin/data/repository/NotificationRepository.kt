package com.theextramile.admin.data.repository

import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.local.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Registra el token FCM del dispositivo en el servidor.
 * El servidor usa este token para enviar push cuando entra una reserva.
 */
class NotificationRepository(private val sessionManager: SessionManager) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Registra el token FCM del dispositivo */
    suspend fun registerToken(token: String): Boolean {
        val authToken = sessionManager.getTokenBlocking() ?: return false
        return try {
            val payload = JSONObject().apply {
                put("token", token)
                put("device", android.os.Build.MODEL)
                put("os", "android")
            }
            val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}notifications.php?action=register_token")
                .header("Authorization", "Bearer $authToken")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            ok
        } catch (e: Exception) {
            false
        }
    }

    /** Desregistra el token al cerrar sesión */
    suspend fun unregisterToken(token: String): Boolean {
        val authToken = sessionManager.getTokenBlocking() ?: return false
        return try {
            val payload = JSONObject().apply { put("token", token) }
            val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}notifications.php?action=unregister_token")
                .header("Authorization", "Bearer $authToken")
                .post(body)
                .build()
            val response = httpClient.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            ok
        } catch (e: Exception) {
            false
        }
    }
}
