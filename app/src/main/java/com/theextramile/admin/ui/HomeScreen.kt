package com.theextramile.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.User
import com.theextramile.admin.ui.components.SoftIconBox
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.Roles
import com.theextramile.admin.util.Section

/**
 * Pantalla principal, rediseñada a partir de la referencia que mandó el
 * usuario: barra inferior con botón flotante en el centro, cabecera con
 * avatar y buscador, título grande, y tarjetas con sombra suave que se
 * pueden ver en cuadrícula o en lista.
 *
 * La barra inferior lleva solo el día a día (Resumen, Reservas,
 * Calendario) y un cuarto botón "Más" con todo lo demás: son 13
 * secciones y en una barra no caben.
 *
 * ⚠️ El rediseño está contenido en esta pantalla a propósito. Cambiar la
 * navegación de toda la app obligaría a tocar las 13 secciones; así se
 * consigue el aspecto de la referencia sin arriesgar lo que ya funciona.
 */
@Composable
fun HomeScreen(
    user: User,
    onLogout: () -> Unit,
    onNavigate: (Section) -> Unit
) {
    val visibles = remember(user.role) { Roles.sectionsFor(user.role) }

    var enCuadricula by remember { mutableStateOf(true) }
    var busqueda by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var mostrarCrear by remember { mutableStateOf(false) }

    // Las cuatro del día a día van en la barra; el resto viven en "Más".
    // Se reparten dos a cada lado del botón +. Ojo: Planes no lo ve el rol
    // 'reservations', y entonces la barra se queda en tres — el filtro y el
    // reparto de BarraInferior ya lo contemplan.
    val enBarra = listOf(
        Section.OVERVIEW, Section.RESERVATIONS, Section.CALENDAR, Section.TOURS
    ).filter { it in visibles }

    val listadas = remember(visibles, busqueda) {
        val q = busqueda.trim().lowercase()
        if (q.isBlank()) visibles
        else visibles.filter {
            it.title.lowercase().contains(q) || descripcionDe(it).lowercase().contains(q)
        }
    }

    Scaffold(
        containerColor = BgDeep,
        bottomBar = {
            BarraInferior(
                secciones = enBarra,
                onNavigate = onNavigate,
                onCrear = { mostrarCrear = true }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Cabecera: avatar, campana y buscador ──
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Purple)
                        .clickable { confirmarSalida = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.name.take(1).uppercase().ifBlank { "K" },
                        color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 17.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { buscando = !buscando }) {
                    Icon(
                        if (buscando) Icons.Default.Close else Icons.Default.Search,
                        "Buscar", tint = TextPrimary, modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { confirmarSalida = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        "Cerrar sesión", tint = TextPrimary, modifier = Modifier.size(21.dp)
                    )
                }
            }

            // ── Título grande + interruptor cuadrícula/lista ──
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        user.name.ifBlank { "Administrador" },
                        color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(Roles.label(user.role), color = TextMuted, fontSize = 13.sp)
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Purple)
                        .clickable { enCuadricula = !enCuadricula },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (enCuadricula) Icons.Default.ViewList else Icons.Default.GridView,
                        "Cambiar vista", tint = TextOnAccent, modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (buscando) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar sección…", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        cursorColor = Purple,
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = GlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            if (listadas.isEmpty()) {
                Text(
                    "No hay ninguna sección con ese nombre",
                    color = TextMuted, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (enCuadricula) 2 else 1),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listadas, key = { it.key }) { section ->
                        TarjetaSeccion(
                            section = section,
                            compacta = enCuadricula,
                            onClick = { onNavigate(section) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarCrear) {
        HojaCrear(
            visibles = visibles,
            onElegir = { s -> mostrarCrear = false; onNavigate(s) },
            onDismiss = { mostrarCrear = false }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = BgLight,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("Tendrás que volver a entrar con tu correo y contraseña.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onLogout() }) {
                    Text("Cerrar sesión", color = OrangeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

/** Tarjeta blanca con sombra suave, como las de la referencia */
@Composable
private fun TarjetaSeccion(
    section: Section,
    compacta: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x14000000))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        if (compacta) {
            Column(Modifier.padding(16.dp)) {
                SoftIconBox(icon = iconoDe(section), tint = Purple)
                Spacer(Modifier.height(12.dp))
                Text(
                    section.title,
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    descripcionDe(section),
                    color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftIconBox(icon = iconoDe(section), tint = Purple)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        section.title,
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(descripcionDe(section), color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Barra inferior con el botón flotante encajado en el centro.
 *
 * Se monta a mano en vez de usar NavigationBar + FloatingActionButton
 * porque el botón tiene que quedar POR ENCIMA de la barra, y con el
 * Scaffold estándar queda al lado.
 */
@Composable
private fun BarraInferior(
    secciones: List<Section>,
    onNavigate: (Section) -> Unit,
    onCrear: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(88.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(66.dp),
            color = BgMid,
            shadowElevation = 16.dp
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mitad = (secciones.size + 1) / 2
                secciones.take(mitad).forEach {
                    BotonBarra(it, Modifier.weight(1f), onNavigate)
                }
                // hueco central para el botón flotante
                Spacer(Modifier.width(72.dp))
                secciones.drop(mitad).forEach {
                    BotonBarra(it, Modifier.weight(1f), onNavigate)
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(58.dp)
                .shadow(10.dp, CircleShape, spotColor = Purple.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(Purple)
                .clickable(onClick = onCrear),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, "Crear", tint = TextOnAccent, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun BotonBarra(
    section: Section,
    modifier: Modifier,
    onNavigate: (Section) -> Unit
) {
    Column(
        modifier = modifier.clickable { onNavigate(section) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(iconoDe(section), section.title, tint = Purple, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(3.dp))
        Text(section.title, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
    }
}

/** El botón + abre las secciones donde de verdad se puede crear algo */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaCrear(
    visibles: List<Section>,
    onElegir: (Section) -> Unit,
    onDismiss: () -> Unit
) {
    val creables = listOf(
        Section.TOURS, Section.BENEFITS, Section.BLOG, Section.PLAN_CONFIG
    ).filter { it in visibles }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDim) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Crear", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (creables.isEmpty()) "Tu rol no permite crear elementos del sitio."
                else "¿Qué quieres añadir?",
                color = TextMuted, fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            creables.forEach { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onElegir(s) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftIconBox(icon = iconoDe(s), tint = Purple)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(s.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(descripcionDe(s), color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun iconoDe(section: Section): ImageVector = when (section) {
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

private fun descripcionDe(section: Section): String = when (section) {
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
    Section.GCAL -> "Cuentas del calendario"
}
