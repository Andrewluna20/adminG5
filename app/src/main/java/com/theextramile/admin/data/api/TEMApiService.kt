package com.theextramile.admin.data.api

import com.google.gson.annotations.SerializedName
import com.theextramile.admin.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Todas las acciones del panel web, una por una.
 *
 * Los endpoints reales son cuatro (data.php, usuarios.php, gcal.php,
 * upload.php) y lo que cambia es el parámetro ?action=. El AuthInterceptor de
 * ApiClient pone el header Authorization: Bearer <token> en cada llamada.
 *
 * ⚠️ Las acciones de guardado mandan la colección ENTERA, no un elemento:
 * saveTours reemplaza todos los planes, saveReservations todas las reservas.
 * Es como funciona el backend (saveJson del archivo completo), así que hay que
 * cargar la lista, modificarla en memoria y volver a mandarla completa.
 */
interface TEMApiService {

    // ═══════════════════════════════════════════
    // AUTENTICACIÓN (usuarios.php)
    // ═══════════════════════════════════════════

    @POST("usuarios.php")
    suspend fun login(
        @Query("action") action: String = "login",
        @Body credentials: LoginRequest
    ): Response<LoginResponse>

    @POST("usuarios.php")
    suspend fun logout(@Query("action") action: String = "logout"): Response<SuccessResponse>

    @GET("usuarios.php")
    suspend fun verify(@Query("action") action: String = "verify"): Response<VerifyResponse>

    @GET("usuarios.php")
    suspend fun me(@Query("action") action: String = "me"): Response<VerifyResponse>

    /** Cambiar la propia contraseña estando dentro */
    @POST("usuarios.php")
    suspend fun changeOwnPassword(
        @Query("action") action: String = "change_own_password",
        @Body request: ChangeOwnPasswordRequest
    ): Response<SuccessResponse>

    /** Paso 1 de "olvidé mi contraseña": manda un código al correo */
    @POST("usuarios.php")
    suspend fun sendPasswordCode(
        @Query("action") action: String = "send_password_code",
        @Body request: EmailRequest
    ): Response<SuccessResponse>

    /** Paso 2: cambia la contraseña con el código recibido */
    @POST("usuarios.php")
    suspend fun changePasswordWithCode(
        @Query("action") action: String = "change_password_with_code",
        @Body request: PasswordCodeRequest
    ): Response<SuccessResponse>

    // ═══════════════════════════════════════════
    // USUARIOS DEL PANEL (usuarios.php)
    // ═══════════════════════════════════════════

    @GET("usuarios.php")
    suspend fun listUsers(@Query("action") action: String = "list"): Response<List<User>>

    @POST("usuarios.php")
    suspend fun saveUser(
        @Query("action") action: String = "save",
        @Body user: SaveUserRequest
    ): Response<SuccessResponse>

    @POST("usuarios.php")
    suspend fun deleteUser(
        @Query("action") action: String = "delete",
        @Body request: IdRequest
    ): Response<SuccessResponse>

    // ═══════════════════════════════════════════
    // RESERVAS (data.php)
    // ═══════════════════════════════════════════

    @GET("data.php")
    suspend fun getReservations(
        @Query("action") action: String = "getReservations"
    ): Response<List<Reservation>>

    /** Reemplaza la lista COMPLETA de reservas */
    @POST("data.php")
    suspend fun saveReservations(
        @Query("action") action: String = "saveReservations",
        @Body request: ReservationsRequest
    ): Response<SuccessResponse>

    /** Alta manual desde el panel */
    @POST("data.php")
    suspend fun createReservation(
        @Query("action") action: String = "createReservation",
        @Body reservation: Map<String, @JvmSuppressWildcards Any?>
    ): Response<CreateReservationResponse>

    /**
     * Cambia la fecha de una reserva sin reescribir la lista entera; el
     * backend también mueve el evento de Google Calendar.
     */
    @POST("data.php")
    suspend fun updateReservationDate(
        @Query("action") action: String = "updateReservationDate",
        @Body request: UpdateDateRequest
    ): Response<SuccessResponse>

    /** Busca una reserva por su código (TEM-XXXXXXXX) */
    @GET("data.php")
    suspend fun lookupReservation(
        @Query("action") action: String = "lookupReservation",
        @Query("id") id: String
    ): Response<Reservation>

    // ═══════════════════════════════════════════
    // PLANES (data.php)
    // ═══════════════════════════════════════════

    /** Lista pública (sin priceNet) */
    @GET("data.php")
    suspend fun getTours(@Query("action") action: String = "getTours"): Response<List<Tour>>

    /** Lista del panel: incluye priceNet y los planes ocultos */
    @GET("data.php")
    suspend fun getToursAdmin(
        @Query("action") action: String = "getToursAdmin"
    ): Response<List<Tour>>

    /** Reemplaza la lista COMPLETA de planes */
    @POST("data.php")
    suspend fun saveTours(
        @Query("action") action: String = "saveTours",
        @Body request: ToursRequest
    ): Response<SuccessResponse>

    /** Imágenes ya subidas a uploads/tours-webp, para reutilizarlas */
    @GET("data.php")
    suspend fun listTourImages(
        @Query("action") action: String = "listTourImages"
    ): Response<List<TourImage>>

    // ═══════════════════════════════════════════
    // AJUSTES (data.php)
    // ═══════════════════════════════════════════

    @GET("data.php")
    suspend fun getSettings(@Query("action") action: String = "getSettings"): Response<SiteSettings>

    /** ⚠️ Manda el objeto COMPLETO: lo que no vaya se pierde */
    @POST("data.php")
    suspend fun saveSettings(
        @Query("action") action: String = "saveSettings",
        @Body request: SettingsRequest
    ): Response<SuccessResponse>

    /** Llave secreta de Bold — el servidor nunca la devuelve */
    @POST("data.php")
    suspend fun saveBoldSecret(
        @Query("action") action: String = "saveBoldSecret",
        @Body request: SecretRequest
    ): Response<SuccessResponse>

    /** Llave secreta de reCAPTCHA — el servidor nunca la devuelve */
    @POST("data.php")
    suspend fun saveRecaptchaSecret(
        @Query("action") action: String = "saveRecaptchaSecret",
        @Body request: SecretRequest
    ): Response<SuccessResponse>

    /** Reconstruye index.html + sitemap.xml del sitio público */
    @POST("data.php")
    suspend fun regenerateIndex(
        @Query("action") action: String = "regenerateIndex",
        @Body body: Map<String, String> = emptyMap()
    ): Response<SuccessResponse>

    // ═══════════════════════════════════════════
    // BENEFICIOS (data.php)
    // ═══════════════════════════════════════════

    @GET("data.php")
    suspend fun getBenefits(@Query("action") action: String = "getBenefits"): Response<List<Benefit>>

    /**
     * ⚠️ Si un beneficio va sin `messageId`, el backend le deja el que ya
     * tenía guardado. Manda siempre el objeto completo que devolvió getBenefits.
     */
    @POST("data.php")
    suspend fun saveBenefits(
        @Query("action") action: String = "saveBenefits",
        @Body request: BenefitsRequest
    ): Response<BenefitsResponse>

    @GET("data.php")
    suspend fun getBenefitMessages(
        @Query("action") action: String = "getBenefitMessages"
    ): Response<List<BenefitMessage>>

    @POST("data.php")
    suspend fun saveBenefitMessages(
        @Query("action") action: String = "saveBenefitMessages",
        @Body request: BenefitMessagesRequest
    ): Response<SuccessResponse>

    /** Beneficios que ya reservaron los clientes */
    @GET("data.php")
    suspend fun getBenefitBookings(
        @Query("action") action: String = "getBenefitBookings"
    ): Response<List<BenefitBooking>>

    @POST("data.php")
    suspend fun updateBenefitBooking(
        @Query("action") action: String = "updateBenefitBooking",
        @Body request: UpdateBenefitBookingRequest
    ): Response<SuccessResponse>

    @POST("data.php")
    suspend fun deleteBenefitBooking(
        @Query("action") action: String = "deleteBenefitBooking",
        @Body request: IdRequest
    ): Response<SuccessResponse>

    // ═══════════════════════════════════════════
    // BLOG (data.php)
    // ═══════════════════════════════════════════

    /** Incluye los borradores (getBlogPosts solo devuelve los publicados) */
    @GET("data.php")
    suspend fun getBlogPostsAdmin(
        @Query("action") action: String = "getBlogPostsAdmin"
    ): Response<List<BlogPost>>

    /** Reemplaza la lista COMPLETA y regenera el sitemap */
    @POST("data.php")
    suspend fun saveBlogPosts(
        @Query("action") action: String = "saveBlogPosts",
        @Body request: BlogPostsRequest
    ): Response<SuccessResponse>

    // ═══════════════════════════════════════════
    // CONFIGURACIÓN DE PLANES (data.php)
    // ═══════════════════════════════════════════

    @GET("data.php")
    suspend fun getDiscounts(@Query("action") action: String = "getDiscounts"): Response<List<Discount>>

    @POST("data.php")
    suspend fun saveDiscounts(
        @Query("action") action: String = "saveDiscounts",
        @Body request: DiscountsRequest
    ): Response<DiscountsResponse>

    @GET("data.php")
    suspend fun getMuelles(@Query("action") action: String = "getMuelles"): Response<List<Muelle>>

    @POST("data.php")
    suspend fun saveMuelles(
        @Query("action") action: String = "saveMuelles",
        @Body request: MuellesRequest
    ): Response<SuccessResponse>

    @GET("data.php")
    suspend fun getSellers(@Query("action") action: String = "getSellers"): Response<SellersResponse>

    @POST("data.php")
    suspend fun saveSellers(
        @Query("action") action: String = "saveSellers",
        @Body request: SellersRequest
    ): Response<SellersResponse>

    // ═══════════════════════════════════════════
    // ACTIVIDAD Y CORREOS (data.php)
    // ═══════════════════════════════════════════

    @GET("data.php")
    suspend fun getActivityLog(
        @Query("action") action: String = "getActivityLog"
    ): Response<ActivityLogResponse>

    @GET("data.php")
    suspend fun emailLog(@Query("action") action: String = "emailLog"): Response<List<EmailLogEntry>>

    @GET("data.php")
    suspend fun sendTestEmail(
        @Query("action") action: String = "sendTestEmail",
        @Query("to") to: String
    ): Response<SendTestEmailResponse>

    // ═══════════════════════════════════════════
    // SUBIDA DE ARCHIVOS (upload.php)
    // ═══════════════════════════════════════════

    /**
     * type solo puede ser uno de los 5 que acepta upload.php:
     * tours | hero | brand | favicon | blog (ver UploadType).
     * Las imágenes de muelles y de beneficios van como 'tours', igual que
     * en el panel web.
     */
    @Multipart
    @POST("upload.php")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<UploadResponse>

    // ═══════════════════════════════════════════
    // GOOGLE CALENDAR (gcal.php)
    // ═══════════════════════════════════════════

    @GET("gcal.php")
    suspend fun gcalConfigStatus(
        @Query("action") action: String = "config_status"
    ): Response<GcalConfigStatus>

    @GET("gcal.php")
    suspend fun gcalList(@Query("action") action: String = "list"): Response<GcalListResponse>

    @POST("gcal.php")
    suspend fun gcalSwitch(
        @Query("action") action: String = "switch",
        @Body request: GcalEmailRequest
    ): Response<SuccessResponse>

    @POST("gcal.php")
    suspend fun gcalDelete(
        @Query("action") action: String = "delete",
        @Body request: GcalEmailRequest
    ): Response<SuccessResponse>
}

// ═══════════════════════════════════════════════════════
// PETICIONES
// ═══════════════════════════════════════════════════════

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SaveUserRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("role") val role: String
)

data class IdRequest(@SerializedName("id") val id: String)

data class EmailRequest(@SerializedName("email") val email: String)

data class ChangeOwnPasswordRequest(
    @SerializedName("current") val current: String,
    @SerializedName("password") val password: String
)

data class PasswordCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("password") val password: String
)

data class ReservationsRequest(
    @SerializedName("reservations") val reservations: List<Reservation>
)

data class UpdateDateRequest(
    @SerializedName("id") val id: String,
    /** Fecha ya formateada en español que se le muestra al cliente */
    @SerializedName("date") val date: String,
    /** ISO 8601 — la que manda de verdad */
    @SerializedName("dateRaw") val dateRaw: String
)

data class ToursRequest(@SerializedName("tours") val tours: List<Tour>)

data class SettingsRequest(@SerializedName("settings") val settings: SiteSettings)

data class SecretRequest(@SerializedName("secret") val secret: String)

data class BenefitsRequest(@SerializedName("benefits") val benefits: List<Benefit>)

data class BenefitMessagesRequest(
    @SerializedName("messages") val messages: List<BenefitMessage>
)

data class UpdateBenefitBookingRequest(
    @SerializedName("id") val id: String,
    /** "yyyy-MM-dd" — obligatoria si el beneficio pide fecha */
    @SerializedName("date") val date: String? = null,
    /** 1..50 — obligatorio si el beneficio pide pasajeros */
    @SerializedName("pax") val pax: Int? = null,
    @SerializedName("notes") val notes: String? = null
)

data class BlogPostsRequest(@SerializedName("posts") val posts: List<BlogPost>)

data class DiscountsRequest(@SerializedName("discounts") val discounts: List<Discount>)

data class MuellesRequest(@SerializedName("muelles") val muelles: List<Muelle>)

data class SellersRequest(@SerializedName("sellers") val sellers: List<Seller>)

data class GcalEmailRequest(@SerializedName("email") val email: String)

// ═══════════════════════════════════════════════════════
// RESPUESTAS
// ═══════════════════════════════════════════════════════

data class SuccessResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("error") val error: String? = null
)

data class ErrorResponse(@SerializedName("error") val error: String?)

data class UploadResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("url") val url: String? = null,
    /** true si el servidor lo convirtió a WebP */
    @SerializedName("optimized") val optimized: Boolean = false,
    @SerializedName("error") val error: String? = null
)

data class CreateReservationResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("id") val id: String? = null,
    @SerializedName("reservation") val reservation: Reservation? = null,
    @SerializedName("error") val error: String? = null
)

data class BenefitsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("benefits") val benefits: List<Benefit> = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class DiscountsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("discounts") val discounts: List<Discount> = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class SellersResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("sellers") val sellers: List<Seller> = emptyList(),
    /** Raíz pública para armar los enlaces de vendedor */
    @SerializedName("base") val base: String = "",
    @SerializedName("error") val error: String? = null
)

data class TourImage(
    @SerializedName("url") val url: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("mtime") val mtime: Long = 0,
    @SerializedName("optimized") val optimized: Boolean = false
)

data class EmailLogEntry(
    @SerializedName("at") val at: String = "",
    @SerializedName("reservationId") val reservationId: String = "",
    @SerializedName("to") val to: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("sent") val sent: Boolean = false,
    @SerializedName("error") val error: String? = null
)

data class SendTestEmailResponse(
    @SerializedName("sent") val sent: Boolean = false,
    @SerializedName("error") val error: String? = null
)
