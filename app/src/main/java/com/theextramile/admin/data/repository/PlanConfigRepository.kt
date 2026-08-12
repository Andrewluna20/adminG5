package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.DiscountsRequest
import com.theextramile.admin.data.api.MuellesRequest
import com.theextramile.admin.data.api.SellersRequest
import com.theextramile.admin.data.model.Discount
import com.theextramile.admin.data.model.MAX_SELLERS
import com.theextramile.admin.data.model.Muelle
import com.theextramile.admin.data.model.Seller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Configuración de planes: descuentos, muelles y vendedores.
 *
 * Son tres archivos JSON distintos en el servidor pero una sola sección del
 * panel, así que van juntos aquí.
 */
class PlanConfigRepository {

    private val _discounts = MutableStateFlow<List<Discount>>(emptyList())
    val discounts: StateFlow<List<Discount>> = _discounts.asStateFlow()

    private val _muelles = MutableStateFlow<List<Muelle>>(emptyList())
    val muelles: StateFlow<List<Muelle>> = _muelles.asStateFlow()

    private val _sellers = MutableStateFlow<List<Seller>>(emptyList())
    val sellers: StateFlow<List<Seller>> = _sellers.asStateFlow()

    /** Raíz pública que devuelve getSellers, para armar los enlaces */
    private val _sellerBase = MutableStateFlow("")
    val sellerBase: StateFlow<String> = _sellerBase.asStateFlow()

    // ═══════ Descuentos ═══════

    suspend fun refreshDiscounts(): ApiResult<List<Discount>> =
        apiCall { ApiClient.service.getDiscounts() }.also {
            if (it is ApiResult.Success) _discounts.value = it.data
        }

    /** El backend devuelve la lista normalizada (ids nuevos, % recortado a 100) */
    suspend fun saveDiscounts(list: List<Discount>): ApiResult<List<Discount>> =
        when (val r = apiCall { ApiClient.service.saveDiscounts(request = DiscountsRequest(list)) }) {
            is ApiResult.Success ->
                if (r.data.success) {
                    _discounts.value = r.data.discounts
                    ApiResult.Success(r.data.discounts)
                } else {
                    ApiResult.Error(r.data.error ?: "No se pudieron guardar los descuentos")
                }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }

    suspend fun upsertDiscount(discount: Discount): ApiResult<List<Discount>> {
        val current = _discounts.value
        val exists = discount.id.isNotBlank() && current.any { it.id == discount.id }
        val updated = if (exists) {
            current.map { if (it.id == discount.id) discount else it }
        } else {
            current + discount
        }
        return saveDiscounts(updated)
    }

    suspend fun deleteDiscount(id: String): ApiResult<List<Discount>> =
        saveDiscounts(_discounts.value.filterNot { it.id == id })

    suspend fun toggleDiscount(id: String): ApiResult<List<Discount>> =
        saveDiscounts(_discounts.value.map { if (it.id == id) it.copy(active = !it.active) else it })

    // ═══════ Muelles ═══════

    suspend fun refreshMuelles(): ApiResult<List<Muelle>> =
        apiCall { ApiClient.service.getMuelles() }.also {
            if (it is ApiResult.Success) _muelles.value = it.data
        }

    suspend fun saveMuelles(list: List<Muelle>): ApiResult<Unit> =
        apiAction { ApiClient.service.saveMuelles(request = MuellesRequest(list)) }
            .also { if (it is ApiResult.Success) _muelles.value = list }

    suspend fun upsertMuelle(muelle: Muelle): ApiResult<Unit> {
        val current = _muelles.value
        val exists = muelle.id.isNotBlank() && current.any { it.id == muelle.id }
        val updated = if (exists) {
            current.map { if (it.id == muelle.id) muelle else it }
        } else {
            // Mismo formato de id que muelles.js: 'muelle-' + timestamp en base 36
            current + muelle.copy(
                id = muelle.id.ifBlank { "muelle-" + System.currentTimeMillis().toString(36) }
            )
        }
        return saveMuelles(updated)
    }

    suspend fun deleteMuelle(id: String): ApiResult<Unit> =
        saveMuelles(_muelles.value.filterNot { it.id == id })

    // ═══════ Vendedores ═══════

    suspend fun refreshSellers(): ApiResult<List<Seller>> =
        when (val r = apiCall { ApiClient.service.getSellers() }) {
            is ApiResult.Success -> {
                _sellers.value = r.data.sellers
                _sellerBase.value = r.data.base
                ApiResult.Success(r.data.sellers)
            }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }

    /**
     * El backend recorta a 200, normaliza el slug y le busca variante libre.
     * Devuelve la lista final con la `url` de cada vendedor ya calculada.
     */
    suspend fun saveSellers(list: List<Seller>): ApiResult<List<Seller>> {
        val trimmed = if (list.size > MAX_SELLERS) list.take(MAX_SELLERS) else list
        return when (val r = apiCall { ApiClient.service.saveSellers(request = SellersRequest(trimmed)) }) {
            is ApiResult.Success ->
                if (r.data.success) {
                    _sellers.value = r.data.sellers
                    if (r.data.base.isNotBlank()) _sellerBase.value = r.data.base
                    ApiResult.Success(r.data.sellers)
                } else {
                    ApiResult.Error(r.data.error ?: "No se pudieron guardar los vendedores")
                }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }
    }

    suspend fun upsertSeller(seller: Seller): ApiResult<List<Seller>> {
        val current = _sellers.value
        val exists = seller.id.isNotBlank() && current.any { it.id == seller.id }
        val updated = if (exists) {
            current.map { if (it.id == seller.id) seller else it }
        } else {
            current + seller
        }
        return saveSellers(updated)
    }

    suspend fun deleteSeller(id: String): ApiResult<List<Seller>> =
        saveSellers(_sellers.value.filterNot { it.id == id })
}
