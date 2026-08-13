package com.theextramile.admin.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ═══════════════════════════════════════════════════════
   Admin G — Paleta CLARA

   Los nombres de los tokens son los mismos de siempre (BgDeep,
   TextPrimary, GlassWhite…) para que las pantallas no cambien: lo que
   cambió son los valores. BgDeep ya no es "el más oscuro" sino "el fondo
   de página", y TextPrimary ya no es blanco sino casi negro.

   ⚠️ Fondos, textos y bordes son FIJOS. El tema remoto del HUB solo
   controla los acentos (ver applyDynamicColors más abajo): si también
   mandara los fondos, la app volvería a ponerse oscura sola.
   ═══════════════════════════════════════════════════════ */

// ═══════ FONDOS ═══════
/** Fondo de página: un gris muy claro para que las tarjetas blancas se vean */
var BgDeep by mutableStateOf(Color(0xFFF4F6F9))
    private set

/** Superficies: hojas inferiores y tarjetas */
var BgMid by mutableStateOf(Color(0xFFFFFFFF))
    private set

/** Diálogos y la barra de guardar */
var BgLight by mutableStateOf(Color(0xFFFFFFFF))
    private set

// ═══════ ACENTOS PRINCIPALES ═══════
// Van más oscuros que en el tema anterior: sobre blanco, un azul claro no
// se lee. Todos cumplen contraste suficiente sobre fondo claro.
var Purple by mutableStateOf(Color(0xFF2563EB))
    private set
var PurpleDark by mutableStateOf(Color(0xFF1D4ED8))
    private set
var PurpleLight by mutableStateOf(Color(0xFF3B82F6))
    private set
var Pink by mutableStateOf(Color(0xFF0891B2))
    private set
var PinkDark by mutableStateOf(Color(0xFF0E7490))
    private set

// ═══════ ACENTOS SECUNDARIOS ═══════
var BlueElectric by mutableStateOf(Color(0xFF2563EB))
    private set
var BlueDeep by mutableStateOf(Color(0xFF1E40AF))
    private set
var Cyan by mutableStateOf(Color(0xFF0891B2))
    private set

/** El acento que más se usa como texto (enlaces, títulos de sección) */
var CyanLight by mutableStateOf(Color(0xFF0E7490))
    private set
var CyanBright by mutableStateOf(Color(0xFF0891B2))
    private set

// ═══════ ACENTOS TERCIARIOS ═══════
var OrangeWarm by mutableStateOf(Color(0xFFD97706))
    private set
var OrangeRed by mutableStateOf(Color(0xFFDC2626))
    private set
var GreenNeon by mutableStateOf(Color(0xFF059669))
    private set
var GreenLight by mutableStateOf(Color(0xFF047857))
    private set
var Yellow by mutableStateOf(Color(0xFFB45309))
    private set

// ═══════ TEXTO ═══════
// Constantes: sobre fondo claro el texto siempre va oscuro.
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF64748B)
val TextDim = Color(0xFF94A3B8)

/**
 * Texto e iconos que van ENCIMA de un color o gradiente (botones con
 * gradiente, avatares, cuadros de icono). Ahí sigue mandando el blanco:
 * si se usara TextPrimary, quedaría casi negro sobre azul.
 */
val TextOnAccent = Color(0xFFFFFFFF)

// ═══════ TARJETAS Y BORDES ═══════
/** Relleno de tarjeta: blanco sólido sobre el gris del fondo */
val GlassWhite = Color(0xFFFFFFFF)

/** Relleno de los campos de texto, para distinguirlos de la tarjeta */
val GlassWhite2 = Color(0xFFF1F4F8)

val GlassBorder = Color(0x14000000)
val GlassBorderStrong = Color(0x29000000)

// ═══════ ESTADOS DE RESERVA ═══════
var StatusPendingBg by mutableStateOf(Color(0x1FD97706))
    private set
var StatusPendingText by mutableStateOf(Color(0xFFB45309))
    private set
var StatusConfirmedBg by mutableStateOf(Color(0x1F059669))
    private set
var StatusConfirmedText by mutableStateOf(Color(0xFF047857))
    private set
var StatusCancelledBg by mutableStateOf(Color(0x1FDC2626))
    private set
var StatusCancelledText by mutableStateOf(Color(0xFFB91C1C))
    private set

// WhatsApp (constante, es su verde de marca)
val WhatsApp = Color(0xFF25D366)

/**
 * Aplica los acentos del tema remoto del HUB.
 *
 * Ojo: ya NO recibe los fondos. El tema claro es fijo; el HUB solo puede
 * cambiar los colores de marca. Si el HUB manda colores muy claros, el
 * texto sobre blanco pierde contraste — por eso conviene mandar tonos
 * medios u oscuros (600/700 de una escala tipo Tailwind).
 */
internal fun applyDynamicColors(
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
    Purple = primary
    PurpleDark = primaryDark
    PurpleLight = primaryLight
    BlueElectric = primary
    BlueDeep = primaryDark

    Pink = secondary
    PinkDark = secondaryDark
    Cyan = secondary

    CyanLight = accent
    CyanBright = accent

    GreenNeon = success
    GreenLight = success
    StatusConfirmedBg = success.copy(alpha = 0.12f)
    StatusConfirmedText = success

    OrangeWarm = warning
    Yellow = warning
    StatusPendingBg = warning.copy(alpha = 0.12f)
    StatusPendingText = warning

    OrangeRed = danger
    StatusCancelledBg = danger.copy(alpha = 0.12f)
    StatusCancelledText = danger
}

/* ═══════════════════════════════════════════════════════
   Gradientes

   Se recalculan solos cuando cambian los acentos, porque Compose observa
   las lecturas de mutableState.
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

    /** Fondo principal: un blanco apenas degradado, no un color plano */
    val Background: Brush
        get() = Brush.verticalGradient(colors = listOf(BgDeep, BgMid, BgDeep))

    /** Fondo del login — el mismo tono claro, sin el azul oscuro de antes */
    val LoginBackground: Brush
        get() = Brush.verticalGradient(colors = listOf(BgMid, BgDeep, BgMid))

    /** Velo sutil para superponer sobre tarjetas */
    val Glass = Brush.linearGradient(
        colors = listOf(Color(0x0A000000), Color(0x03000000))
    )

    /** Gradiente único por avatar, a partir del nombre */
    fun forAvatar(seed: String): Brush {
        val gradients = listOf(
            Brush.linearGradient(listOf(BlueElectric, Cyan)),
            Brush.linearGradient(listOf(BlueDeep, BlueElectric)),
            Brush.linearGradient(listOf(Cyan, CyanBright)),
            Brush.linearGradient(listOf(GreenNeon, Cyan)),
            Brush.linearGradient(listOf(BlueElectric, GreenNeon)),
            Brush.linearGradient(listOf(Color(0xFF4F46E5), BlueElectric)),
            Brush.linearGradient(listOf(Cyan, BlueDeep)),
        )
        val hash = seed.fold(0) { acc, c -> acc + c.code }
        return gradients[hash % gradients.size]
    }
}
