package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Configuración del sitio (data/settings.json) — las 11 sub-secciones de
 * "Ajustes" del panel web viven todas aquí dentro.
 *
 * ⚠️ Se guarda con saveSettings mandando el objeto COMPLETO. Por eso la app
 * carga primero el settings del servidor y guarda una copia modificada con
 * copy(): si mandara solo los campos de una sub-sección, borraría el resto.
 *
 * ⚠️ Los secretos (boldSecret, recaptchaSecret) NO viven aquí — tienen sus
 * propias acciones saveBoldSecret / saveRecaptchaSecret y el servidor nunca
 * los devuelve.
 */
data class SiteSettings(
    // ═══════ Hero ═══════
    @SerializedName("heroTitle") val heroTitle: String = "",
    @SerializedName("heroSubtitle") val heroSubtitle: String = "",
    @SerializedName("mobileHeroEyebrow") val mobileHeroEyebrow: String = "",
    /** "gradient" | "image" | "video" | "color" */
    @SerializedName("heroBgType") val heroBgType: String = "gradient",
    @SerializedName("heroBgImage") val heroBgImage: String = "",
    @SerializedName("heroBgVideo") val heroBgVideo: String = "",
    @SerializedName("heroPosterImage") val heroPosterImage: String = "",
    @SerializedName("heroBgColor") val heroBgColor: String = "#0b2c5e",

    // ═══════ Textos de la sección de planes ═══════
    @SerializedName("toursLabel") val toursLabel: String = "",
    @SerializedName("toursTitle") val toursTitle: String = "",
    @SerializedName("toursLabelEn") val toursLabelEn: String = "",
    @SerializedName("toursTitleEn") val toursTitleEn: String = "",

    // ═══════ Marca ═══════
    @SerializedName("siteName") val siteName: String = "",
    @SerializedName("siteLogo") val siteLogo: String = "",
    /** Segundo logo, el de la barra sobre el hero */
    @SerializedName("siteLogoHero") val siteLogoHero: String = "",
    @SerializedName("siteBrandColor") val siteBrandColor: String = "#1a6274",
    @SerializedName("siteAccentColor") val siteAccentColor: String = "#a99447",
    @SerializedName("siteBrandFont") val siteBrandFont: String = "DM Sans",
    @SerializedName("faviconUrl") val faviconUrl: String = "",

    // ═══════ Colores del sitio público ═══════
    @SerializedName("heroGradientStart") val heroGradientStart: String = "#0b2c5e",
    @SerializedName("heroGradientMid") val heroGradientMid: String = "#1a4fa0",
    @SerializedName("heroGradientEnd") val heroGradientEnd: String = "#1e6fd4",
    @SerializedName("footerBg") val footerBg: String = "#0b2c5e",
    @SerializedName("footerText") val footerText: String = "#ffffff",
    @SerializedName("buttonBg") val buttonBg: String = "#0b2c5e",
    @SerializedName("buttonText") val buttonText: String = "#ffffff",
    @SerializedName("reserveBtnBg") val reserveBtnBg: String = "#0b2c5e",
    @SerializedName("reserveBtnText") val reserveBtnText: String = "#ffffff",

    // ═══════ Empresa (sale en la factura) ═══════
    @SerializedName("companyName") val companyName: String = "",
    @SerializedName("companyTagline") val companyTagline: String = "",
    @SerializedName("companyNit") val companyNit: String = "",
    @SerializedName("companyAddress") val companyAddress: String = "",
    @SerializedName("companyPhone") val companyPhone: String = "",
    @SerializedName("companyEmail") val companyEmail: String = "",
    @SerializedName("companyLogo") val companyLogo: String = "",

    // ═══════ WhatsApp ═══════
    @SerializedName("adminWhatsApp") val adminWhatsApp: String = "",
    @SerializedName("waMessage") val waMessage: String = "",
    @SerializedName("waPerPlan") val waPerPlan: Map<String, String>? = null,
    @SerializedName("whatsappDefaultMessage") val whatsappDefaultMessage: String = "",
    @SerializedName("whatsappCustomEnabled") val whatsappCustomEnabled: Boolean = false,
    @SerializedName("invoiceMessage") val invoiceMessage: String = "",

    // ═══════ Operador ═══════
    @SerializedName("operatorDefaultMessage") val operatorDefaultMessage: String = "",
    @SerializedName("operatorCustomEnabled") val operatorCustomEnabled: Boolean = false,

    // ═══════ Google Calendar ═══════
    /** "webhook" | "oauth" */
    @SerializedName("gcalMethod") val gcalMethod: String = "webhook",
    @SerializedName("gcalWebhookUrl") val gcalWebhookUrl: String = "",
    @SerializedName("calendarWebhook") val calendarWebhook: String = "",
    @SerializedName("gcalConnected") val gcalConnected: Boolean = false,
    @SerializedName("gcalTitleTemplate") val gcalTitleTemplate: String = "{name} - {tour}",
    @SerializedName("gcalDescTemplate") val gcalDescTemplate: String = "",

    // ═══════ SEO ═══════
    @SerializedName("seoTitle") val seoTitle: String = "",
    @SerializedName("seoDescription") val seoDescription: String = "",
    @SerializedName("seoKeywords") val seoKeywords: String = "",
    @SerializedName("seoImage") val seoImage: String = "",
    @SerializedName("seoTwitter") val seoTwitter: String = "",
    @SerializedName("seoSitemapEnabled") val seoSitemapEnabled: Boolean = true,
    @SerializedName("siteUrl") val siteUrl: String = "",
    @SerializedName("smartSearch") val smartSearch: SmartSearch? = null,

    // ═══════ Pasarela Bold ═══════
    @SerializedName("boldEnabled") val boldEnabled: Boolean = false,
    @SerializedName("boldApiKey") val boldApiKey: String = "",
    @SerializedName("boldTestMode") val boldTestMode: Boolean = false,
    /** "full" = cobra todo · "deposit" = cobra un porcentaje */
    @SerializedName("boldChargeMode") val boldChargeMode: String = "full",
    @SerializedName("boldDepositPercent") val boldDepositPercent: Int = 50,

    // ═══════ reCAPTCHA ═══════
    @SerializedName("recaptchaSiteKey") val recaptchaSiteKey: String = "",

    // ═══════ Correos ═══════
    @SerializedName("confirmEmailEnabled") val confirmEmailEnabled: Boolean = true,

    // ═══════ Política del tiquete ═══════
    @SerializedName("ticketPolicyShort") val ticketPolicyShort: String = "",
    @SerializedName("ticketPolicyFull") val ticketPolicyFull: String = "",

    @SerializedName("mapsApiKey") val mapsApiKey: String = "",
    @SerializedName("adminFcmTokens") val adminFcmTokens: List<String>? = null,

    // ═══════ Bancos reutilizables que cada plan marca ═══════
    /** tour.faqIds */
    @SerializedName("faqs") val faqs: List<Faq> = emptyList(),
    /** tour.infoIds */
    @SerializedName("infos") val infos: List<InfoBlock> = emptyList(),
    /** tour.scheduleIds — horarios informativos, NO seleccionables al reservar */
    @SerializedName("schedules") val schedules: List<ScheduleNote> = emptyList(),
    /** tour.tagIds */
    @SerializedName("planTags") val planTags: List<PlanTag> = emptyList()
) {
    val boldChargesDeposit: Boolean get() = boldChargeMode == "deposit"
}

/** Pregunta frecuente reutilizable */
data class Faq(
    @SerializedName("id") val id: String = "",
    @SerializedName("q") val q: String = "",
    @SerializedName("a") val a: String = "",
    @SerializedName("en") val en: FaqTranslation? = null
)

data class FaqTranslation(
    @SerializedName("q") val q: String = "",
    @SerializedName("a") val a: String = ""
)

/** Bloque de información ("Qué llevar", etc.) */
data class InfoBlock(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("en") val en: InfoTranslation? = null
)

data class InfoTranslation(
    @SerializedName("title") val title: String = "",
    @SerializedName("text") val text: String = ""
)

/** Horario informativo (texto libre) */
data class ScheduleNote(
    @SerializedName("id") val id: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("en") val en: ScheduleTranslation? = null
)

data class ScheduleTranslation(
    @SerializedName("text") val text: String = ""
)

/** Etiqueta de color para los planes */
data class PlanTag(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("color") val color: String = "#1a6274",
    @SerializedName("en") val en: PlanTagTranslation? = null
)

data class PlanTagTranslation(
    @SerializedName("name") val name: String = ""
)

/** Buscador inteligente del sitio público */
data class SmartSearch(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("title") val title: String = "",
    @SerializedName("placeholder") val placeholder: String = "",
    @SerializedName("entries") val entries: List<SmartSearchEntry> = emptyList()
)

data class SmartSearchEntry(
    /** "tour" | "blog" | … */
    @SerializedName("type") val type: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("phrases") val phrases: List<String> = emptyList()
)
