package com.theextramile.admin.ui.tours

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourEditSheet(
    tour: Tour?,
    viewModel: ToursViewModel,
    onDismiss: () -> Unit
) {
    val isNew = tour == null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf(tour?.title ?: "") }
    var shortDesc by remember { mutableStateOf(tour?.shortDesc ?: "") }
    var price by remember { mutableStateOf(tour?.price ?: "") }
    var priceLabel by remember { mutableStateOf(tour?.priceLabel ?: "") }
    var imageUrl by remember { mutableStateOf(tour?.img ?: "") }
    var active by remember { mutableStateOf(tour?.active ?: true) }
    var includes by remember { mutableStateOf(tour?.includes ?: emptyList()) }
    var newInclude by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                error = null
                val tmpFile = uriToTempFile(context, uri)
                if (tmpFile != null) {
                    val url = viewModel.uploadImage(tmpFile)
                    tmpFile.delete()
                    if (url != null) imageUrl = url
                    else error = "No se pudo subir la imagen"
                } else error = "No se pudo leer la imagen"
                isUploading = false
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = BgMid,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassBorderStrong) }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isNew) "Nuevo tour" else "Editar tour",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .clickable(enabled = !isSaving) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassWhite)
                    .clickable(enabled = !isUploading) {
                        imagePickerLauncher.launch("image/*")
                    }
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isUploading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OrangeWarm)
                            Spacer(Modifier.height(8.dp))
                            // Va encima del velo negro de la imagen, no del fondo claro
                            Text("Subiendo…", color = TextOnAccent, fontSize = 12.sp)
                        }
                    }
                } else if (imageUrl.isBlank()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null,
                            tint = TextSecondary, modifier = Modifier.size(40.dp))
                        Text("Toca para subir imagen", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Surface(
                        modifier = Modifier.padding(10.dp).align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.7f),
                        contentColor = TextOnAccent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cambiar", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Campos
            DarkField("Título *", title, { title = it }, enabled = !isSaving)
            DarkField("Descripción corta", shortDesc, { shortDesc = it }, enabled = !isSaving, minLines = 2)

            Row {
                Box(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    DarkField("Precio", price, { price = it }, enabled = !isSaving)
                }
                Box(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    DarkField("Etiqueta", priceLabel, { priceLabel = it }, enabled = !isSaving)
                }
            }

            // Toggle activo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassWhite)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (active) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    null, tint = if (active) GreenNeon else TextMuted
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tour visible al público", color = TextPrimary,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(if (active) "Se muestra en el sitio web" else "Oculto del sitio",
                        color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = active,
                    onCheckedChange = { active = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = GreenNeon,
                        uncheckedThumbColor = TextPrimary,
                        uncheckedTrackColor = TextDim
                    )
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("INCLUYE", color = TextSecondary, fontSize = 10.sp,
                letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)

            includes.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OrangeWarm)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(item, color = TextPrimary, fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            includes = includes.toMutableList().also { it.removeAt(idx) }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, "Eliminar",
                            tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DarkField("", newInclude, { newInclude = it },
                        placeholder = "Ej: Transporte incluido", enabled = !isSaving)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Gradients.OrangePink)
                        .clickable {
                            if (newInclude.isNotBlank()) {
                                includes = includes + newInclude.trim()
                                newInclude = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Agregar", tint = TextPrimary)
                }
            }

            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusCancelledBg,
                    contentColor = OrangeRed
                ) { Text(error!!, modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            }

            Spacer(Modifier.height(24.dp))

            GradientButton(
                text = if (isNew) "CREAR TOUR" else "GUARDAR CAMBIOS",
                onClick = {
                    if (title.isBlank()) { error = "El título es obligatorio"; return@GradientButton }
                    error = null
                    scope.launch {
                        isSaving = true
                        /* ⚠️ Se parte del plan cargado con copy(), NUNCA de un
                           Tour() nuevo: un plan trae muchos más campos de los
                           que se editan aquí (galería, horarios, muelle,
                           precio neto, reseñas, traducción al inglés…) y
                           construirlo desde cero los dejaría en su valor por
                           defecto, borrándolos del sitio al guardar. */
                        val base = tour ?: Tour()
                        val saved = base.copy(
                            id = tour?.id ?: title.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-").trim('-')
                                .ifBlank { "tour-${UUID.randomUUID().toString().take(8)}" },
                            title = title.trim(),
                            shortDesc = shortDesc.trim(),
                            price = price.trim(),
                            priceLabel = priceLabel.trim(),
                            img = imageUrl,
                            active = active,
                            includes = includes
                        )
                        val ok = viewModel.saveTour(saved, isNew = isNew)
                        isSaving = false
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = Gradients.OrangePink,
                icon = Icons.Default.Save,
                isLoading = isSaving,
                enabled = !isUploading
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        if (label.isNotEmpty()) {
            Text(label, color = TextSecondary, fontSize = 10.sp,
                letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = TextMuted, fontSize = 13.sp) }
            } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = minLines == 1,
            minLines = minLines,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary,
                focusedBorderColor = OrangeWarm,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassWhite2,
                unfocusedContainerColor = GlassWhite2,
                cursorColor = OrangeWarm
            )
        )
    }
}

private fun uriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val tmpFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
        input.close()
        tmpFile
    } catch (e: Exception) { null }
}
