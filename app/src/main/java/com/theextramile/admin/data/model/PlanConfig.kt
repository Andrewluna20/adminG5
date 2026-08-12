package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Descuento aplicable a uno o varios planes (data/discounts.json).
 *
 * Espejo de normalizeDiscounts() en api/data.php:
 *  - `type` solo puede ser "percent" o "amount" (cualquier otra cosa → percent)
 *  - si es "percent", el backend recorta el valor a 100
 *  - `tourIds` vacío = se aplica a todos los planes
 */
data class Discount(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = TYPE_PERCENT,
    @SerializedName("value") val value: Int = 0,
    @SerializedName("tourIds") val tourIds: List<String> = emptyList(),
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("createdAt") val createdAt: String = ""
) {
    val isPercent: Boolean get() = type != TYPE_AMOUNT

    val appliesToAll: Boolean get() = tourIds.isEmpty()

    /** "15%" o "$50.000" */
    val displayValue: String
        get() = if (isPercent) "$value%" else "$" + formatThousands(value)

    /** Precio con el descuento ya aplicado (nunca baja de 0) */
    fun applyTo(price: Int): Int {
        val result = if (isPercent) price - (price * value / 100) else price - value
        return result.coerceAtLeast(0)
    }

    companion object {
        const val TYPE_PERCENT = "percent"
        const val TYPE_AMOUNT = "amount"
    }
}

/**
 * Muelle / punto de encuentro (data/muelles.json).
 * Se enlaza desde cada plan por `muelleId` y sale dentro del tiquete.
 */
data class Muelle(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    /** Enlace de Google Maps que se abre desde el tiquete */
    @SerializedName("mapsUrl") val mapsUrl: String = "",
    @SerializedName("image") val image: String = ""
) {
    val hasImage: Boolean get() = image.isNotBlank()
    val hasMap: Boolean get() = mapsUrl.isNotBlank()
}

/**
 * Vendedor con enlace propio (data/sellers.json).
 *
 * `slug` lo normaliza el backend (sellerSlugify) y le busca variante libre
 * (-2, -3…) si choca con otro vendedor o con una página del sitio. El slug de
 * un vendedor que ya existía NO cambia salvo que se edite a mano, porque sus
 * enlaces ya están repartidos. `url` la calcula el backend, es de solo lectura.
 */
data class Seller(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
    /** Calculada por el backend (sellerLinkUrl) — no se manda al guardar */
    @SerializedName("url") val url: String = ""
)

/** El backend recorta la lista de vendedores a 200 */
const val MAX_SELLERS = 200

/** Separador de miles con punto, como el panel web */
fun formatThousands(n: Int): String =
    n.toString().reversed().chunked(3).joinToString(".").reversed()
