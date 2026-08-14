package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.ReservationsRequest
import com.theextramile.admin.data.model.Reservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Repositorio de reservas.
 * Mantiene caché en memoria para no pedir al servidor cada vez.
 */
class ReservationRepository {

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        object NoConnection : Result<Nothing>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /** Refresca la lista desde el servidor */
    suspend fun refresh(): Result<List<Reservation>> {
        return try {
            val response = ApiClient.service.getReservations()
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                _reservations.value = list
                Result.Success(list)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: IOException) {
            Result.NoConnection
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error desconocido")
        }
    }

    /** Actualiza el estado de una reserva (pending/confirmed/cancelled) */
    suspend fun updateStatus(reservationId: String, newStatus: String): Result<Unit> {
        val current = _reservations.value
        val updated = current.map {
            if (it.id == reservationId) it.copy(status = newStatus) else it
        }
        return saveAll(updated)
    }

    /** Borra reservas (las que estén en la lista de IDs) */
    suspend fun deleteReservations(ids: Set<String>): Result<Unit> {
        val current = _reservations.value
        val updated = current.filterNot { it.id in ids }
        return saveAll(updated)
    }

    /**
     * Registra el pago de una reserva — espejo de submitPayment() en
     * admin-js/reservations.js.
     *
     * ⚠️ Guardar el pago TAMBIÉN pasa la reserva a "confirmed". No es un
     * efecto secundario: es lo que hace el panel web, y es lo que dispara
     * en el servidor el correo de confirmación con el tiquete. Si aquí se
     * dejara en "pending", el cliente nunca recibiría ese correo.
     */
    suspend fun updatePayment(
        id: String,
        total: Int,
        deposit: Int,
        balance: Int,
        paymentStatus: String
    ): Result<Unit> {
        val updated = _reservations.value.map {
            if (it.id == id) it.copy(
                total = total,
                deposit = deposit,
                balance = balance,
                paymentStatus = paymentStatus,
                status = "confirmed"
            ) else it
        }
        return saveAll(updated)
    }

    /** Guarda toda la lista al servidor */
    private suspend fun saveAll(list: List<Reservation>): Result<Unit> {
        return try {
            val response = ApiClient.service.saveReservations(
                request = ReservationsRequest(list)
            )
            if (response.isSuccessful) {
                _reservations.value = list
                Result.Success(Unit)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: IOException) {
            Result.NoConnection
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error desconocido")
        }
    }
}
