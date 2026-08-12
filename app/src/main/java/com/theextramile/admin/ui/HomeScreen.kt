package com.theextramile.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.User
import com.theextramile.admin.ui.components.GlassCard
import com.theextramile.admin.ui.components.GradientAvatar
import com.theextramile.admin.ui.components.GradientIconBox
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.Roles
import com.theextramile.admin.util.Section
import com.theextramile.admin.util.SectionGroup

/**
 * Menú principal — el equivalente de admin-html/sidebar.html.
 *
 * Muestra las mismas secciones que el panel web y en el mismo orden, pero
 * filtradas por el rol: un Gestor de Reservas solo ve Resumen, Reservas y
 * Calendario; un Editor suma Planes, Extracto y Beneficios; el Super Admin
 * lo ve todo.
 */
@Composable
fun HomeScreen(
    user: User,
    onLogout: () -> Unit,
    onNavigate: (Section) -> Unit
) {
    val sections = remember(user.role) { Roles.sectionsFor(user.role) }
    var confirmLogout by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Cabecera con el usuario ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(user.name.ifBlank { user.email }, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Admin G",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        user.name.ifBlank { "Administrador" },
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(Roles.label(user.role), color = CyanLight, fontSize = 12.sp)
                }
                IconButton(onClick = { confirmLogout = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Secciones agrupadas, igual que el menú del panel ──
            SectionGroup.entries.forEach { group ->
                val groupSections = sections.filter { it.group == group }
                if (groupSections.isEmpty()) return@forEach

                Text(
                    group.title.uppercase(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
                groupSections.forEach { section ->
                    SectionRow(section) { onNavigate(section) }
                    Spacer(Modifier.height(9.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = BgLight,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("Tendrás que volver a entrar con tu correo y contraseña.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Cerrar sesión", color = OrangeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun SectionRow(section: Section, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIconBox(
                icon = iconFor(section),
                gradient = gradientFor(section),
                size = 42.dp,
                iconSize = 20.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    section.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(descriptionFor(section), color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun iconFor(section: Section): ImageVector = when (section) {
    Section.OVERVIEW -> Icons.Default.Dashboard
    Section.RESERVATIONS -> Icons.Default.EventNote
    Section.CALENDAR -> Icons.Default.CalendarMonth
    Section.TOURS -> Icons.Default.Sailing
    Section.PLAN_CONFIG -> Icons.Default.Tune
    Section.BENEFITS -> Icons.Default.CardGiftcard
    Section.BLOG -> Icons.Default.Article
    Section.SEO -> Icons.Default.TravelExplore
    Section.EXTRACTO -> Icons.Default.ReceiptLong
    Section.ACTIVITY -> Icons.Default.History
    Section.SETTINGS -> Icons.Default.Settings
    Section.USERS -> Icons.Default.Group
    Section.GCAL -> Icons.Default.Event
}

private fun descriptionFor(section: Section): String = when (section) {
    Section.OVERVIEW -> "Contadores y últimas reservas"
    Section.RESERVATIONS -> "Confirmar, cobrar y contactar"
    Section.CALENDAR -> "Las reservas día por día"
    Section.TOURS -> "Crear y editar los planes"
    Section.PLAN_CONFIG -> "Descuentos, muelles y vendedores"
    Section.BENEFITS -> "Catálogo y beneficios reservados"
    Section.BLOG -> "Entradas del blog del sitio"
    Section.SEO -> "Cómo se ve el sitio en Google"
    Section.EXTRACTO -> "Ventas con filtros y exportación"
    Section.ACTIVITY -> "Quién hizo qué en el panel"
    Section.SETTINGS -> "Configuración del sitio público"
    Section.USERS -> "Usuarios del panel y sus roles"
    Section.GCAL -> "Cuentas vinculadas del calendario"
}

private fun gradientFor(section: Section): Brush = when (section.group) {
    SectionGroup.GENERAL -> Gradients.BlueCyan
    SectionGroup.SITE -> Gradients.PurpleBlue
    SectionGroup.USERS -> Gradients.GreenCyan
}
