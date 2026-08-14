package com.theextramile.admin.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ═══════════════════════════════════════════════════════
   Admin G — Paleta CLARA

   Los nombres de los tokens son los mismos de siempre (BgDeep,
   TextPrimary, GlassWhite…) para que las pantallas no cambien: lo que
   cambió son los valores. BgDeep ya no es "el más oscuro" sino "el fondo
   de página", y TextPrimary ya no es blanco sino casi negro.

   ⚠️ TODOS los colores son constantes: se compilan dentro de la app y
   nada de fuera los cambia. Antes eran variables que un tema remoto del
   HUB podía reescribir en marcha (había un applyDynamicColors aquí); eso
   se quitó porque ese JSON es UNO SOLO para todas las apps del sistema y
   manda un morado que no es la marca de este sitio. Para cambiar un
   color se cambia aquí y se recompila, y no hay más sitio donde mirar.
   ═══════════════════════════════════════════════════════ */

// ═══════ FONDOS ═══════
/** Fondo de página: un gris muy claro para que las tarjetas blancas se vean */
val BgDeep = Color(0xFFF4F6F9)

/** Superficies: hojas inferiores y tarjetas */
val BgMid = Color(0xFFFFFFFF)

/** Diálogos y la barra de guardar */
val BgLight = Color(0xFFFFFFFF)

// ═══════ ACENTOS PRINCIPALES ═══════
// Van más oscuros que en el tema anterior: sobre blanco, un azul claro no
// se lee. Todos cumplen contraste suficiente sobre fondo claro.
val Purple = Color(0xFF2563EB)
val PurpleDark = Color(0xFF1D4ED8)
val PurpleLight = Color(0xFF3B82F6)
val Pink = Color(0xFF0891B2)
val PinkDark = Color(0xFF0E7490)

// ═══════ ACENTOS SECUNDARIOS ═══════
val BlueElectric = Color(0xFF2563EB)
val BlueDeep = Color(0xFF1E40AF)
val Cyan = Color(0xFF0891B2)

/** El acento que más se usa como texto (enlaces, títulos de sección) */
val CyanLight = Color(0xFF0E7490)
val CyanBright = Color(0xFF0891B2)

// ═══════ ACENTOS TERCIARIOS ═══════
val OrangeWarm = Color(0xFFD97706)
val OrangeRed = Color(0xFFDC2626)
val GreenNeon = Color(0xFF059669)
val GreenLight = Color(0xFF047857)
val Yellow = Color(0xFFB45309)

// ═══════ TEXTO ═══════
// Sobre fondo claro el texto siempre va oscuro.
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
// El fondo es el mismo tono del texto al 12%: la etiqueta se lee sin
// convertirse en un bloque de color.
val StatusPendingBg = Color(0x1FD97706)
val StatusPendingText = Color(0xFFB45309)
val StatusConfirmedBg = Color(0x1F059669)
val StatusConfirmedText = Color(0xFF047857)
val StatusCancelledBg = Color(0x1FDC2626)
val StatusCancelledText = Color(0xFFB91C1C)

// WhatsApp (constante, es su verde de marca)
val WhatsApp = Color(0xFF25D366)

/* ═══════════════════════════════════════════════════════
   Gradientes

   Admin G sí usa degradados de verdad (a diferencia de Admin K, que los
   dejó planos). Ya no hacen falta accesores get(): los colores son
   constantes, así que cada Brush se construye una sola vez.
   ═══════════════════════════════════════════════════════ */

object Gradients {
    val PurplePink: Brush = Brush.linearGradient(listOf(BlueElectric, Cyan))
    val BlueCyan: Brush = Brush.linearGradient(listOf(BlueElectric, Cyan))
    val OrangePink: Brush = Brush.linearGradient(listOf(OrangeWarm, OrangeRed))
    val GreenCyan: Brush = Brush.linearGradient(listOf(GreenNeon, Cyan))
    val PurpleBlue: Brush = Brush.linearGradient(listOf(BlueDeep, BlueElectric))
    val PinkOrange: Brush = Brush.linearGradient(listOf(Cyan, BlueElectric))

    /** Fondo principal: un blanco apenas degradado, no un color plano */
    val Background: Brush = Brush.verticalGradient(colors = listOf(BgDeep, BgMid, BgDeep))

    /** Fondo del login — el mismo tono claro, sin el azul oscuro de antes */
    val LoginBackground: Brush = Brush.verticalGradient(colors = listOf(BgMid, BgDeep, BgMid))

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
