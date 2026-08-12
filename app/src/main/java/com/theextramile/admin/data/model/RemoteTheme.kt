package com.theextramile.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * Tema visual de la app móvil, configurable desde g-panel.html (HUB).
 * URL: https://gpanelcol.online/data/mobile_theme.json
 *
 * Si no se puede descargar (offline o no existe), se usa el tema por defecto
 * en ui/theme/Color.kt
 */
data class RemoteTheme(
    @SerializedName("version") val version: String = "1.0",

    // === COLORES BASE ===
    /** Fondo principal de la app (oscuro azulado por defecto) */
    @SerializedName("bgDeep") val bgDeep: String = "#050810",
    @SerializedName("bgMid") val bgMid: String = "#0A1226",
    @SerializedName("bgLight") val bgLight: String = "#101D38",

    // === COLORES DE ACENTO ===
    /** Color principal (azul eléctrico por defecto) */
    @SerializedName("primary") val primary: String = "#3B82F6",
    @SerializedName("primaryDark") val primaryDark: String = "#1E40AF",
    @SerializedName("primaryLight") val primaryLight: String = "#60A5FA",

    /** Color secundario (cyan por defecto) */
    @SerializedName("secondary") val secondary: String = "#06B6D4",
    @SerializedName("secondaryDark") val secondaryDark: String = "#0891B2",

    /** Color terciario / acento (cyan brillante) */
    @SerializedName("accent") val accent: String = "#22D3EE",

    // === ESTADO ===
    @SerializedName("success") val success: String = "#10B981",
    @SerializedName("warning") val warning: String = "#F59E0B",
    @SerializedName("danger") val danger: String = "#EF4444",
    @SerializedName("info") val info: String = "#3B82F6",

    // === TEXTO ===
    @SerializedName("textPrimary") val textPrimary: String = "#FFFFFF",
    @SerializedName("textSecondary") val textSecondary: String = "#A8B4CC",
    @SerializedName("textMuted") val textMuted: String = "#6B7891",

    // === CARDS / GLASSMORPHISM ===
    /** Intensidad del glass (0.0 - 1.0). Mayor = más opaco */
    @SerializedName("glassIntensity") val glassIntensity: Double = 0.08,
    @SerializedName("borderRadius") val borderRadius: Int = 20,

    // === GRADIENTE PRINCIPAL ===
    /** Color inicio del gradiente principal */
    @SerializedName("gradientStart") val gradientStart: String = "#3B82F6",
    /** Color fin del gradiente principal */
    @SerializedName("gradientEnd") val gradientEnd: String = "#06B6D4",

    // === GRADIENTE DE FONDO ===
    @SerializedName("bgGradientStart") val bgGradientStart: String = "#050810",
    @SerializedName("bgGradientMid") val bgGradientMid: String = "#0A1738",
    @SerializedName("bgGradientEnd") val bgGradientEnd: String = "#050810",

    // === TIPOGRAFÍA ===
    /** Tamaño base de texto (en sp). Defecto 14. */
    @SerializedName("fontSizeBase") val fontSizeBase: Int = 14,
    /** Tamaño de títulos grandes */
    @SerializedName("fontSizeHeading") val fontSizeHeading: Int = 24,

    // === ESTILO DE CARDS ===
    /** "glass" (translúcido) | "solid" (color sólido) | "outlined" (solo borde) */
    @SerializedName("cardStyle") val cardStyle: String = "glass",

    // === BRANDING ===
    @SerializedName("appName") val appName: String = "Admin G",
    @SerializedName("logoUrl") val logoUrl: String = "",

    // === META ===
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false,
    @SerializedName("forceUpdateMessage") val forceUpdateMessage: String = ""
)
