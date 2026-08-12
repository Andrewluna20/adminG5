package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.model.ActivityEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Historial de actividad del panel. Solo lectura: lo escribe el servidor.
 * Llega ya invertido (recientes primero) y recortado a 500 registros.
 */
class ActivityRepository {

    private val _log = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val log: StateFlow<List<ActivityEntry>> = _log.asStateFlow()

    suspend fun refresh(): ApiResult<List<ActivityEntry>> =
        when (val r = apiCall { ApiClient.service.getActivityLog() }) {
            is ApiResult.Success -> {
                _log.value = r.data.log
                ApiResult.Success(r.data.log)
            }
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }
}
