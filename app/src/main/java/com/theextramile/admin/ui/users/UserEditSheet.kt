package com.theextramile.admin.ui.users

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.User
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditSheet(
    user: User?,
    viewModel: UsersViewModel,
    onDismiss: () -> Unit
) {
    val isNew = user == null
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(user?.role ?: "viewer") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isNew) "Nuevo usuario" else "Editar usuario",
                    color = TextPrimary, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(GlassWhite)
                        .clickable(enabled = !isSaving) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary,
                        modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            DarkField("Nombre completo", name, { name = it }, enabled = !isSaving)
            Spacer(Modifier.height(10.dp))
            DarkField("Email", email, { email = it }, enabled = !isSaving,
                keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(10.dp))

            // Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (isNew) "Contraseña" else "Nueva contraseña (vacío = no cambiar)",
                        color = TextSecondary, fontSize = 12.sp
                    )
                },
                singleLine = true, enabled = !isSaving,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            null, tint = TextSecondary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassWhite2,
                    unfocusedContainerColor = GlassWhite2,
                    cursorColor = Purple
                )
            )

            Spacer(Modifier.height(20.dp))
            Text("ROL", color = TextSecondary, fontSize = 10.sp,
                letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            RoleOption("Super Admin", "Acceso total",
                role == "super", Gradients.PurplePink) { role = "super" }
            RoleOption("Gestor de Reservas", "Solo reservas",
                role == "reservations", Gradients.BlueCyan) { role = "reservations" }
            RoleOption("Solo Ver", "Solo lectura",
                role == "viewer", Brush.linearGradient(listOf(TextDim, TextMuted))) { role = "viewer" }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StatusCancelledBg,
                    contentColor = OrangeRed
                ) { Text(error!!, modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            }

            Spacer(Modifier.height(24.dp))

            GradientButton(
                text = if (isNew) "CREAR USUARIO" else "GUARDAR CAMBIOS",
                onClick = {
                    when {
                        name.isBlank() -> { error = "Nombre obligatorio"; return@GradientButton }
                        email.isBlank() -> { error = "Email obligatorio"; return@GradientButton }
                        isNew && password.isBlank() -> { error = "Contraseña obligatoria"; return@GradientButton }
                        password.isNotBlank() && password.length < 6 -> {
                            error = "Mínimo 6 caracteres"; return@GradientButton
                        }
                    }
                    error = null
                    scope.launch {
                        isSaving = true
                        val ok = viewModel.saveUser(
                            id = user?.id, name = name.trim(), email = email.trim(),
                            password = password.takeIf { it.isNotBlank() }, role = role
                        )
                        isSaving = false
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = Gradients.PurplePink,
                icon = Icons.Default.Save,
                isLoading = isSaving
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkField(
    label: String, value: String, onChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Purple,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = GlassWhite2,
            unfocusedContainerColor = GlassWhite2,
            cursorColor = Purple
        )
    )
}

@Composable
private fun RoleOption(
    title: String, subtitle: String, selected: Boolean,
    gradient: Brush, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Purple.copy(alpha = 0.10f) else GlassWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, if (selected) Purple else GlassBorder
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Default.Check, null, tint = TextPrimary,
                        modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Purple, unselectedColor = TextMuted
                )
            )
        }
    }
}
