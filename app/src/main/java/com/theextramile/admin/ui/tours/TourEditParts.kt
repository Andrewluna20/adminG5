package com.theextramile.admin.ui.tours

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.api.TourImage
import com.theextramile.admin.ui.blog.absoluteUrl
import com.theextramile.admin.ui.components.FlowRowSimple
import com.theextramile.admin.ui.components.adminFieldColors
import com.theextramile.admin.ui.theme.*

/* ═══════════════════════════════════════════════════════
   Piezas propias del editor de planes.

   El plan tiene una treintena de campos: de una tirada no se lee. Aquí
   están los bloques que lo parten en secciones plegables y los que
   manejan sus imágenes.

   Los controles genéricos (desplegables, fecha, hora, etiquetas) están
   en ui/components/FormParts.kt, porque Reservas usa los mismos.
   ═══════════════════════════════════════════════════════ */

/**
 * Sección plegable del editor.
 *
 * Empiezan cerradas menos la primera: abrir el editor y encontrarse
 * treinta campos de golpe no ayuda a nadie.
 */
@Composable
fun EditorSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    /** Resumen a la derecha del título: "3 fotos", "sin horarios"… */
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (!summary.isNullOrBlank()) {
                Text(summary, color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (open) "Plegar" else "Desplegar",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { content() }
        }
    }
}

/**
 * Marcar varios de un banco (etiquetas, FAQ, información, horarios).
 *
 * Los bancos se crean en otras secciones del panel; aquí solo se marcan.
 * Si el banco está vacío se dice dónde se llena, que si no parece un fallo.
 */
@Composable
fun ChipMultiSelect(
    options: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    emptyHint: String,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) {
        Text(
            emptyHint,
            color = TextDim,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    FlowRowSimple(modifier.padding(vertical = 4.dp)) {
        options.forEach { (id, text) ->
            val on = id in selected
            Surface(
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp).clickable { onToggle(id) },
                shape = RoundedCornerShape(50),
                color = if (on) Purple.copy(alpha = 0.12f) else GlassWhite2,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (on) Purple else GlassBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (on) {
                        Icon(Icons.Default.Check, null, tint = Purple, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text,
                        color = if (on) Purple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Selector de imágenes YA subidas al servidor.
 *
 * Es el equivalente de "Elegir de mis imágenes subidas" del panel web:
 * evita volver a subir (y duplicar) una foto que ya está en uploads.
 *
 * En modo `multi` (la galería) no se cierra al elegir, para poder marcar
 * varias de una vez.
 */
@Composable
fun UploadedImagePicker(
    images: List<TourImage>,
    isLoading: Boolean,
    siteBaseUrl: String,
    selected: List<String>,
    multi: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(images, query) {
        if (query.isBlank()) images
        else images.filter { it.name.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgLight,
        title = { Text("Mis imágenes", color = TextPrimary, fontWeight = FontWeight.Bold) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (multi) "Listo" else "Cerrar", color = Purple, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar por nombre", color = TextDim, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    shape = RoundedCornerShape(14.dp),
                    colors = adminFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                when {
                    isLoading -> Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Purple) }

                    filtered.isEmpty() -> Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (images.isEmpty()) "Todavía no hay imágenes subidas"
                            else "Ninguna imagen coincide",
                            color = TextMuted, fontSize = 13.sp
                        )
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { img ->
                            val on = img.url in selected
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassWhite2)
                                    .border(
                                        if (on) 2.dp else 1.dp,
                                        if (on) Purple else GlassBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onPick(img.url) }
                            ) {
                                AsyncImage(
                                    model = absoluteUrl(img.url, siteBaseUrl),
                                    contentDescription = img.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (on) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Purple)
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = TextOnAccent,
                                            modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Cuadrícula de imágenes con botón de añadir y X para quitar.
 * Sirve para la galería del plan.
 */
@Composable
fun ImageStrip(
    urls: List<String>,
    siteBaseUrl: String,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(urls.size) { index ->
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassWhite2)
            ) {
                AsyncImage(
                    model = absoluteUrl(urls[index], siteBaseUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .align(Alignment.TopEnd)
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Quitar", tint = TextOnAccent, modifier = Modifier.size(13.dp))
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassWhite2)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isBusy, onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Purple, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, null, tint = TextMuted, modifier = Modifier.size(24.dp))
                        Text("Añadir", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
