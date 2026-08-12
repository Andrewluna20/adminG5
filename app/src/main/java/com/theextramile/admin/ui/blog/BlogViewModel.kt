package com.theextramile.admin.ui.blog

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theextramile.admin.data.model.BlogPost
import com.theextramile.admin.data.repository.ApiResult
import com.theextramile.admin.data.repository.BlogRepository
import com.theextramile.admin.data.repository.UploadRepository
import com.theextramile.admin.data.repository.UploadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Blog — port de admin-js/blog.js.
 *
 * Guardar cualquier entrada reescribe blog.json entero y regenera el sitemap,
 * así que el repositorio recarga después de guardar: el backend puede haber
 * cambiado el slug.
 */
class BlogViewModel(
    private val repository: BlogRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** "" = todas · "published" · "draft" */
    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    /** Entrada abierta en el editor (null = cerrado) */
    private val _editing = MutableStateFlow<BlogPost?>(null)
    val editing: StateFlow<BlogPost?> = _editing.asStateFlow()

    /** Id con el que se abrió el editor: null si es una entrada nueva */
    private var editingOriginalId: String? = null

    val posts: StateFlow<List<BlogPost>> =
        combine(repository.posts, _query, _filter) { list, q, f ->
            val needle = q.trim().lowercase()
            list.asSequence()
                .filter {
                    when (f) {
                        "published" -> it.published
                        "draft" -> !it.published
                        else -> true
                    }
                }
                .filter {
                    needle.isBlank() ||
                        it.title.lowercase().contains(needle) ||
                        it.excerpt.lowercase().contains(needle) ||
                        it.tags.any { t -> t.lowercase().contains(needle) }
                }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val counts: StateFlow<Map<String, Int>> = repository.posts
        .map { list ->
            mapOf(
                "" to list.size,
                "published" to list.count { it.published },
                "draft" to list.count { !it.published }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        load(initial = true)
    }

    fun onQueryChange(value: String) { _query.value = value }
    fun onFilterChange(value: String) { _filter.value = value }

    fun refresh() = load(initial = false)

    fun startNew() {
        editingOriginalId = null
        _editing.value = BlogPost(date = LocalDate.now().toString())
    }

    fun startEdit(post: BlogPost) {
        editingOriginalId = post.id
        _editing.value = post
    }

    fun updateDraft(transform: (BlogPost) -> BlogPost) {
        _editing.value = _editing.value?.let(transform)
    }

    fun cancelEdit() {
        editingOriginalId = null
        _editing.value = null
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    /** Sube la portada y la deja puesta en el borrador abierto */
    fun uploadCover(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            when (val r = uploadRepository.upload(uri, UploadType.BLOG)) {
                is ApiResult.Success -> {
                    updateDraft { it.copy(coverImage = r.data) }
                    _uiState.value = _uiState.value.copy(isUploading = false, message = "Portada subida")
                }
                else -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = r.errorMessage ?: "No se pudo subir la portada"
                )
            }
        }
    }

    fun save() {
        val draft = _editing.value ?: return
        if (draft.title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "La entrada necesita un título")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            when (val r = repository.upsert(draft, editingOriginalId)) {
                is ApiResult.Success -> {
                    editingOriginalId = null
                    _editing.value = null
                    _uiState.value = _uiState.value.copy(isSaving = false, message = "Entrada guardada")
                }
                else -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = r.errorMessage ?: "No se pudo guardar"
                )
            }
        }
    }

    fun delete(post: BlogPost) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val r = repository.delete(post.id)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                error = r.errorMessage,
                message = if (r is ApiResult.Success) "Entrada eliminada" else null
            )
        }
    }

    fun togglePublished(post: BlogPost) {
        viewModelScope.launch {
            val r = repository.togglePublished(post.id)
            if (r !is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(error = r.errorMessage)
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initial, isRefreshing = !initial, error = null
            )
            val r = repository.refresh()
            _uiState.value = _uiState.value.copy(
                isLoading = false, isRefreshing = false, error = r.errorMessage
            )
        }
    }
}
