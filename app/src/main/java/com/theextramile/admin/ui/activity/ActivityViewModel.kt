package com.theextramile.admin.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.ActivityEntry
import com.theextramile.admin.data.repository.ActivityRepository
import com.theextramile.admin.data.repository.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Historial de actividad. Filtro por tipo y búsqueda por nombre, correo o
 * detalle — igual que renderActivity() en admin-js/activity.js.
 */
class ActivityViewModel(private val repository: ActivityRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _typeFilter = MutableStateFlow("")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    /** Total sin filtrar — el panel muestra "N de M registro(s)" */
    val total: StateFlow<Int> = repository.log
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val entries: StateFlow<List<ActivityEntry>> =
        combine(repository.log, _query, _typeFilter) { log, q, type ->
            val needle = q.trim().lowercase()
            log.asSequence()
                .filter { type.isBlank() || it.action == type }
                .filter {
                    needle.isBlank() ||
                        it.userName.lowercase().contains(needle) ||
                        it.userEmail.lowercase().contains(needle) ||
                        it.summary.lowercase().contains(needle)
                }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        load(initial = true)
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun onTypeChange(value: String) { _typeFilter.value = value }

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial,
                isRefreshing = !initial,
                error = null
            )
            val result = repository.refresh()
            _uiState.value = UiState(
                isLoading = false,
                isRefreshing = false,
                error = (result as? ApiResult.Error)?.message
                    ?: if (result is ApiResult.NoConnection) "Sin conexión" else null
            )
        }
    }
}
