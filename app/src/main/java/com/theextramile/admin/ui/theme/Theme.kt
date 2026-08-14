package com.theextramile.admin.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema de Admin K — CLARO y FIJO.
 *
 * Toda la paleta está en Color.kt y se compila dentro de la app: no se
 * descarga nada, así que abrir la app sin datos se ve igual que con
 * ellos y una actualización del servidor no puede cambiarle los colores.
 *
 * ⚠️ Antes esto leía un "tema remoto" de gpanelcol.online (el HUB):
 * un mobile_theme.json que repintaba la app entera. Se quitó porque ese
 * archivo es UNO SOLO para todas las apps del sistema y manda el morado
 * #902fa8, que no es la marca de este sitio. Si algún día se quisiera
 * volver a mandar el color desde el servidor, ese JSON tendría que
 * servir un tono por app, no uno para todas.
 */
@Composable
fun TEMAdminTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

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
        errorContainer = Color(0x14C0402C),
        onErrorContainer = Color(0xFFC0402C),

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TEMTypography,
        content = content
    )
}
