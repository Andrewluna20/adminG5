package com.theextramile.admin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.theextramile.admin.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "tem_session")

/**
 * Guarda y recupera la sesión del usuario:
 *  - Token de autenticación
 *  - Datos del usuario logueado
 *
 * El token se obtiene de forma síncrona en el AuthInterceptor para añadirlo
 * automáticamente a cada petición HTTP.
 */
class SessionManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val KEY_USER = stringPreferencesKey("logged_user")
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
    }

    /** Flow del usuario actual (null si no hay sesión) */
    val currentUser: Flow<User?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER]?.let { json ->
            try { gson.fromJson(json, User::class.java) } catch (e: Exception) { null }
        }
    }

    /** Flow del token actual (null si no hay sesión) */
    val currentToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    suspend fun isLoggedIn(): Boolean {
        return currentToken.first() != null && currentUser.first() != null
    }

    suspend fun saveSession(user: User, token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER] = gson.toJson(user)
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER)
            prefs.remove(KEY_TOKEN)
        }
    }

    /**
     * Lee el token de forma síncrona. SOLO debe usarse desde el AuthInterceptor
     * porque OkHttp es síncrono. Es seguro porque DataStore tiene caché interno.
     */
    fun getTokenBlocking(): String? {
        return try {
            runBlocking { currentToken.first() }
        } catch (e: Exception) {
            null
        }
    }
}
