package com.theextramile.admin.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.ACTIVITY_FILTER_TYPES
import com.theextramile.admin.data.model.ActivityEntry
import com.theextramile.admin.data.model.ActivityTone
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Actividad — port de admin-html/activity.html + admin-js/activity.js.
 * Solo lectura, igual que en el panel.
 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val total by viewModel.total.collectAsState()
    val query by viewModel.query.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()

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
                title = "Actividad",
                subtitle = if (total > 0) "${entries.size} de $total registro(s)" else null,
                onBack = onBack
            )

            Column(Modifier.padding(horizontal = 16.dp)) {
                SearchField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar por usuario o detalle…"
                )
            }
            Spacer(Modifier.height(12.dp))

            FilterChipRow(
                options = ACTIVITY_FILTER_TYPES,
                selected = typeFilter,
                onSelect = viewModel::onTypeChange
            )
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
                when {
                    uiState.isLoading -> SectionPlaceholder("Cargando…", isLoading = true)

                    uiState.error != null -> SectionPlaceholder(
                        message = uiState.error!!,
                        icon = Icons.Default.History,
                        actionLabel = "Reintentar",
                        onAction = { viewModel.refresh() }
                    )

                    entries.isEmpty() -> SectionPlaceholder(
                        message = if (total > 0) "Sin resultados con estos filtros"
                        else "Aún no hay actividad registrada",
                        icon = Icons.Default.History
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.at + it.action + it.refId }) { entry ->
                            ActivityRow(entry)
                        }
                    }
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
private fun ActivityRow(entry: ActivityEntry) {
    GlassCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TonePill(entry.meta.label, toneColor(entry.meta.tone))
                Spacer(Modifier.weight(1f))
                Text(formatActivityDate(entry.at), color = TextDim, fontSize = 11.sp)
            }
            if (entry.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(entry.summary, color = TextPrimary, fontSize = 14.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(entry.displayUser, size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        entry.displayUser,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (entry.userEmail.isNotBlank()) {
                        Text(entry.userEmail, color = TextDim, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** Mismo color que la clase CSS de cada acción en el panel */
private fun toneColor(tone: ActivityTone): Color = when (tone) {
    ActivityTone.RESERVA -> GreenLight
    ActivityTone.CANCEL -> OrangeRed
    ActivityTone.WARN -> Yellow
    ActivityTone.PLAN -> CyanLight
    ActivityTone.CONFIG -> PurpleLight
    ActivityTone.NEUTRAL -> TextMuted
}

/** actFmtDate(): "12 ago 2026, 14:30" en es-CO */
private val ACTIVITY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("es", "CO"))

fun formatActivityDate(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).format(ACTIVITY_DATE_FORMAT)
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(iso.take(19)).format(ACTIVITY_DATE_FORMAT)
        } catch (e2: Exception) {
            iso
        }
    }
}
