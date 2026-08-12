package com.theextramile.admin.data.repository

import android.content.Context
import android.net.Uri
import com.theextramile.admin.data.api.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Subida de archivos a upload.php, compartida por todas las pantallas.
 *
 * upload.php solo acepta 5 carpetas (ver UploadType). Las imágenes de muelles
 * y beneficios se suben como TOURS, exactamente igual que el panel web —
 * cualquier otro valor lo rechaza el servidor.
 *
 * El servidor convierte a WebP las de tipo tours/blog/hero (si son imágenes) y
 * devuelve `optimized = true`; el MP4 del hero pasa tal cual.
 */
class UploadRepository(private val context: Context) {

    suspend fun upload(uri: Uri, type: String = UploadType.TOURS): ApiResult<String> {
        val temp = try {
            copyToCache(uri)
        } catch (e: IOException) {
            return ApiResult.Error("No se pudo leer el archivo elegido")
        } ?: return ApiResult.Error("No se pudo leer el archivo elegido")

        return try {
            uploadFile(temp, type)
        } finally {
            temp.delete()
        }
    }

    suspend fun uploadFile(file: File, type: String = UploadType.TOURS): ApiResult<String> {
        val mime = guessMime(file.name)
        val part = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody(mime.toMediaTypeOrNull())
        )
        val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())

        return when (val r = apiCall { ApiClient.service.uploadImage(part, typeBody) }) {
            is ApiResult.Success -> {
                val url = r.data.url
                if (!url.isNullOrBlank()) ApiResult.Success(url)
                else ApiResult.Error(r.data.error ?: "El servidor no devolvió la URL")
            }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }
    }

    /**
     * Retrofit necesita un File real, y un Uri del selector de Android no lo
     * es (puede venir de Drive, de Fotos…). Se copia a la caché y se borra al
     * terminar.
     */
    private fun copyToCache(uri: Uri): File? {
        val name = queryName(uri) ?: "upload_${System.currentTimeMillis()}"
        val out = File(context.cacheDir, "up_${System.currentTimeMillis()}_$name")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        return out
    }

    private fun queryName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "svg" -> "image/svg+xml"
        "ico" -> "image/x-icon"
        else -> "image/jpeg"
    }
}

/** Las 5 carpetas que acepta upload.php ($allowedTypes) */
object UploadType {
    /** Planes; también muelles y beneficios. Se optimiza a WebP. */
    const val TOURS = "tours"

    /** Portada: imagen, póster y el MP4 (el vídeo no se optimiza) */
    const val HERO = "hero"

    /** Logos del sitio y de la empresa. NO se optimiza. */
    const val BRAND = "brand"

    /** Favicon. NO se optimiza. */
    const val FAVICON = "favicon"

    /** Portadas del blog y la imagen social de SEO. Se optimiza a WebP. */
    const val BLOG = "blog"
}
