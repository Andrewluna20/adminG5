package com.theextramile.admin.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import com.theextramile.admin.util.payFormatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Resumen — port de admin-html/overview.html.
 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    userName: String,
    onBack: () -> Unit,
    onOpenReservations: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val recent by viewModel.recent.collectAsState()

    val pullState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(
                title = "Resumen",
                subtitle = todayLabel(),
                onBack = onBack
            )

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    if (userName.isNotBlank()) {
                        Text(
                            "Hola, $userName",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Contadores de reservas ──
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            "Total", stats.total.toString(),
                            Modifier.weight(1f), CyanLight, Icons.Default.Inbox
                        )
                        StatCard(
                            "Confirmadas", stats.confirmed.toString(),
                            Modifier.weight(1f), GreenLight, Icons.Default.CheckCircle
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            "Pendientes", stats.pending.toString(),
                            Modifier.weight(1f), Yellow, Icons.Default.Schedule
                        )
                        StatCard(
                            "Canceladas", stats.cancelled.toString(),
                            Modifier.weight(1f), OrangeRed, Icons.Default.Cancel
                        )
                    }

                    // ── Dinero (sin contar las canceladas) ──
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "DINERO",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            "Vendido", payFormatMoney(stats.vendido),
                            Modifier.weight(1f), CyanLight, Icons.Default.ShowChart
                        )
                        StatCard(
                            "Recibido", payFormatMoney(stats.recibido),
                            Modifier.weight(1f), GreenLight, Icons.Default.Payments
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    StatCard(
                        "Saldo pendiente", payFormatMoney(stats.saldo),
                        Modifier.fillMaxWidth(), Yellow, Icons.Default.AccountBalanceWallet
                    )

                    // ── Últimas reservas ──
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "ÚLTIMAS RESERVAS",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Ver todas",
                            color = CyanLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onOpenReservations)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    when {
                        uiState.isLoading -> SectionPlaceholder("Cargando…", isLoading = true)

                        uiState.error != null -> SectionPlaceholder(
                            message = uiState.error!!,
                            icon = Icons.Default.CloudOff,
                            actionLabel = "Reintentar",
                            onAction = { viewModel.refresh() }
                        )

                        recent.isEmpty() -> SectionPlaceholder(
                            message = "No hay reservas aún",
                            icon = Icons.Default.Inbox
                        )

                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recent.forEach { RecentReservationRow(it) }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = BgLight,
                    contentColor = CyanLight
                )
            }
        }
    }
}

@Composable
private fun RecentReservationRow(r: Reservation) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(r.name.ifBlank { "?" }, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.name.ifBlank { "Sin nombre" },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    r.tourTitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (r.date.isNotBlank()) {
                    Text(r.date, color = TextDim, fontSize = 11.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(
                label = r.statusDisplay,
                backgroundColor = when {
                    r.isConfirmed -> StatusConfirmedBg
                    r.isCancelled -> StatusCancelledBg
                    else -> StatusPendingBg
                },
                contentColor = when {
                    r.isConfirmed -> StatusConfirmedText
                    r.isCancelled -> StatusCancelledText
                    else -> StatusPendingText
                }
            )
        }
    }
}

/** adm-date-display: "martes, 12 de agosto de 2026" */
private fun todayLabel(): String = try {
    LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CO"))
    ).replaceFirstChar { it.uppercase() }
} catch (e: Exception) {
    ""
}
