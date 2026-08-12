package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.BenefitMessagesRequest
import com.theextramile.admin.data.api.BenefitsRequest
import com.theextramile.admin.data.api.IdRequest
import com.theextramile.admin.data.api.UpdateBenefitBookingRequest
import com.theextramile.admin.data.model.Benefit
import com.theextramile.admin.data.model.BenefitBooking
import com.theextramile.admin.data.model.BenefitMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Beneficios del cliente: el catálogo, los mensajes del correo y las reservas
 * de beneficios que ya hicieron los clientes.
 */
class BenefitRepository {

    private val _benefits = MutableStateFlow<List<Benefit>>(emptyList())
    val benefits: StateFlow<List<Benefit>> = _benefits.asStateFlow()

    private val _messages = MutableStateFlow<List<BenefitMessage>>(emptyList())
    val messages: StateFlow<List<BenefitMessage>> = _messages.asStateFlow()

    private val _bookings = MutableStateFlow<List<BenefitBooking>>(emptyList())
    val bookings: StateFlow<List<BenefitBooking>> = _bookings.asStateFlow()

    // ═══════ Catálogo ═══════

    suspend fun refreshBenefits(): ApiResult<List<Benefit>> =
        apiCall { ApiClient.service.getBenefits() }.also {
            if (it is ApiResult.Success) _benefits.value = it.data
        }

    /**
     * Guarda el catálogo completo. El backend devuelve la lista ya
     * normalizada (ids generados, mapSrc/mapUrl calculados), así que la
     * respuesta reemplaza la caché — si no, la app se quedaría con los ids
     * provisionales que inventó ella.
     */
    suspend fun saveBenefits(list: List<Benefit>): ApiResult<List<Benefit>> =
        when (val r = apiCall { ApiClient.service.saveBenefits(request = BenefitsRequest(list)) }) {
            is ApiResult.Success ->
                if (r.data.success) {
                    _benefits.value = r.data.benefits
                    ApiResult.Success(r.data.benefits)
                } else {
                    ApiResult.Error(r.data.error ?: "No se pudieron guardar los beneficios")
                }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }

    suspend fun upsertBenefit(benefit: Benefit): ApiResult<List<Benefit>> {
        val current = _benefits.value
        val exists = current.any { it.id == benefit.id && it.id.isNotBlank() }
        val updated = if (exists) {
            current.map { if (it.id == benefit.id) benefit else it }
        } else {
            current + benefit
        }
        return saveBenefits(updated)
    }

    suspend fun deleteBenefit(id: String): ApiResult<List<Benefit>> =
        saveBenefits(_benefits.value.filterNot { it.id == id })

    suspend fun toggleActive(id: String): ApiResult<List<Benefit>> =
        saveBenefits(_benefits.value.map { if (it.id == id) it.copy(active = !it.active) else it })

    // ═══════ Mensajes del correo ═══════

    suspend fun refreshMessages(): ApiResult<List<BenefitMessage>> =
        apiCall { ApiClient.service.getBenefitMessages() }.also {
            if (it is ApiResult.Success) _messages.value = it.data
        }

    suspend fun saveMessages(list: List<BenefitMessage>): ApiResult<Unit> =
        apiAction {
            ApiClient.service.saveBenefitMessages(request = BenefitMessagesRequest(list))
        }.also {
            if (it is ApiResult.Success) _messages.value = list
        }

    suspend fun upsertMessage(message: BenefitMessage): ApiResult<Unit> {
        val current = _messages.value
        val exists = current.any { it.id == message.id && it.id.isNotBlank() }
        val updated = if (exists) {
            current.map { if (it.id == message.id) message else it }
        } else {
            current + message
        }
        return saveMessages(updated)
    }

    suspend fun deleteMessage(id: String): ApiResult<Unit> =
        saveMessages(_messages.value.filterNot { it.id == id })

    // ═══════ Reservas de beneficios ═══════

    suspend fun refreshBookings(): ApiResult<List<BenefitBooking>> =
        apiCall { ApiClient.service.getBenefitBookings() }.also {
            if (it is ApiResult.Success) {
                // Más recientes primero, como el panel
                _bookings.value = it.data.sortedByDescending { b -> b.createdAt }
            }
        }

    /**
     * Edita una reserva de beneficio. `date` y `pax` solo se mandan si el
     * beneficio los pide (askDate / askPax): el backend los rechaza si el
     * beneficio no los usa.
     */
    suspend fun updateBooking(
        id: String,
        date: String?,
        pax: Int?,
        notes: String?
    ): ApiResult<Unit> = apiAction {
        ApiClient.service.updateBenefitBooking(
            request = UpdateBenefitBookingRequest(id = id, date = date, pax = pax, notes = notes)
        )
    }.also { if (it is ApiResult.Success) refreshBookings() }

    suspend fun deleteBooking(id: String): ApiResult<Unit> =
        apiAction { ApiClient.service.deleteBenefitBooking(request = IdRequest(id)) }
            .also {
                if (it is ApiResult.Success) {
                    _bookings.value = _bookings.value.filterNot { b -> b.id == id }
                }
            }
}
