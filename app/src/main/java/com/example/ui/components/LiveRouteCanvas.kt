package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsPoint
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun LiveRouteCanvas(
    routePoints: List<GpsPoint>,
    modifier: Modifier = Modifier,
    isTracking: Boolean = true,
    hasGpsFix: Boolean = true
) {
    // Pulse animation for the current GPS position beacon
    val infiniteTransition = rememberInfiniteTransition(label = "RadarBeacon")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeaconRipple"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeaconAlpha"
    )

    // Radar scan line rotation for when waiting / tracking
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080C14))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw High-Tech Radar Coordinate Grid
            drawRadarGrid(width, height)

            // 2. Draw GPS Polyline Route
            if (routePoints.size > 1) {
                drawRoutePolyline(routePoints, width, height, pulseRadius, pulseAlpha)
            } else {
                // Draw Radar Sweep Animation if no route points yet
                drawRadarSweep(width, height, scanAngle)
            }
        }

        // Top-Left Status HUD Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC0E1422))
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = if (isTracking) (if (hasGpsFix || routePoints.isNotEmpty()) NeonGreen else NeonCyan) else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isTracking) {
                    if (hasGpsFix || routePoints.isNotEmpty()) "GPS LOCKED • LIVE" else "ACQUIRING GPS..."
                } else "GPS READY",
                style = MaterialTheme.typography.labelSmall,
                color = if (isTracking) (if (hasGpsFix || routePoints.isNotEmpty()) NeonGreen else NeonCyan) else TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Bottom-Right Coordinates / Elevation Tag
        val lastPt = routePoints.lastOrNull()
        if (lastPt != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0E1422))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format("LAT %.4f°  LNG %.4f°", lastPt.latitude, lastPt.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Text(
                    text = String.format("ALT: %.1f m  •  %d PTS", lastPt.altitude, routePoints.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun DrawScope.drawRadarGrid(width: Float, height: Float) {
    val centerX = width / 2f
    val centerY = height / 2f
    val gridColor = Color(0xFF131D2E)
    val ringColor = Color(0xFF162338)

    // Concentric Radar Rings
    drawCircle(color = ringColor, radius = min(width, height) * 0.2f, style = Stroke(width = 1f))
    drawCircle(color = ringColor, radius = min(width, height) * 0.38f, style = Stroke(width = 1f))
    drawCircle(color = ringColor, radius = min(width, height) * 0.55f, style = Stroke(width = 1f))

    // Crosshairs
    drawLine(
        color = gridColor,
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 1f
    )
    drawLine(
        color = gridColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, height),
        strokeWidth = 1f
    )

    // Diagonal Radar ticks
    val step = 40f
    var x = step
    while (x < width) {
        drawLine(
            color = Color(0x1500E5FF),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.5f
        )
        x += step
    }
    var y = step
    while (y < height) {
        drawLine(
            color = Color(0x1500E5FF),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
        y += step
    }
}

private fun DrawScope.drawRadarSweep(width: Float, height: Float, scanAngle: Float) {
    val centerX = width / 2f
    val centerY = height / 2f
    val maxRadius = min(width, height) * 0.5f

    val angleRad = Math.toRadians(scanAngle.toDouble())
    val endX = (centerX + maxRadius * cos(angleRad)).toFloat()
    val endY = (centerY + maxRadius * sin(angleRad)).toFloat()

    drawLine(
        brush = Brush.linearGradient(
            listOf(NeonCyan.copy(alpha = 0.8f), Color.Transparent),
            start = Offset(centerX, centerY),
            end = Offset(endX, endY)
        ),
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = 2f
    )

    drawCircle(
        color = NeonCyan,
        radius = 4f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawRoutePolyline(
    points: List<GpsPoint>,
    width: Float,
    height: Float,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    var minLat = points.minOf { it.latitude }
    var maxLat = points.maxOf { it.latitude }
    var minLng = points.minOf { it.longitude }
    var maxLng = points.maxOf { it.longitude }

    val latSpan = max(maxLat - minLat, 0.0008)
    val lngSpan = max(maxLng - minLng, 0.0008)

    val padding = 36f
    val plotWidth = width - padding * 2
    val plotHeight = height - padding * 2

    fun mapToCanvas(pt: GpsPoint): Offset {
        val normX = ((pt.longitude - minLng) / lngSpan).toFloat().coerceIn(0f, 1f)
        val normY = (1f - ((pt.latitude - minLat) / latSpan).toFloat()).coerceIn(0f, 1f)
        return Offset(padding + normX * plotWidth, padding + normY * plotHeight)
    }

    val canvasPoints = points.map { mapToCanvas(it) }

    // Draw Glowing Backdrop Shadow Path
    val path = Path().apply {
        moveTo(canvasPoints.first().x, canvasPoints.first().y)
        for (i in 1 until canvasPoints.size) {
            lineTo(canvasPoints[i].x, canvasPoints[i].y)
        }
    }

    // Outer Neon Glow
    drawPath(
        path = path,
        color = NeonCyan.copy(alpha = 0.25f),
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Inner Sharp Route Line with Pace / Zone color gradient
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            listOf(NeonGreen, NeonCyan, NeonCoral),
            start = canvasPoints.first(),
            end = canvasPoints.last()
        ),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Draw Start Marker (Green Circle with Ring)
    val startOffset = canvasPoints.first()
    drawCircle(
        color = NeonGreen.copy(alpha = 0.4f),
        radius = 8f,
        center = startOffset
    )
    drawCircle(
        color = NeonGreen,
        radius = 4f,
        center = startOffset
    )

    // Draw Current Position Beacon (Pulsing Cyan Ring + White Dot)
    val currentOffset = canvasPoints.last()
    drawCircle(
        color = NeonCyan.copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = currentOffset,
        style = Stroke(width = 2f)
    )
    drawCircle(
        color = NeonCyan,
        radius = 6f,
        center = currentOffset
    )
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = currentOffset
    )
}
