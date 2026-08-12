package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Beneficio del cliente (data/benefits.json).
 *
 * Espejo exacto de normalizeBenefits() en api/data.php: si la app manda un
 * beneficio sin `messageId`, el backend conserva el que ya estaba guardado.
 */
data class Benefit(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    /** Lo que pegó el admin (iframe completo), para poder reeditarlo */
    @SerializedName("mapEmbed") val mapEmbed: String = "",
    /** URL limpia del iframe que calcula el backend — solo lectura */
    @SerializedName("mapSrc") val mapSrc: String = "",
    /** Enlace para el botón del correo que calcula el backend — solo lectura */
    @SerializedName("mapUrl") val mapUrl: String = "",
    @SerializedName("images") val images: List<String> = emptyList(),
    /** ¿Se le pide fecha al cliente al reservar este beneficio? */
    @SerializedName("askDate") val askDate: Boolean = true,
    /** ¿Se le piden pasajeros? */
    @SerializedName("askPax") val askPax: Boolean = true,
    /** ¿Sale en los correos al cliente? (van 4 como mucho) */
    @SerializedName("inEmail") val inEmail: Boolean = true,
    /** Mensaje del correo del tiquete; "" = el de siempre */
    @SerializedName("messageId") val messageId: String = "",
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("createdAt") val createdAt: String = ""
) {
    /** El panel muestra `title` y cae a `name` cuando está vacío */
    val displayTitle: String get() = title.ifBlank { name }.ifBlank { "Sin título" }

    val coverImage: String? get() = images.firstOrNull()

    val hasMap: Boolean get() = mapSrc.isNotBlank()
}

/** Máximo de imágenes por beneficio (TEM_MAX_BENEFIT_IMAGES en el backend) */
const val MAX_BENEFIT_IMAGES = 6

/** Máximo de beneficios que caben en los correos al cliente */
const val MAX_BENEFITS_IN_EMAIL = 4

/**
 * Mensaje del correo del tiquete asociable a un beneficio
 * (data/benefit_messages.json).
 */
data class BenefitMessage(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

/**
 * Beneficio que YA reservó un cliente (data/benefit_bookings.json).
 * Lo crea el sitio público; el panel solo lista, edita y borra.
 */
data class BenefitBooking(
    @SerializedName("id") val id: String = "",
    @SerializedName("benefitId") val benefitId: String = "",
    @SerializedName("benefitTitle") val benefitTitle: String = "",
    @SerializedName("benefitName") val benefitName: String = "",
    @SerializedName("reservationId") val reservationId: String = "",
    @SerializedName("groupId") val groupId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("dateLabel") val dateLabel: String = "",
    @SerializedName("pax") val pax: Int = 0,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
) {
    val displayBenefit: String get() = benefitTitle.ifBlank { benefitName }.ifBlank { "Beneficio" }

    /** El panel muestra dateLabel cuando existe (ya viene formateada en español) */
    val displayDate: String get() = dateLabel.ifBlank { date }
}
