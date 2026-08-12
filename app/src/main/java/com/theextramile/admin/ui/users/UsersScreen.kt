package com.theextramile.admin.ui.users

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.User
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: UsersViewModel,
    currentUserId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingUser by remember { mutableStateOf<User?>(null) }
    var creatingNew by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<User?>(null) }

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
                        Text("Usuarios admin", color = TextPrimary,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${users.size} usuario(s)", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            floatingActionButton = {
                if (!uiState.forbidden) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(Gradients.PurpleBlue)
                            .clickable { creatingNew = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, "Nuevo", tint = TextPrimary)
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple)
                    }
                    uiState.forbidden -> ForbiddenView()
                    users.isEmpty() -> EmptyUsers()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(users, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                isCurrentUser = user.id == currentUserId,
                                onClick = { editingUser = user },
                                onDelete = { deleteConfirm = user }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingUser != null || creatingNew) {
        UserEditSheet(
            user = editingUser,
            viewModel = viewModel,
            onDismiss = { editingUser = null; creatingNew = false }
        )
    }

    deleteConfirm?.let { u ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            containerColor = BgMid,
            title = { Text("¿Eliminar usuario?", color = TextPrimary) },
            text = { Text("Vas a eliminar a \"${u.name}\".", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(u.id); deleteConfirm = null
                }) { Text("Eliminar", color = Color(0xFFF87171)) }
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
private fun UserCard(
    user: User,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (roleColor, roleBg) = when (user.role) {
        "super" -> Yellow to Color(0x33FBBF24)
        "reservations" -> BlueElectric to Color(0x333B82F6)
        "viewer" -> TextSecondary to GlassWhite
        else -> TextSecondary to GlassWhite
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(user.name, size = 46.dp,
                gradient = Gradients.forAvatar(user.email))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, color = TextPrimary,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GreenNeon
                        ) {
                            Text("TÚ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(user.email, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                StatusChip(user.roleDisplay, roleBg, roleColor)
            }
            if (!isCurrentUser) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar",
                        tint = Color(0xFFF87171))
                }
            }
        }
    }
}

@Composable
private fun ForbiddenView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(GlassWhite),
            contentAlignment = Alignment.Center
        ) { Text("🔒", fontSize = 36.sp) }
        Spacer(Modifier.height(16.dp))
        Text("Acceso restringido", color = TextPrimary,
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Solo Super Admin puede gestionar usuarios",
            color = TextMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun EmptyUsers() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👥", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("Sin usuarios", color = TextPrimary, fontSize = 16.sp)
    }
}
