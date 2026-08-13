package com.theextramile.admin.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.theextramile.admin.data.model.RemoteTheme

/**
 * CompositionLocal con el tema remoto, por si alguna pantalla necesita
 * leer algo más que los colores (nombre de la app, logo…).
 */
val LocalRemoteTheme = staticCompositionLocalOf { RemoteTheme() }

/**
 * Parsea un color hex (#RRGGBB) a Color de Compose.
 * Si el hex viene mal, se queda con el de reserva.
 */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF2563EB)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Tema de Admin G — CLARO.
 *
 * Los fondos, los textos y los bordes son fijos (ver Color.kt). Del tema
 * remoto del HUB solo se toman los acentos de marca: así la app no se
 * vuelve oscura sola si el JSON del HUB todavía trae la paleta antigua.
 */
@Composable
fun TEMAdminTheme(
    remoteTheme: RemoteTheme? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val effectiveTheme = remoteTheme ?: RemoteTheme()

    // Solo los acentos; los fondos del RemoteTheme se ignoran a propósito
    applyDynamicColors(
        primary = parseHexColor(effectiveTheme.primary, Color(0xFF2563EB)),
        primaryDark = parseHexColor(effectiveTheme.primaryDark, Color(0xFF1D4ED8)),
        primaryLight = parseHexColor(effectiveTheme.primaryLight, Color(0xFF3B82F6)),
        secondary = parseHexColor(effectiveTheme.secondary, Color(0xFF0891B2)),
        secondaryDark = parseHexColor(effectiveTheme.secondaryDark, Color(0xFF0E7490)),
        accent = parseHexColor(effectiveTheme.accent, Color(0xFF0E7490)),
        success = parseHexColor(effectiveTheme.success, Color(0xFF047857)),
        warning = parseHexColor(effectiveTheme.warning, Color(0xFFB45309)),
        danger = parseHexColor(effectiveTheme.danger, Color(0xFFDC2626))
    )

    val colorScheme = lightColorScheme(
        primary = Purple,
        onPrimary = TextOnAccent,
        primaryContainer = PurpleLight,
        onPrimaryContainer = TextOnAccent,

        secondary = Pink,
        onSecondary = TextOnAccent,
        secondaryContainer = PinkDark,

        tertiary = CyanLight,
        onTertiary = TextOnAccent,

        error = OrangeRed,
        onError = TextOnAccent,
        errorContainer = Color(0x1FDC2626),
        onErrorContainer = Color(0xFFB91C1C),

        background = BgDeep,
        onBackground = TextPrimary,
        surface = BgMid,
        onSurface = TextPrimary,
        surfaceVariant = GlassWhite2,
        onSurfaceVariant = TextSecondary,

        outline = GlassBorder,
        outlineVariant = GlassBorderStrong,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDeep.toArgb()
            window.navigationBarColor = BgDeep.toArgb()
            // Barras claras → los iconos del sistema (hora, batería) van oscuros
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(LocalRemoteTheme provides effectiveTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TEMTypography,
            content = content
        )
    }
}
