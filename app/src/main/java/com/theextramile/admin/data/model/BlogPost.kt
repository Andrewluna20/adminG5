package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Entrada del blog (data/blog.json).
 *
 * El `id` es el slug: el backend lo recalcula con blogSlugify() al guardar y
 * resuelve duplicados añadiendo -2, -3… Si se manda vacío, lo genera del título.
 * Guardar cualquier entrada dispara regeneratePublicIndex() (refresca sitemap.xml).
 */
data class BlogPost(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("excerpt") val excerpt: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("coverImage") val coverImage: String = "",
    @SerializedName("author") val author: String = "",
    /** yyyy-MM-dd — si va vacía el backend pone la de hoy */
    @SerializedName("date") val date: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("published") val published: Boolean = false,
    /** Destacado en la barra del sitio público */
    @SerializedName("showInNav") val showInNav: Boolean = false,
    /** Destacado en el pie del sitio público */
    @SerializedName("showInFooter") val showInFooter: Boolean = false,
    /** Etiqueta corta de la barra — el backend la recorta a 40 caracteres */
    @SerializedName("navLabel") val navLabel: String = "",
    @SerializedName("metaTitle") val metaTitle: String = "",
    @SerializedName("metaDescription") val metaDescription: String = ""
) {
    val hasCover: Boolean get() = coverImage.isNotBlank()

    val statusLabel: String get() = if (published) "Publicado" else "Borrador"

    /** Etiqueta que se ve en la barra: navLabel si existe, si no el título */
    val effectiveNavLabel: String get() = navLabel.ifBlank { title }.take(40)
}

/** Límite que aplica el backend a navLabel */
const val MAX_NAV_LABEL = 40
