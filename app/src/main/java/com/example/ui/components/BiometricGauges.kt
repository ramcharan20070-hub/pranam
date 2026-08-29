package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HeartRateZone
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun HeartRateTachometer(
    currentHr: Int,
    maxHr: Int,
    currentZone: HeartRateZone,
    modifier: Modifier = Modifier,
    hrSource: String = ""
) {
    val safeMax = if (maxHr > 50) maxHr else 190
    val progress = if (currentHr > 0) (currentHr.toFloat() / safeMax.toFloat()).coerceIn(0.1f, 1.0f) else 0.05f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "HrGauge")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height * 0.75f)
            val radius = min(width * 0.42f, height * 0.7f)

            val startAngle = 160f
            val sweepAngle = 220f

            // 1. Background Arc
            drawArc(
                color = CyberBorder,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // 2. Active Glowing Colored Arc
            val activeSweep = sweepAngle * animatedProgress
            val activeBrush = Brush.sweepGradient(
                0.0f to Zone1WarmUp,
                0.25f to Zone2FatBurn,
                0.50f to Zone3Aerobic,
                0.75f to Zone4Anaerobic,
                1.0f to Zone5Peak,
                center = center
            )

            drawArc(
                brush = activeBrush,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // 3. Zone Tick Dividers
            val zonePct = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
            for (p in zonePct) {
                val angle = startAngle + sweepAngle * p
                val rad = Math.toRadians(angle.toDouble())
                val p1 = Offset(
                    (center.x + (radius - 12) * cos(rad)).toFloat(),
                    (center.y + (radius - 12) * sin(rad)).toFloat()
                )
                val p2 = Offset(
                    (center.x + (radius + 12) * cos(rad)).toFloat(),
                    (center.y + (radius + 12) * sin(rad)).toFloat()
                )
                drawLine(
                    color = CyberBackground,
                    start = p1,
                    end = p2,
                    strokeWidth = 3f
                )
            }

            // 4. Current Value Indicator Needle Dot
            val needleAngle = startAngle + activeSweep
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needlePoint = Offset(
                (center.x + radius * cos(needleRad)).toFloat(),
                (center.y + radius * sin(needleRad)).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = needlePoint
            )
        }

        // Center Metric Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (currentHr > 0) currentZone.getColor() else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentHr > 0) "$currentHr" else "--",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = " BPM",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (currentHr > 0) {
                ZoneBadge(zone = currentZone)
            } else {
                Text(
                    text = "Awaiting Sensor Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (hrSource.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Source: $hrSource",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StaminaFatigueGauge(
    staminaPercent: Int,
    fatigueScore: Int,
    modifier: Modifier = Modifier
) {
    val staminaProgress by animateFloatAsState(targetValue = staminaPercent / 100f, label = "Stamina")
    val fatigueProgress by animateFloatAsState(targetValue = fatigueScore / 100f, label = "Fatigue")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stamina
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STAMINA",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$staminaPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CyberBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(staminaProgress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(NeonCyan, NeonGreen)))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Fatigue
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FATIGUE INDEX",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$fatigueScore/100",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fatigueScore > 70) NeonCoral else NeonAmber,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CyberBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fatigueProgress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(NeonAmber, NeonCoral)))
                )
            }
        }
    }
}

@Composable
fun HeartRateZoneDistributionBar(
    zoneTimesSeconds: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    val totalSec = zoneTimesSeconds.values.sum().coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HEART RATE ZONE DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = formatDuration(totalSec),
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stacked Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(CyberBorder)
        ) {
            for (zoneNum in 1..5) {
                val sec = zoneTimesSeconds[zoneNum] ?: 0L
                val fraction = (sec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                if (fraction > 0f) {
                    val zone = HeartRateZone.entries.first { it.zoneNumber == zoneNum }
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(zone.getColor())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Zone Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (zoneNum in 1..5) {
                val sec = zoneTimesSeconds[zoneNum] ?: 0L
                val pct = ((sec.toFloat() / totalSec.toFloat()) * 100).toInt()
                val zone = HeartRateZone.entries.first { it.zoneNumber == zoneNum }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(zone.getColor())
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Z$zoneNum",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = zone.getColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
