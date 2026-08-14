package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Plan turístico (data/tours.json).
 *
 * Estructura completa del panel web. `priceNet` es el costo interno: solo lo
 * ve y lo edita el Super Admin (canEditNet en util/Permissions.kt), así que
 * la lista pública se pide con getTours y la del panel con getToursAdmin.
 */
data class Tour(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("shortDesc") val shortDesc: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("category") val category: String = "",

    // ── Precio ──
    @SerializedName("price") val price: String = "",
    /** Precio tachado (antes) */
    @SerializedName("priceBefore") val priceBefore: String = "",
    @SerializedName("priceLabel") val priceLabel: String = "",
    /** Costo interno privado — SOLO Super Admin */
    @SerializedName("priceNet") val priceNet: String = "",

    // ── Imágenes y vídeo ──
    @SerializedName("img") val img: String = "",
    @SerializedName("gallery") val gallery: List<String> = emptyList(),
    @SerializedName("videoWeb") val videoWeb: String = "",
    @SerializedName("videoMobile") val videoMobile: String = "",

    // ── Visibilidad ──
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("popular") val popular: Boolean = false,

    @SerializedName("includes") val includes: List<String> = emptyList(),
    @SerializedName("testimonials") val testimonials: List<Testimonial> = emptyList(),

    // ── Cupos y horarios ──
    @SerializedName("cupos") val cupos: String = "",
    @SerializedName("showCupos") val showCupos: Boolean = false,
    @SerializedName("horarios") val horarios: List<Horario> = emptyList(),
    /** Horarios bloqueados en un día concreto */
    @SerializedName("blockedSlots") val blockedSlots: List<BlockedSlot> = emptyList(),
    /** Días completos no disponibles ("yyyy-MM-dd") */
    @SerializedName("unavailableDates") val unavailableDates: List<String> = emptyList(),

    // ── Punto de encuentro y mapa ──
    @SerializedName("muelleId") val muelleId: String = "",
    @SerializedName("mapEmbed") val mapEmbed: String = "",

    // ── Google Calendar por plan ──
    @SerializedName("gcalAccount") val gcalAccount: String = "",
    @SerializedName("gcalCalendarId") val gcalCalendarId: String = "",

    // ── Mensajes ──
    @SerializedName("whatsappMessage") val whatsappMessage: String = "",
    @SerializedName("operatorMessage") val operatorMessage: String = "",

    // ── Referencias a bloques definidos en Ajustes ──
    @SerializedName("faqIds") val faqIds: List<String> = emptyList(),
    @SerializedName("infoIds") val infoIds: List<String> = emptyList(),
    @SerializedName("scheduleIds") val scheduleIds: List<String> = emptyList(),
    @SerializedName("tagIds") val tagIds: List<String> = emptyList(),

    @SerializedName("metaDescription") val metaDescription: String = "",

    /** Traducción al inglés (en/index.html) */
    @SerializedName("en") val en: TourTranslation? = null
) {
    val hasImage: Boolean get() = img.isNotBlank()

    val displayPrice: String get() =
        if (price.isBlank() || price.equals("Consultar", true)) "Consultar" else "$$price"

    val hasDiscountPrice: Boolean get() = priceBefore.isNotBlank()

    val statusLabel: String get() = if (active) "Visible" else "Oculto"
}

/** Franja horaria de un plan */
data class Horario(
    @SerializedName("inicio") val inicio: String = "",
    @SerializedName("fin") val fin: String = ""
) {
    val label: String get() = if (fin.isBlank()) inicio else "$inicio – $fin"
}

/** Franja bloqueada en un día concreto */
data class BlockedSlot(
    /** "yyyy-MM-dd" */
    @SerializedName("date") val date: String = "",
    @SerializedName("inicio") val inicio: String = "",
    @SerializedName("fin") val fin: String = ""
)

/** Reseña que sale en la ficha del plan */
data class Testimonial(
    @SerializedName("name") val name: String = "",
    @SerializedName("rating") val rating: Int = 5,
    @SerializedName("text") val text: String = "",
    /** Texto libre: "Hace un mes" */
    @SerializedName("date") val date: String = "",
    @SerializedName("img") val img: String = "",
    /** La misma reseña en inglés (en/index.html). El panel web la guarda,
     *  así que tiene que estar aquí o se borraría al editar desde la app. */
    @SerializedName("textEn") val textEn: String = ""
)

/** Campos traducidos del plan */
data class TourTranslation(
    @SerializedName("title") val title: String = "",
    @SerializedName("shortDesc") val shortDesc: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("priceLabel") val priceLabel: String = "",
    @SerializedName("includes") val includes: List<String> = emptyList()
)
