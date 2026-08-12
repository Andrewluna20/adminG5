package com.theextramile.admin.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ═══════════════════════════════════════════════════════
   Admin G — Paleta DINAMICA (controlable desde el HUB)

   Las constantes se inicializan con valores por defecto
   (azul eléctrico + cyan), pero TEMAdminTheme las actualiza
   cuando el RemoteTheme cambia.

   Por eso son `var` y no `val`.
   ═══════════════════════════════════════════════════════ */

// ═══════ FONDOS (mutables) ═══════
var BgDeep by mutableStateOf(Color(0xFF050810))
    private set
var BgMid by mutableStateOf(Color(0xFF0A1226))
    private set
var BgLight by mutableStateOf(Color(0xFF101D38))
    private set

// ═══════ ACENTOS PRINCIPALES ═══════
var Purple by mutableStateOf(Color(0xFF3B82F6))
    private set
var PurpleDark by mutableStateOf(Color(0xFF1E40AF))
    private set
var PurpleLight by mutableStateOf(Color(0xFF60A5FA))
    private set
var Pink by mutableStateOf(Color(0xFF06B6D4))
    private set
var PinkDark by mutableStateOf(Color(0xFF0891B2))
    private set

// ═══════ ACENTOS SECUNDARIOS ═══════
var BlueElectric by mutableStateOf(Color(0xFF3B82F6))
    private set
var BlueDeep by mutableStateOf(Color(0xFF1E3A8A))
    private set
var Cyan by mutableStateOf(Color(0xFF06B6D4))
    private set
var CyanLight by mutableStateOf(Color(0xFF22D3EE))
    private set
var CyanBright by mutableStateOf(Color(0xFF67E8F9))
    private set

// ═══════ ACENTOS TERCIARIOS ═══════
var OrangeWarm by mutableStateOf(Color(0xFFF59E0B))
    private set
var OrangeRed by mutableStateOf(Color(0xFFEF4444))
    private set
var GreenNeon by mutableStateOf(Color(0xFF10B981))
    private set
var GreenLight by mutableStateOf(Color(0xFF34D399))
    private set
var Yellow by mutableStateOf(Color(0xFFFBBF24))
    private set

// ═══════ TEXTO (constante - blanco siempre va sobre oscuro) ═══════
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA8B4CC)
val TextMuted = Color(0xFF6B7891)
val TextDim = Color(0xFF4A5670)

// ═══════ CARDS / GLASS ═══════
val GlassWhite = Color(0x14FFFFFF)
val GlassWhite2 = Color(0x0AFFFFFF)
val GlassBorder = Color(0x1AFFFFFF)
val GlassBorderStrong = Color(0x33FFFFFF)

// ═══════ ESTADOS DE RESERVA ═══════
var StatusPendingBg by mutableStateOf(Color(0x33FBBF24))
    private set
var StatusPendingText by mutableStateOf(Color(0xFFFBBF24))
    private set
var StatusConfirmedBg by mutableStateOf(Color(0x3310B981))
    private set
var StatusConfirmedText by mutableStateOf(Color(0xFF34D399))
    private set
var StatusCancelledBg by mutableStateOf(Color(0x33EF4444))
    private set
var StatusCancelledText by mutableStateOf(Color(0xFFF87171))
    private set

// WhatsApp (constante)
val WhatsApp = Color(0xFF25D366)

/**
 * Actualiza todas las constantes de color desde el tema remoto.
 * Llamado por TEMAdminTheme cuando cambia el RemoteTheme.
 */
internal fun applyDynamicColors(
    bgDeep: Color,
    bgMid: Color,
    bgLight: Color,
    primary: Color,
    primaryDark: Color,
    primaryLight: Color,
    secondary: Color,
    secondaryDark: Color,
    accent: Color,
    success: Color,
    warning: Color,
    danger: Color
) {
    BgDeep = bgDeep
    BgMid = bgMid
    BgLight = bgLight

    // Primary mapea a Purple/BlueElectric (mismo color)
    Purple = primary
    PurpleDark = primaryDark
    PurpleLight = primaryLight
    BlueElectric = primary
    BlueDeep = primaryDark

    // Secondary mapea a Pink/Cyan
    Pink = secondary
    PinkDark = secondaryDark
    Cyan = secondary

    // Accent
    CyanLight = accent
    CyanBright = accent

    // Estados (con alpha 0x33 para los backgrounds = 20% de opacidad)
    GreenNeon = success
    GreenLight = success
    StatusConfirmedBg = success.copy(alpha = 0.2f)
    StatusConfirmedText = success

    OrangeWarm = warning
    Yellow = warning
    StatusPendingBg = warning.copy(alpha = 0.2f)
    StatusPendingText = warning

    OrangeRed = danger
    StatusCancelledBg = danger.copy(alpha = 0.2f)
    StatusCancelledText = danger
}

/* ═══════════════════════════════════════════════════════
   Gradientes DINAMICOS (recomputados cuando cambian los colores)

   Compose tracks reads de mutableState, así que cuando los colores
   cambian, los componentes que usen Gradients.X se re-componen
   automáticamente.
   ═══════════════════════════════════════════════════════ */

object Gradients {
    val PurplePink: Brush
        get() = Brush.linearGradient(listOf(BlueElectric, Cyan))

    val BlueCyan: Brush
        get() = Brush.linearGradient(listOf(BlueElectric, Cyan))

    val OrangePink: Brush
        get() = Brush.linearGradient(listOf(OrangeWarm, OrangeRed))

    val GreenCyan: Brush
        get() = Brush.linearGradient(listOf(GreenNeon, Cyan))

    val PurpleBlue: Brush
        get() = Brush.linearGradient(listOf(BlueDeep, BlueElectric))

    val PinkOrange: Brush
        get() = Brush.linearGradient(listOf(Cyan, BlueElectric))

    /** Fondo principal de la app — usa los colores dinámicos */
    val Background: Brush
        get() = Brush.verticalGradient(
            colors = listOf(BgDeep, BgMid, BgDeep)
        )

    /** Fondo del login (más dramático) */
    val LoginBackground: Brush
        get() = Brush.verticalGradient(
            colors = listOf(BgDeep, BgMid, BlueDeep)
        )

    val Glass = Brush.linearGradient(
        colors = listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))
    )

    /** Gradiente único para avatar basado en string hash */
    fun forAvatar(seed: String): Brush {
        val gradients = listOf(
            Brush.linearGradient(listOf(BlueElectric, Cyan)),
            Brush.linearGradient(listOf(BlueDeep, BlueElectric)),
            Brush.linearGradient(listOf(Cyan, CyanBright)),
            Brush.linearGradient(listOf(GreenNeon, Cyan)),
            Brush.linearGradient(listOf(BlueElectric, GreenNeon)),
            Brush.linearGradient(listOf(Color(0xFF6366F1), BlueElectric)),
            Brush.linearGradient(listOf(Cyan, BlueDeep)),
        )
        val hash = seed.fold(0) { acc, c -> acc + c.code }
        return gradients[hash % gradients.size]
    }
}
