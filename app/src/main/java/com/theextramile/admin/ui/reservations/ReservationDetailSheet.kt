package com.theextramile.admin.ui.reservations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.data.model.Reservation
import com.theextramile.admin.ui.components.*
import com.theextramile.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailSheet(
    reservation: Reservation,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRestore: () -> Unit,
    onWhatsAppClient: () -> Unit,
    onWhatsAppForward: () -> Unit,
    onCall: () -> Unit,
    onSendInvoice: () -> Unit = {},
    onPreviewInvoice: () -> Unit = {},
    hasAdminWhatsApp: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                SquareGradientAvatar(
                    text = reservation.name,
                    size = 62.dp,
                    cornerRadius = 18.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reservation.name,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val (bg, txt, label) = when {
                        reservation.isConfirmed -> Triple(StatusConfirmedBg, StatusConfirmedText, "CONFIRMADA")
                        reservation.isCancelled -> Triple(StatusCancelledBg, StatusCancelledText, "CANCELADA")
                        else -> Triple(StatusPendingBg, StatusPendingText, "PENDIENTE")
                    }
                    Spacer(Modifier.height(4.dp))
                    StatusChip(label, bg, txt)
                }
            }

            Spacer(Modifier.height(24.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DataRow(Icons.Default.Tour, "Tour", reservation.tourTitle, Gradients.OrangePink)
                    DataRow(Icons.Default.CalendarMonth, "Fecha", reservation.date, Gradients.PurplePink)
                    DataRow(Icons.Default.Group, "Pasajeros", "${reservation.pax}", Gradients.BlueCyan)
                    DataRow(Icons.Default.Phone, "Teléfono", reservation.phone, Gradients.GreenCyan)
                    reservation.email?.takeIf { it.isNotBlank() }?.let {
                        DataRow(Icons.Default.Email, "Email", it, Gradients.PurpleBlue)
                    }
                    reservation.notes?.takeIf { it.isNotBlank() }?.let {
                        DataRow(Icons.Default.Notes, "Notas", it, Gradients.PinkOrange)
                    }
                    DataRow(Icons.Default.Tag, "Código", "#${reservation.id}", Gradients.PurplePink)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // BOTONES DE WHATSAPP — 2 botones
            // ═══════════════════════════════════════════════════════════
            Text(
                "CONTACTAR",
                color = TextSecondary,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            // Botón 1: WhatsApp Cliente (verde WhatsApp)
            GradientButton(
                text = "ESCRIBIR AL CLIENTE",
                onClick = onWhatsAppClient,
                modifier = Modifier.fillMaxWidth(),
                gradient = Brush.linearGradient(listOf(WhatsApp, Color(0xFF128C7E))),
                icon = Icons.Default.Chat,
                height = 50.dp,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Botón 2: Reenviar a Mi WhatsApp (azul-cyan)
            if (hasAdminWhatsApp) {
                GradientButton(
                    text = "REENVIAR A MI WHATSAPP",
                    onClick = onWhatsAppForward,
                    modifier = Modifier.fillMaxWidth(),
                    gradient = Gradients.BlueCyan,
                    icon = Icons.Default.Forward,
                    height = 50.dp,
                    shape = RoundedCornerShape(14.dp)
                )
            } else {
                // Aviso: configurar primero
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33FBBF24),
                    contentColor = Yellow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Configura tu número en Ajustes para reenviarte reservas",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Botón: Llamar (outline)
            OutlinedButton(
                onClick = onCall,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary,
                    containerColor = GlassWhite
                )
            ) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "LLAMAR AL CLIENTE",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // ACCIONES DE ESTADO
            // ═══════════════════════════════════════════════════════════
            Text(
                "ESTADO DE LA RESERVA",
                color = TextSecondary,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            when {
                isUpdating -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BlueElectric)
                    }
                }
                reservation.isPending -> {
                    GradientButton(
                        text = "CONFIRMAR RESERVA",
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Gradients.GreenCyan,
                        icon = Icons.Default.Check
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedActionButton(
                        text = "Cancelar reserva",
                        onClick = onCancel,
                        icon = Icons.Default.Close,
                        tint = OrangeRed
                    )
                }
                reservation.isConfirmed -> {
                    // ★ BOTONES DE FACTURA — solo en confirmadas
                    GradientButton(
                        text = "ENVIAR FACTURA POR WHATSAPP",
                        onClick = onSendInvoice,
                        modifier = Modifier.fillMaxWidth(),
                        gradient = Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                        ),
                        icon = Icons.Default.Receipt,
                        height = 50.dp,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedActionButton(
                        text = "Ver factura (PDF)",
                        onClick = onPreviewInvoice,
                        icon = Icons.Default.PictureAsPdf,
                        tint = PurpleLight
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedActionButton(
                        text = "Volver a pendiente",
                        onClick = onRestore,
                        icon = Icons.Default.Undo,
                        tint = Yellow
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedActionButton(
                        text = "Cancelar reserva",
                        onClick = onCancel,
                        icon = Icons.Default.Close,
                        tint = OrangeRed
                    )
                }
                reservation.isCancelled -> {
                    OutlinedActionButton(
                        text = "Restaurar a pendiente",
                        onClick = onRestore,
                        icon = Icons.Default.Restore,
                        tint = BlueElectric
                    )
                }
            }
        }
    }
}

@Composable
private fun DataRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconGradient: Brush
) {
    Row(verticalAlignment = Alignment.Top) {
        GradientIconBox(
            icon = icon,
            gradient = iconGradient,
            size = 36.dp,
            iconSize = 18.dp,
            cornerRadius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
            Text(
                label,
                color = TextSecondary,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tint.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tint,
            containerColor = tint.copy(alpha = 0.08f)
        )
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}
