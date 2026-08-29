package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HeartRateZone
import com.example.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PulseTheme.colors.border,
    glowColor: Color? = null,
    backgroundColor: Color = PulseTheme.colors.surfaceCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (glowColor != null) {
                        Brush.linearGradient(listOf(glowColor.copy(alpha = 0.6f), borderColor))
                    } else {
                        Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
                    }
                ),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (PulseTheme.colors.isDark) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun BiometricStatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    accentColor: Color = PulseTheme.colors.primary,
    icon: ImageVector? = null,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PulseTheme.colors.surfaceElevated)
            .border(BorderStroke(1.dp, PulseTheme.colors.border), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PulseTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 3.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneBadge(
    zone: HeartRateZone,
    modifier: Modifier = Modifier
) {
    val zoneColor = zone.getColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(zoneColor.copy(alpha = if (PulseTheme.colors.isDark) 0.15f else 0.12f))
            .border(BorderStroke(1.dp, zoneColor.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(zoneColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "ZONE ${zone.zoneNumber} • ${zone.title.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = if (PulseTheme.colors.isDark) zoneColor else zoneColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    icon: ImageVector? = null,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Formatting Utilities
fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatPace(paceSecPerKm: Int): String {
    if (paceSecPerKm <= 0 || paceSecPerKm > 3600) return "--:--"
    val m = paceSecPerKm / 60
    val s = paceSecPerKm % 60
    return String.format("%d:%02d", m, s)
}

fun formatDistance(meters: Double): String {
    val km = meters / 1000.0
    return String.format("%.2f", km)
}
