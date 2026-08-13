package com.theextramile.admin.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.User
import com.theextramile.admin.ui.components.GlassCard
import com.theextramile.admin.ui.components.GradientButton
import com.theextramile.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (User) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess && uiState.loggedUser != null) {
            onLoginSuccess(uiState.loggedUser!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.LoginBackground)
    ) {
        // ───── Orbes decorativos ─────
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset((-60).dp, (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Purple.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .blur(40.dp)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(160.dp, 100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Pink.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .blur(40.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset((-40).dp, 500.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BlueElectric.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
                .blur(40.dp)
        )

        // ───── Contenido principal ─────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo G
            LogoG()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Admin G",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Panel de administración",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ───── Card del formulario (glassmorphism) ─────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = 24.dp,
                backgroundColor = GlassWhite
            ) {
                Column {
                    Text("Iniciar sesión",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Ingresa tus credenciales",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.errorMessage != null) {
                        ErrorMessage(uiState.errorMessage!!)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    FieldLabel("EMAIL")
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "admin@correo.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        enabled = !uiState.isLoading
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FieldLabel("CONTRASEÑA")
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focusManager.clearFocus()
                            viewModel.login()
                        },
                        passwordVisible = uiState.passwordVisible,
                        onTogglePassword = viewModel::togglePasswordVisibility,
                        enabled = !uiState.isLoading
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GradientButton(
                        text = "ACCEDER",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Gradients.PurplePink,
                        isLoading = uiState.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "The Extra Mile · Cartagena",
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun LogoG() {
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Gradients.PurplePink),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "G",
            color = TextPrimary,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-2).sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    passwordVisible: Boolean? = null,
    onTogglePassword: () -> Unit = {},
    enabled: Boolean = true
) {
    val isPassword = passwordVisible != null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
        leadingIcon = {
            Icon(leadingIcon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (passwordVisible == true) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        null,
                        tint = TextSecondary
                    )
                }
            }
        } else null,
        visualTransformation = when {
            isPassword && passwordVisible != true -> PasswordVisualTransformation()
            else -> VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Purple,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = GlassWhite2,
            unfocusedContainerColor = GlassWhite2,
            cursorColor = Purple,
            focusedLeadingIconColor = Purple,
            unfocusedLeadingIconColor = TextSecondary
        )
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ErrorMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StatusCancelledBg,
        contentColor = OrangeRed
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                message,
                fontSize = 13.sp
            )
        }
    }
}
