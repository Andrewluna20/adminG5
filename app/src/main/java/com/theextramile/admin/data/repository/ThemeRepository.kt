package com.theextramile.admin.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.theextramile.admin.BuildConfig
import com.theextramile.admin.data.model.RemoteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga el tema visual remoto desde el HUB (gpanelcol.online).
 *
 * Estrategia:
 *  1) Al inicio: lee del caché local (instantáneo, funciona offline)
 *  2) En paralelo: descarga del servidor para refrescar
 *  3) Si descarga ok: actualiza caché + StateFlow
 *
 * URL del archivo en el servidor:
 *   https://gpanelcol.online/data/mobile_theme.json
 */
class ThemeRepository(private val context: Context) {

    companion object {
        private const val TAG = "ThemeRepo"
        private const val THEME_URL = "${BuildConfig.HUB_BASE_URL}data/mobile_theme.json"
        private const val CACHE_FILE = "mobile_theme_cache.json"
    }

    private val gson = Gson()

    private val _theme = MutableStateFlow(RemoteTheme())
    val theme: StateFlow<RemoteTheme> = _theme.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Carga inicial: primero del caché (rápido), luego del servidor.
     */
    suspend fun loadTheme() {
        loadFromCache()?.let { cached ->
            _theme.value = cached
            Log.d(TAG, "Tema cargado del caché v${cached.version}")
        }
        refreshFromServer()
    }

    /**
     * Solo del caché local (offline).
     */
    private suspend fun loadFromCache(): RemoteTheme? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            if (!file.exists()) return@withContext null
            val text = file.readText()
            gson.fromJson(text, RemoteTheme::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo caché del tema: ${e.message}")
            null
        }
    }

    /**
     * Descarga del servidor y actualiza el StateFlow + caché.
     */
    suspend fun refreshFromServer(): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val conn = (URL(THEME_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Servidor respondió ${conn.responseCode}")
                return@withContext false
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val newTheme = gson.fromJson(text, RemoteTheme::class.java)
            _theme.value = newTheme
            saveToCache(text)
            Log.d(TAG, "Tema refrescado del servidor v${newTheme.version}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando tema: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    private fun saveToCache(jsonText: String) {
        try {
            File(context.filesDir, CACHE_FILE).writeText(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando caché del tema: ${e.message}")
        }
    }
}
