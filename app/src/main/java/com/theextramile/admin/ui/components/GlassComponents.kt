package com.theextramile.admin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theextramile.admin.ui.theme.*

/**
 * Card translúcida con efecto glassmorphism.
 * Es la base de todo el diseño nuevo.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    backgroundColor: Color = GlassWhite,
    borderColor: Color = GlassBorder,
    content: @Composable () -> Unit
) {
    val baseModifier = modifier
        .clip(shape)
        .background(backgroundColor)

    val withClick = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Surface(
        modifier = withClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Card con gradiente colorido (para tarjetas destacadas).
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = Gradients.PurplePink,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val baseModifier = modifier
        .clip(shape)
        .background(gradient)

    val withClick = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Box(modifier = withClick.padding(contentPadding)) {
        content()
    }
}

/**
 * Avatar circular con gradiente único basado en el nombre.
 */
@Composable
fun GradientAvatar(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    gradient: Brush? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(gradient ?: Gradients.forAvatar(text)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = TextOnAccent,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42).sp
        )
    }
}

/**
 * Avatar con esquinas redondeadas (no circular).
 */
@Composable
fun SquareGradientAvatar(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    gradient: Brush? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient ?: Gradients.forAvatar(text)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = TextOnAccent,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42).sp
        )
    }
}

/**
 * Chip de estado con glow.
 */
@Composable
fun StatusChip(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

/**
 * Icono dentro de un cuadrado con gradiente.
 */
@Composable
fun GradientIconBox(
    icon: ImageVector,
    gradient: Brush,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextOnAccent,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Botón principal con gradiente.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = Gradients.PurplePink,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    shape: RoundedCornerShape = RoundedCornerShape(50)
) {
    val effectiveEnabled = enabled && !isLoading
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (effectiveEnabled) gradient else Brush.linearGradient(listOf(TextDim, TextDim)))
            .clickable(enabled = effectiveEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = TextOnAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = TextOnAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
