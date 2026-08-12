package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.SettingsRequest
import com.theextramile.admin.data.model.SiteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class SettingsRepository {

    private val _settings = MutableStateFlow(SiteSettings())
    val settings: StateFlow<SiteSettings> = _settings.asStateFlow()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        object NoConnection : Result<Nothing>()
        data class Error(val message: String) : Result<Nothing>()
    }

    suspend fun refresh(): Result<SiteSettings> {
        return try {
            val response = ApiClient.service.getSettings()
            if (response.isSuccessful) {
                val data = response.body() ?: SiteSettings()
                _settings.value = data
                Result.Success(data)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun save(newSettings: SiteSettings): Result<Unit> {
        return try {
            val response = ApiClient.service.saveSettings(
                request = SettingsRequest(newSettings)
            )
            if (response.isSuccessful) {
                _settings.value = newSettings
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    /**
     * Guarda solo unos campos sin pisar el resto.
     *
     * saveSettings reemplaza el objeto entero en el servidor, así que las
     * pantallas que tocan una sub-sección (SEO, Colores, Bold…) parten SIEMPRE
     * del settings ya cargado y le aplican su cambio con copy(). Si el
     * settings todavía no se ha cargado, primero se trae: guardar el objeto
     * vacío por defecto borraría la configuración del sitio entero.
     */
    suspend fun patch(transform: (SiteSettings) -> SiteSettings): Result<Unit> {
        if (_settings.value == SiteSettings()) {
            when (val loaded = refresh()) {
                is Result.Error -> return loaded
                Result.NoConnection -> return Result.NoConnection
                is Result.Success -> Unit
            }
        }
        return save(transform(_settings.value))
    }

    /** Llave secreta de Bold — va aparte, el servidor nunca la devuelve */
    suspend fun saveBoldSecret(secret: String): ApiResult<Unit> =
        apiAction {
            ApiClient.service.saveBoldSecret(
                request = com.theextramile.admin.data.api.SecretRequest(secret)
            )
        }

    /** Llave secreta de reCAPTCHA — igual que la de Bold */
    suspend fun saveRecaptchaSecret(secret: String): ApiResult<Unit> =
        apiAction {
            ApiClient.service.saveRecaptchaSecret(
                request = com.theextramile.admin.data.api.SecretRequest(secret)
            )
        }

    /** Reconstruye index.html y sitemap.xml del sitio público */
    suspend fun regenerateIndex(): ApiResult<Unit> =
        apiAction { ApiClient.service.regenerateIndex() }

    /** Manda un correo de confirmación de prueba a una dirección */
    suspend fun sendTestEmail(to: String): ApiResult<Unit> =
        when (val r = apiCall { ApiClient.service.sendTestEmail(to = to) }) {
            is ApiResult.Success ->
                if (r.data.sent) ApiResult.Success(Unit)
                else ApiResult.Error(r.data.error ?: "No se pudo enviar el correo")
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }

    /** Sube una imagen (logo, favicon, etc) */
    suspend fun uploadImage(file: File, type: String): Result<String> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestFile = RequestBody.create(mediaType, file)
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val typeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), type)
            val response = ApiClient.service.uploadImage(imagePart, typeBody)
            if (response.isSuccessful && response.body()?.url != null) {
                Result.Success(response.body()!!.url!!)
            } else Result.Error(response.body()?.error ?: "Error al subir imagen")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }
}
