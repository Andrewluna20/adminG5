package com.theextramile.admin.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
 * CompositionLocal que expone el tema remoto.
 * (Disponible pero la mayoría de pantallas usa Color.kt directo
 *  porque las constantes ya son dinámicas).
 */
val LocalRemoteTheme = staticCompositionLocalOf { RemoteTheme() }

/**
 * Parsea un color hex (#RRGGBB) a Color de Compose.
 * Si falla, devuelve el color de fallback.
 */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF3B82F6)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Tema principal de Admin G con colores DINAMICOS.
 *
 * Cuando recibe un RemoteTheme nuevo, actualiza las variables
 * en Color.kt para que toda la app use los nuevos colores
 * automáticamente (gracias a mutableStateOf).
 */
@Composable
fun TEMAdminTheme(
    remoteTheme: RemoteTheme? = null,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val effectiveTheme = remoteTheme ?: RemoteTheme()

    // ★ APLICAR los colores del tema remoto a las variables globales
    //   Esto recompone toda la app cuando cambia el tema
    applyDynamicColors(
        bgDeep = parseHexColor(effectiveTheme.bgDeep, Color(0xFF050810)),
        bgMid = parseHexColor(effectiveTheme.bgMid, Color(0xFF0A1226)),
        bgLight = parseHexColor(effectiveTheme.bgLight, Color(0xFF101D38)),
        primary = parseHexColor(effectiveTheme.primary, Color(0xFF3B82F6)),
        primaryDark = parseHexColor(effectiveTheme.primaryDark, Color(0xFF1E40AF)),
        primaryLight = parseHexColor(effectiveTheme.primaryLight, Color(0xFF60A5FA)),
        secondary = parseHexColor(effectiveTheme.secondary, Color(0xFF06B6D4)),
        secondaryDark = parseHexColor(effectiveTheme.secondaryDark, Color(0xFF0891B2)),
        accent = parseHexColor(effectiveTheme.accent, Color(0xFF22D3EE)),
        success = parseHexColor(effectiveTheme.success, Color(0xFF10B981)),
        warning = parseHexColor(effectiveTheme.warning, Color(0xFFF59E0B)),
        danger = parseHexColor(effectiveTheme.danger, Color(0xFFEF4444))
    )

    val dynamicColorScheme = darkColorScheme(
        primary = Purple,
        onPrimary = TextPrimary,
        primaryContainer = PurpleDark,
        onPrimaryContainer = TextPrimary,

        secondary = Pink,
        onSecondary = TextPrimary,
        secondaryContainer = PinkDark,

        tertiary = CyanLight,
        onTertiary = TextPrimary,

        error = OrangeRed,
        onError = TextPrimary,
        errorContainer = Color(0x33EF4444),
        onErrorContainer = Color(0xFFF87171),

        background = BgDeep,
        onBackground = TextPrimary,
        surface = BgMid,
        onSurface = TextPrimary,
        surfaceVariant = GlassWhite,
        onSurfaceVariant = TextSecondary,

        outline = GlassBorder,
        outlineVariant = GlassBorderStrong,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDeep.toArgb()
            window.navigationBarColor = BgDeep.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalRemoteTheme provides effectiveTheme) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = TEMTypography,
            content = content
        )
    }
}
