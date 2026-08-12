package com.theextramile.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.ui.theme.*

/**
 * Piezas que comparten todas las secciones portadas del panel web, para que
 * cada pantalla se ocupe solo de lo suyo.
 */

/** Cabecera de sección: botón atrás, título, subtítulo y acción opcional */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    actionIcon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction) {
                Icon(actionIcon, actionLabel, tint = CyanLight)
            }
        }
    }
}

/** Campo de búsqueda igual que el del panel */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Buscar…",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, "Limpiar", tint = TextMuted)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = adminFieldColors()
    )
}

/** Fila de filtros con contador, como los del panel */
@Composable
fun FilterChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    counts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val active = key == selected
            val count = counts[key]
            Surface(
                shape = RoundedCornerShape(50),
                color = if (active) Purple.copy(alpha = 0.25f) else GlassWhite,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (active) Purple else GlassBorder
                ),
                modifier = Modifier.clickable { onSelect(key) }
            ) {
                Text(
                    text = if (count != null) "$label ($count)" else label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (active) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** Tarjeta de estadística del Resumen y del Extracto */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = CyanLight,
    icon: ImageVector? = null
) {
    GlassCard(modifier = modifier, contentPadding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    label.uppercase(),
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                color = accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Campo de formulario con etiqueta y ayuda, como los del panel */
@Composable
fun AdminField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = TextDim, fontSize = 13.sp) } },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = adminFieldColors()
        )
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

/** Interruptor con título y explicación */
@Composable
fun AdminSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(hint, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = Purple,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = GlassWhite,
                uncheckedBorderColor = GlassBorder
            )
        )
    }
}

/** Encabezado de un grupo de campos dentro de un formulario largo */
@Composable
fun FormSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
        color = CyanLight,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

/**
 * Una acción destructiva a la espera de confirmarse.
 *
 * Va como data class en vez de Pair<String, () -> Unit> porque la inferencia
 * de tipos de Kotlin con lambdas dentro de `to` es frágil, y así se lee mejor.
 */
data class PendingAction(val message: String, val run: () -> Unit)

/** Diálogo de confirmación para las acciones que borran */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Eliminar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgLight,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = OrangeRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}

/** Estado vacío / de carga / de error, con el mismo texto que el panel */
@Composable
fun SectionPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isLoading -> CircularProgressIndicator(color = CyanLight, strokeWidth = 2.5.dp)
            icon != null -> Icon(icon, null, tint = TextDim, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            message,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            GradientButton(text = actionLabel, onClick = onAction, height = 44.dp)
        }
    }
}

/**
 * Cuadrito de color con su valor hex — el selector de la sección Colores.
 *
 * Ojo con el orden: `onHexChange` va el ÚLTIMO para que se pueda llamar con
 * lambda pegada, `ColorField("Fondo", hex) { ... }`. Si `modifier` fuera el
 * último, la lambda se engancharía a él y Kotlin no lo perdona.
 */
@Composable
fun ColorField(
    label: String,
    hex: String,
    modifier: Modifier = Modifier,
    onHexChange: (String) -> Unit
) {
    val parsed = remember(hex) { parseHexColor(hex) }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(parsed ?: Color.Transparent)
                .border(1.dp, GlassBorderStrong, RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = hex,
                onValueChange = { onHexChange(normalizeHexInput(it)) },
                singleLine = true,
                placeholder = { Text("#000000", color = TextDim, fontSize = 13.sp) },
                isError = parsed == null && hex.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** "#1a6274" → Color. Devuelve null si no es un hex válido. */
fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val value = clean.toLong(16)
        if (clean.length == 6) Color(value or 0xFF000000L) else Color(value)
    } catch (e: NumberFormatException) {
        null
    }
}

/** Deja escribir el hex sin pelearse: fuerza '#' y recorta a 7 caracteres */
private fun normalizeHexInput(input: String): String {
    val body = input.trim().removePrefix("#").filter { it.isDigit() || it in "abcdefABCDEF" }
    return "#" + body.take(6)
}

/** Colores de los campos de texto, iguales en toda la app */
@Composable
fun adminFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextMuted,
    focusedContainerColor = GlassWhite2,
    unfocusedContainerColor = GlassWhite2,
    disabledContainerColor = GlassWhite2,
    cursorColor = CyanLight,
    focusedBorderColor = Purple,
    unfocusedBorderColor = GlassBorder,
    disabledBorderColor = GlassBorder,
    errorBorderColor = OrangeRed
)

/** Pastilla pequeña de color plano, para etiquetas y estados */
@Composable
fun TonePill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Botón redondo flotante con gradiente, para "añadir" */
@Composable
fun AddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    gradient: Brush = Gradients.PurplePink,
    contentDescription: String = "Añadir"
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = TextPrimary, modifier = Modifier.size(26.dp))
    }
}

/** Barra fija de guardar que aparece solo cuando hay cambios sin guardar */
@Composable
fun SaveBar(
    visible: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Guardar cambios"
) {
    if (!visible) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BgLight,
        shadowElevation = 12.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, null, tint = Yellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Hay cambios sin guardar",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = label,
                onClick = onSave,
                isLoading = isSaving,
                height = 44.dp,
                modifier = Modifier.widthIn(min = 150.dp)
            )
        }
    }
}
