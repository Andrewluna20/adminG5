package com.theextramile.admin.ui.tours

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
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theextramile.admin.data.model.Tour
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ToursScreen(
    viewModel: ToursViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tours by viewModel.tours.collectAsState()

    var editingTour by remember { mutableStateOf<Tour?>(null) }
    var creatingNew by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<Tour?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        uiState.infoMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(180.dp, (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(OrangeWarm.copy(alpha = 0.2f), Color.Transparent))
                )
                .blur(60.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás",
                            tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tours & Planes", color = TextPrimary,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${tours.size} en total", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Gradients.OrangePink)
                        .clickable { creatingNew = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Nuevo tour", tint = TextPrimary)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullRefresh(pullRefreshState)
            ) {
                if (tours.isEmpty() && !uiState.isRefreshing) {
                    EmptyTours()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tours, key = { it.id }) { tour ->
                            TourCard(
                                tour = tour,
                                isUpdating = tour.id in uiState.updatingIds,
                                onClick = { editingTour = tour },
                                onToggleActive = { viewModel.toggleActive(tour.id) },
                                onDelete = { deleteConfirm = tour }
                            )
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgMid,
                    contentColor = OrangeWarm
                )
            }
        }
    }

    if (editingTour != null || creatingNew) {
        TourEditSheet(
            tour = editingTour,
            viewModel = viewModel,
            onDismiss = { editingTour = null; creatingNew = false }
        )
    }

    deleteConfirm?.let { tour ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            containerColor = BgMid,
            title = { Text("¿Eliminar tour?", color = TextPrimary) },
            text = {
                Text("Vas a eliminar \"${tour.title}\". Esta acción no se puede deshacer.",
                    color = TextSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTour(tour.id)
                    deleteConfirm = null
                }) {
                    Text("Eliminar", color = OrangeRed)
                }
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
private fun TourCard(
    tour: Tour,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        contentPadding = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(BgLight)
            ) {
                if (tour.hasImage) {
                    AsyncImage(
                        model = tour.img,
                        contentDescription = tour.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Gradients.OrangePink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null,
                            tint = TextPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp))
                    }
                }
                // Badge activo
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(50),
                    color = if (tour.active) GreenNeon else TextDim,
                    contentColor = TextPrimary
                ) {
                    Text(
                        if (tour.active) "ACTIVO" else "OCULTO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
                if (isUpdating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = OrangeWarm) }
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(tour.title, color = TextPrimary,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (tour.shortDesc.isNotBlank()) {
                    Text(tour.shortDesc, color = TextSecondary, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tour.displayPrice, color = OrangeWarm,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (tour.priceLabel.isNotBlank()) {
                        Text(" ${tour.priceLabel}", color = TextMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onToggleActive, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (tour.active) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "Activar/desactivar",
                            tint = if (tour.active) GreenNeon else TextMuted
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar",
                            tint = OrangeRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTours() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(GlassWhite),
            contentAlignment = Alignment.Center
        ) {
            Text("🏝️", fontSize = 36.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("No hay tours aún", color = TextPrimary,
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Toca el + para crear el primero",
            color = TextMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp))
    }
}
