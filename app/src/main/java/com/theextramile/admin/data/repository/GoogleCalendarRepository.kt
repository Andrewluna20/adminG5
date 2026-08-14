package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.GcalEmailRequest
import com.theextramile.admin.data.model.GcalAccountCalendars
import com.theextramile.admin.data.model.GcalConfigStatus
import com.theextramile.admin.data.model.GoogleCalendarAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class GoogleCalendarRepository {

    private val _accounts = MutableStateFlow<List<GoogleCalendarAccount>>(emptyList())
    val accounts: StateFlow<List<GoogleCalendarAccount>> = _accounts.asStateFlow()

    private val _activeEmail = MutableStateFlow<String?>(null)
    val activeEmail: StateFlow<String?> = _activeEmail.asStateFlow()

    private val _configStatus = MutableStateFlow(GcalConfigStatus())
    val configStatus: StateFlow<GcalConfigStatus> = _configStatus.asStateFlow()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        object NoConnection : Result<Nothing>()
        data class Error(val message: String) : Result<Nothing>()
    }

    suspend fun checkConfig(): Result<GcalConfigStatus> {
        return try {
            val response = ApiClient.service.gcalConfigStatus()
            if (response.isSuccessful) {
                val data = response.body() ?: GcalConfigStatus()
                _configStatus.value = data
                Result.Success(data)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun refresh(): Result<List<GoogleCalendarAccount>> {
        return try {
            val response = ApiClient.service.gcalList()
            if (response.isSuccessful) {
                val body = response.body()
                _accounts.value = body?.accounts ?: emptyList()
                _activeEmail.value = body?.activeEmail
                Result.Success(_accounts.value)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    /**
     * Calendarios de todas las cuentas, para el selector del editor de planes.
     *
     * Cada llamada consulta a Google una vez por cuenta vinculada, así que
     * conviene pedirlo una sola vez y quedarse con el resultado en memoria
     * (es lo que hace ToursViewModel).
     */
    suspend fun listCalendars(): Result<List<GcalAccountCalendars>> {
        return try {
            val response = ApiClient.service.gcalListCalendars()
            if (response.isSuccessful) {
                Result.Success(response.body()?.accounts ?: emptyList())
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun switchAccount(email: String): Result<Unit> {
        return try {
            val response = ApiClient.service.gcalSwitch(
                request = GcalEmailRequest(email)
            )
            if (response.isSuccessful) {
                refresh()
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }

    suspend fun deleteAccount(email: String): Result<Unit> {
        return try {
            val response = ApiClient.service.gcalDelete(
                request = GcalEmailRequest(email)
            )
            if (response.isSuccessful) {
                refresh()
                Result.Success(Unit)
            } else Result.Error("Error ${response.code()}")
        } catch (e: IOException) { Result.NoConnection }
        catch (e: Exception) { Result.Error(e.message ?: "Error") }
    }
}
