package com.theextramile.admin.ui.gcal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.theextramile.admin.data.model.GoogleCalendarAccount
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleCalendarScreen(
    viewModel: GoogleCalendarViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val configStatus by viewModel.configStatus.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var deleteConfirm by remember { mutableStateOf<GoogleCalendarAccount?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        uiState.infoMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Gradients.Background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(GlassWhite).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás",
                            tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Calendar", color = TextPrimary,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${accounts.size} cuenta(s)",
                            color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!configStatus.configured) {
                    item { WarningCard() }
                }

                item {
                    GradientButton(
                        text = if (accounts.isEmpty()) "CONECTAR GOOGLE CALENDAR"
                        else "+ AGREGAR OTRA CUENTA",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.authUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Gradients.GreenCyan,
                        icon = Icons.Default.Add,
                        enabled = configStatus.configured
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = GreenNeon) }
                    }
                } else if (accounts.isEmpty()) {
                    item { EmptyGcalCard() }
                } else {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("CUENTAS VINCULADAS", color = TextSecondary,
                            fontSize = 10.sp, letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(accounts, key = { it.email }) { account ->
                        AccountCard(
                            account = account,
                            onSetActive = { viewModel.switchAccount(account.email) },
                            onDelete = { deleteConfirm = account }
                        )
                    }
                }
            }
        }
    }

    deleteConfirm?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            containerColor = BgMid,
            title = { Text("¿Desvincular cuenta?", color = TextPrimary) },
            text = {
                Text("${acc.email}\nLos eventos creados permanecerán en Google Calendar.",
                    color = TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(acc.email); deleteConfirm = null
                }) { Text("Desvincular", color = Color(0xFFF87171)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun WarningCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0x33FBBF24),
        borderColor = Yellow.copy(alpha = 0.4f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Configuración pendiente",
                    fontWeight = FontWeight.SemiBold, color = Yellow)
                Text("Configura las credenciales OAuth desde config20.html",
                    fontSize = 11.sp, color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun EmptyGcalCard() {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📅", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("Sin cuentas vinculadas", color = TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Los eventos se crearán automáticamente al recibir reservas",
                color = TextSecondary, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun AccountCard(
    account: GoogleCalendarAccount,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (account.isActive) Color(0x3310B981) else GlassWhite,
        borderColor = if (account.isActive) GreenNeon.copy(alpha = 0.4f) else GlassBorder,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(account.email, size = 40.dp,
                gradient = Gradients.GreenCyan)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(account.email, color = TextPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (account.isActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GreenNeon
                        ) {
                            Text("ACTIVA",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                color = TextPrimary, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account.hasRefreshToken) {
                        Icon(Icons.Default.AutoMode, null, tint = GreenNeon,
                            modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Token permanente", color = TextMuted, fontSize = 10.sp)
                    } else {
                        Icon(Icons.Default.Warning, null, tint = Yellow,
                            modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Reconectar", color = Yellow, fontSize = 10.sp)
                    }
                }
            }
            if (!account.isActive) {
                TextButton(onClick = onSetActive) {
                    Text("Usar", color = GreenNeon,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, "Desvincular",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
