package com.theextramile.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/* ═══════════════════════════════════════════════════════
   Controles de formulario que usan varias secciones.

   Nacieron en el editor de planes, pero Reservas necesita los mismos
   desplegables y selectores de fecha, así que viven aquí y no dentro de
   una sección concreta.
   ═══════════════════════════════════════════════════════ */

/** "yyyy-MM-dd" en UTC — ver el porqué en [millisToIsoDate] */
private fun isoDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * El DatePicker de Material devuelve la medianoche del día elegido **en
 * UTC**. Formatear eso en la zona local restaría cinco horas en Colombia
 * y guardaría el día anterior, así que se formatea también en UTC.
 */
fun millisToIsoDate(millis: Long): String = isoDateFormat().format(java.util.Date(millis))

/** "2026-08-20" → "20 ago 2026", para enseñarlo sin que parezca un dato de máquina */
fun prettyDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val meses = listOf("ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic")
    val mes = parts[1].toIntOrNull()?.minus(1)?.takeIf { it in meses.indices } ?: return iso
    return "${parts[2].trimStart('0')} ${meses[mes]} ${parts[0]}"
}

/**
 * Desplegable de una sola opción.
 *
 * `options` son pares valor→texto; el valor es lo que se guarda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    // Si el valor guardado ya no está en la lista (un muelle borrado, un
    // calendario que se fue) se enseña su valor crudo en vez de un hueco:
    // así se ve que hay algo puesto y no se pierde al guardar.
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (label.isNotBlank()) {
            Text(
                label.uppercase(),
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(6.dp))
        }
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        null, tint = TextMuted
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(BgMid)
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text,
                                color = if (value == selected) Purple else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelect(value); expanded = false }
                    )
                }
            }
        }
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

/**
 * Fila que salta de línea sola.
 *
 * Compose trae FlowRow, pero es experimental y cambia de firma entre
 * versiones; esto hace lo justo que se necesita aquí y no se rompe al
 * subir el BOM.
 */
@Composable
fun FlowRowSimple(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var x = 0
        var y = 0
        var rowHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()
        placeables.forEach { p ->
            if (x + p.width > maxWidth && x > 0) { x = 0; y += rowHeight; rowHeight = 0 }
            positions.add(x to y)
            x += p.width
            rowHeight = maxOf(rowHeight, p.height)
        }
        layout(maxWidth, y + rowHeight) {
            placeables.forEachIndexed { i, p -> p.place(positions[i].first, positions[i].second) }
        }
    }
}

/** Etiqueta con una X para quitarla */
@Composable
fun RemovablePill(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Color = Purple
) {
    Surface(
        modifier = modifier.padding(end = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(50),
        color = tone.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = tone, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Quitar", tint = tone, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/**
 * Campo de fecha que abre el calendario de Android.
 *
 * Guarda "yyyy-MM-dd", que es como lo espera el sitio, pero enseña
 * "20 ago 2026".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var show by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { show = true },
        shape = RoundedCornerShape(14.dp),
        color = GlassWhite2,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                if (value.isBlank()) label else prettyDate(value),
                color = if (value.isBlank()) TextDim else TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (show) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(millisToIsoDate(it)) }
                    show = false
                }) { Text("Elegir", color = Purple, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancelar", color = TextMuted) }
            },
            colors = DatePickerDefaults.colors(containerColor = BgLight)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = BgLight))
        }
    }
}

/** Campo de hora ("HH:mm") que abre el reloj de Android */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.clickable { show = true },
        shape = RoundedCornerShape(14.dp),
        color = GlassWhite2,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                value.ifBlank { label },
                color = if (value.isBlank()) TextDim else TextPrimary,
                fontSize = 14.sp
            )
        }
    }

    if (show) {
        val parts = value.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = BgLight,
            confirmButton = {
                TextButton(onClick = {
                    onPick(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
                    show = false
                }) { Text("Elegir", color = Purple, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancelar", color = TextMuted) }
            },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            }
        )
    }
}
