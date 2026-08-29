package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalTextApi::class)
@Composable
fun PerformanceTrendsChart(
    trends: List<DayPerformanceTrend>,
    metricMode: TrendsMetricMode,
    selectedIndex: Int?,
    onSelectDay: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Animation progress for smooth entrance
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(metricMode, trends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorderBright, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("performance_trends_canvas_card")
    ) {
        Column {
            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .pointerInput(trends) {
                        detectTapGestures(
                            onPress = { offset ->
                                val count = trends.size
                                if (count > 0) {
                                    val slotWidth = size.width / count
                                    val idx = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                        )
                    }
                    .pointerInput(trends) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val count = trends.size
                                if (count > 0) {
                                    val slotWidth = size.width / count
                                    val idx = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            },
                            onDrag = { change, _ ->
                                val count = trends.size
                                if (count > 0) {
                                    val slotWidth = size.width / count
                                    val idx = (change.position.x / slotWidth).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                        )
                    }
            ) {
                if (trends.isEmpty()) return@Canvas

                val progress = animationProgress.value
                val count = trends.size
                val chartHeight = size.height - 30.dp.toPx()
                val chartWidth = size.width
                val slotWidth = chartWidth / count

                // 1. Draw Gridlines and Baseline References
                drawGridAndReferenceLines(
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    metricMode = metricMode,
                    textMeasurer = textMeasurer
                )

                // 2. Draw Active Mode Data Series
                when (metricMode) {
                    TrendsMetricMode.DISTANCE_VOLUME -> {
                        drawDistanceVolumeBars(
                            trends = trends,
                            slotWidth = slotWidth,
                            chartHeight = chartHeight,
                            progress = progress,
                            selectedIndex = selectedIndex
                        )
                    }
                    TrendsMetricMode.HRV_RECOVERY -> {
                        drawHrvRecoveryCurve(
                            trends = trends,
                            slotWidth = slotWidth,
                            chartHeight = chartHeight,
                            progress = progress,
                            selectedIndex = selectedIndex
                        )
                    }
                    TrendsMetricMode.PACE_INTENSITY -> {
                        drawPaceIntensityMultiLine(
                            trends = trends,
                            slotWidth = slotWidth,
                            chartHeight = chartHeight,
                            progress = progress,
                            selectedIndex = selectedIndex
                        )
                    }
                }

                // 3. Draw Selected Scrubber Vertical Guide Line & Node
                if (selectedIndex != null && selectedIndex in trends.indices) {
                    val x = (selectedIndex + 0.5f) * slotWidth
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.8f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Scrubber top pointer dot
                    drawCircle(
                        color = NeonCyan,
                        radius = 5.dp.toPx(),
                        center = Offset(x, 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, 4.dp.toPx())
                    )
                }

                // 4. Draw X-Axis Weekday Labels
                val labelY = chartHeight + 18.dp.toPx()
                trends.forEachIndexed { i, day ->
                    val x = (i + 0.5f) * slotWidth
                    val isSelected = (i == selectedIndex)
                    val isToday = day.isToday

                    val labelColor = when {
                        isSelected -> NeonCyan
                        isToday -> NeonGreen
                        else -> TextMuted
                    }

                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString(day.dayLabel),
                        style = TextStyle(
                            color = labelColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    )

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x - textLayout.size.width / 2f, labelY - textLayout.size.height / 2f)
                    )
                }
            }
        }
    }
}

// --- Volume Bar Chart Renderer ---
private fun DrawScope.drawDistanceVolumeBars(
    trends: List<DayPerformanceTrend>,
    slotWidth: Float,
    chartHeight: Float,
    progress: Float,
    selectedIndex: Int?
) {
    val maxDist = max(10000.0, (trends.maxOfOrNull { it.totalDistanceMeters } ?: 0.0) * 1.2)
    val barWidth = (slotWidth * 0.55f).coerceIn(12f, 40f)

    trends.forEachIndexed { i, day ->
        val xCenter = (i + 0.5f) * slotWidth
        val left = xCenter - (barWidth / 2f)
        val distFraction = ((day.totalDistanceMeters / maxDist).toFloat() * progress).coerceIn(0f, 1f)
        val barHeight = chartHeight * distFraction
        val top = chartHeight - barHeight

        val isSelected = (i == selectedIndex)
        val isToday = day.isToday

        if (barHeight > 4f) {
            // Gradient Fill
            val barBrush = Brush.verticalGradient(
                colors = when {
                    isSelected -> listOf(NeonCyan, Color(0xFF006680))
                    isToday -> listOf(NeonGreen, Color(0xFF008040))
                    day.totalDistanceMeters > 0 -> listOf(NeonCyan.copy(alpha = 0.85f), CyberSurface)
                    else -> listOf(CyberBorderBright, CyberSurface)
                },
                startY = top,
                endY = chartHeight
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Neon Glowing Top Cap
            drawLine(
                color = if (isSelected) Color.White else if (isToday) NeonGreen else NeonCyan,
                start = Offset(left, top),
                end = Offset(left + barWidth, top),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // Rest / Zero Day subtle base dot
            drawCircle(
                color = if (isSelected) NeonCyan.copy(alpha = 0.5f) else CyberBorderBright,
                radius = 3.dp.toPx(),
                center = Offset(xCenter, chartHeight - 4.dp.toPx())
            )
        }

        // Workout count pill indicator if multiple workouts
        if (day.workoutCount > 1) {
            drawCircle(
                color = NeonAmber,
                radius = 4.dp.toPx(),
                center = Offset(xCenter, top - 8.dp.toPx())
            )
        }
    }
}

// --- Heart Rate Variability (HRV) Smooth Cubic Curve Renderer ---
private fun DrawScope.drawHrvRecoveryCurve(
    trends: List<DayPerformanceTrend>,
    slotWidth: Float,
    chartHeight: Float,
    progress: Float,
    selectedIndex: Int?
) {
    val minRmssd = 35f
    val maxRmssd = 90f
    val range = maxRmssd - minRmssd

    // Shaded Normal Baseline Zone (55ms to 75ms)
    val baselineTop = chartHeight - ((75f - minRmssd) / range) * chartHeight
    val baselineBottom = chartHeight - ((55f - minRmssd) / range) * chartHeight
    drawRect(
        color = NeonGreen.copy(alpha = 0.08f),
        topLeft = Offset(0f, baselineTop),
        size = Size(size.width, baselineBottom - baselineTop)
    )
    drawLine(
        color = NeonGreen.copy(alpha = 0.25f),
        start = Offset(0f, baselineTop),
        end = Offset(size.width, baselineTop),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    )
    drawLine(
        color = NeonGreen.copy(alpha = 0.25f),
        start = Offset(0f, baselineBottom),
        end = Offset(size.width, baselineBottom),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    )

    val points = trends.mapIndexed { i, day ->
        val x = (i + 0.5f) * slotWidth
        val yFrac = ((day.hrvRmssdMs - minRmssd) / range).coerceIn(0f, 1f)
        val y = chartHeight - (yFrac * chartHeight * progress)
        Offset(x, y)
    }

    if (points.size >= 2) {
        val path = Path()
        val fillPath = Path()

        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, chartHeight)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlX1 = (p0.x + p1.x) / 2f
            val controlY1 = p0.y
            val controlX2 = (p0.x + p1.x) / 2f
            val controlY2 = p1.y

            path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, chartHeight)
        fillPath.close()

        // Gradient Fill Under HRV Curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.28f), NeonCyan.copy(alpha = 0.02f)),
                startY = 0f,
                endY = chartHeight
            )
        )

        // Stroke Line
        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Glowing Data Points on nodes
    points.forEachIndexed { i, pt ->
        val day = trends[i]
        val isSelected = (i == selectedIndex)
        val dotColor = day.recoveryStatus.color

        // Outer glow
        drawCircle(
            color = dotColor.copy(alpha = if (isSelected) 0.5f else 0.25f),
            radius = if (isSelected) 10.dp.toPx() else 6.dp.toPx(),
            center = pt
        )
        // Inner core
        drawCircle(
            color = dotColor,
            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
            center = pt
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = pt
        )
    }
}

// --- Pace & Heart Rate Intensity Dual-Line Renderer ---
private fun DrawScope.drawPaceIntensityMultiLine(
    trends: List<DayPerformanceTrend>,
    slotWidth: Float,
    chartHeight: Float,
    progress: Float,
    selectedIndex: Int?
) {
    // 1. Heart Rate Points (60 to 190 BPM)
    val hrPoints = trends.mapIndexed { i, day ->
        val x = (i + 0.5f) * slotWidth
        val hr = if (day.avgHeartRate > 0) day.avgHeartRate else 125
        val hrFrac = ((hr - 60f) / 130f).coerceIn(0f, 1f)
        val y = chartHeight - (hrFrac * chartHeight * progress)
        Offset(x, y)
    }

    // 2. Pace Points (3:30 to 8:00 /km -> 210s to 480s)
    val pacePoints = trends.mapIndexed { i, day ->
        val x = (i + 0.5f) * slotWidth
        val pace = if (day.avgPaceSecPerKm > 0) day.avgPaceSecPerKm else 330
        // Inverted: faster pace (lower sec) is higher on chart
        val paceFrac = (1f - ((pace - 210f) / 270f)).coerceIn(0.05f, 0.95f)
        val y = chartHeight - (paceFrac * chartHeight * progress)
        Offset(x, y)
    }

    // Draw HR Line (Coral)
    drawCurveLine(hrPoints, NeonCoral)

    // Draw Pace Line (SpeedColor / Emerald)
    drawCurveLine(pacePoints, SpeedColor)

    // Draw Points
    hrPoints.forEachIndexed { i, pt ->
        val isSelected = (i == selectedIndex)
        drawCircle(color = NeonCoral, radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(), center = pt)
    }
    pacePoints.forEachIndexed { i, pt ->
        val isSelected = (i == selectedIndex)
        drawCircle(color = SpeedColor, radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(), center = pt)
    }
}

private fun DrawScope.drawCurveLine(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        path.cubicTo((p0.x + p1.x) / 2f, p0.y, (p0.x + p1.x) / 2f, p1.y, p1.x, p1.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawGridAndReferenceLines(
    chartWidth: Float,
    chartHeight: Float,
    metricMode: TrendsMetricMode,
    textMeasurer: TextMeasurer
) {
    // 3 Horizontal Grid Lines
    for (step in 1..3) {
        val y = chartHeight * (step / 4f)
        drawLine(
            color = CyberBorder.copy(alpha = 0.6f),
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Mode Legend / Unit Label in Top Right Corner
    val topLabel = when (metricMode) {
        TrendsMetricMode.DISTANCE_VOLUME -> "KM DISTANCE"
        TrendsMetricMode.HRV_RECOVERY -> "HRV RMSSD (MS)"
        TrendsMetricMode.PACE_INTENSITY -> "HR (BPM) & PACE"
    }

    val textLayout = textMeasurer.measure(
        text = AnnotatedString(topLabel),
        style = TextStyle(
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    )
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(chartWidth - textLayout.size.width - 4.dp.toPx(), 4.dp.toPx())
    )
}
