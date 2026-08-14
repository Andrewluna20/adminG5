package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.TourImage
import com.theextramile.admin.data.api.ToursRequest
import com.theextramile.admin.data.api.UploadResponse
import com.theextramile.admin.data.model.Tour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.IOException

class TourRepository {

    private val _tours = MutableStateFlow<List<Tour>>(emptyList())
    val tours: StateFlow<List<Tour>> = _tours.asStateFlow()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        object NoConnection : Result<Nothing>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Usa getToursAdmin, no getTours: la del panel trae también los planes
     * ocultos y el precio neto. El backend solo devuelve priceNet al Super
     * Admin, así que para los demás roles llega vacío igual que en la web.
     */
    suspend fun refresh(): Result<List<Tour>> {
        return try {
            val response = ApiClient.service.getToursAdmin()
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                _tours.value = list
                Result.Success(list)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun saveTour(tour: Tour, isNew: Boolean): Result<Unit> {
        val current = _tours.value.toMutableList()
        if (isNew) {
            current.add(tour)
        } else {
            val idx = current.indexOfFirst { it.id == tour.id }
            if (idx >= 0) current[idx] = tour else current.add(tour)
        }
        return saveAll(current)
    }

    suspend fun deleteTour(tourId: String): Result<Unit> {
        val updated = _tours.value.filterNot { it.id == tourId }
        return saveAll(updated)
    }

    suspend fun toggleActive(tourId: String): Result<Unit> {
        val updated = _tours.value.map {
            if (it.id == tourId) it.copy(active = !it.active) else it
        }
        return saveAll(updated)
    }

    private suspend fun saveAll(list: List<Tour>): Result<Unit> {
        return try {
            val response = ApiClient.service.saveTours(request = ToursRequest(list))
            if (response.isSuccessful) {
                _tours.value = list
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    /**
     * Imágenes que ya están subidas al servidor, para poder reutilizarlas
     * en otro plan sin volver a subirlas (y sin duplicar el archivo).
     */
    suspend fun listImages(): Result<List<TourImage>> {
        return try {
            val response = ApiClient.service.listTourImages()
            if (response.isSuccessful) Result.Success(response.body() ?: emptyList())
            else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    /** Sube una imagen al servidor y devuelve la URL pública */
    suspend fun uploadImage(file: File, type: String = "tours"): Result<String> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestFile = RequestBody.create(mediaType, file)
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val typeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), type)

            val response = ApiClient.service.uploadImage(imagePart, typeBody)
            if (response.isSuccessful && response.body()?.url != null) {
                Result.Success(response.body()!!.url!!)
            } else {
                Result.Error(response.body()?.error ?: "Error al subir imagen")
            }
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }
}
