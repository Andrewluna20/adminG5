package com.theextramile.admin.data.repository

import com.theextramile.admin.data.api.ApiClient
import com.theextramile.admin.data.api.BlogPostsRequest
import com.theextramile.admin.data.model.BlogPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Entradas del blog.
 *
 * Se usa getBlogPostsAdmin (incluye borradores). Guardar reemplaza la lista
 * entera y el backend regenera el sitemap, así que después de guardar hay que
 * recargar: el `id` (slug) puede haber cambiado si venía vacío o repetido.
 */
class BlogRepository {

    private val _posts = MutableStateFlow<List<BlogPost>>(emptyList())
    val posts: StateFlow<List<BlogPost>> = _posts.asStateFlow()

    suspend fun refresh(): ApiResult<List<BlogPost>> =
        apiCall { ApiClient.service.getBlogPostsAdmin() }.also {
            if (it is ApiResult.Success) {
                _posts.value = it.data.sortedByDescending { p -> p.date }
            }
        }

    private suspend fun saveAll(list: List<BlogPost>): ApiResult<List<BlogPost>> =
        when (val r = apiAction { ApiClient.service.saveBlogPosts(request = BlogPostsRequest(list)) }) {
            is ApiResult.Success -> refresh()   // el backend recalcula los slugs
            is ApiResult.Error -> r
            ApiResult.NoConnection -> ApiResult.NoConnection
        }

    suspend fun upsert(post: BlogPost, originalId: String?): ApiResult<List<BlogPost>> {
        val current = _posts.value
        val updated = if (!originalId.isNullOrBlank() && current.any { it.id == originalId }) {
            current.map { if (it.id == originalId) post else it }
        } else {
            current + post
        }
        return saveAll(updated)
    }

    suspend fun delete(id: String): ApiResult<List<BlogPost>> =
        saveAll(_posts.value.filterNot { it.id == id })

    suspend fun togglePublished(id: String): ApiResult<List<BlogPost>> =
        saveAll(_posts.value.map { if (it.id == id) it.copy(published = !it.published) else it })
}
