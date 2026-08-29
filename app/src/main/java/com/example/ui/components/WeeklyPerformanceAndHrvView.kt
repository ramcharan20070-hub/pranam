package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.tracker.TrendsCalculator
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel

@Composable
fun WeeklyPerformanceAndHrvView(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedTimeframe by remember { mutableStateOf(TrendsTimeframe.WEEK_7_DAYS) }
    var selectedMetricMode by remember { mutableStateOf(TrendsMetricMode.DISTANCE_VOLUME) }

    // Generate Trends list from recorded workouts + physiological HRV models
    val trends = remember(sessions, selectedTimeframe, userProfile) {
        TrendsCalculator.generateTrends(sessions, selectedTimeframe, userProfile)
    }

    val summary = remember(trends, userProfile) {
        TrendsCalculator.computeSummary(trends, userProfile)
    }

    // Default selected index to today (last element)
    var selectedIndex by remember(trends) {
        mutableStateOf<Int?>(if (trends.isNotEmpty()) trends.size - 1 else null)
    }

    val selectedDay = remember(selectedIndex, trends) {
        selectedIndex?.let { idx -> trends.getOrNull(idx) } ?: trends.lastOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Controls (Timeframe + Mode Selectors) ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Timeframe Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERFORMANCE & HRV TRENDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrendsTimeframe.entries.forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.2f) else PulseTheme.colors.surfaceElevated)
                                .border(1.dp, if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border, RoundedCornerShape(12.dp))
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("timeframe_${tf.days}_days")
                        ) {
                            Text(
                                text = tf.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Metric Mode Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendsMetricMode.entries.forEach { mode ->
                    val isSelected = selectedMetricMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border, RoundedCornerShape(10.dp))
                            .clickable { selectedMetricMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- Interactive Canvas Data Visualization ---
        PerformanceTrendsChart(
            trends = trends,
            metricMode = selectedMetricMode,
            selectedIndex = selectedIndex,
            onSelectDay = { idx -> selectedIndex = idx }
        )

        // --- Interactive Scrubber HUD Tooltip Card ---
        if (selectedDay != null) {
            SelectedDayScrubberCard(day = selectedDay)
        }

        // --- Weekly Volume vs Goal Progress Card ---
        WeeklyGoalProgressCard(summary = summary)

        // --- Heart Rate Variability (HRV) Recovery Deep Dive Card ---
        HrvRecoveryReadinessCard(summary = summary, todayTrend = selectedDay)
    }
}

@Composable
private fun SelectedDayScrubberCard(day: DayPerformanceTrend) {
    CyberCard(
        borderColor = if (day.isToday) NeonGreen.copy(alpha = 0.6f) else PulseTheme.colors.borderBright,
        glowColor = day.recoveryStatus.color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (day.isToday) NeonGreen else day.recoveryStatus.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (day.isToday) "${day.dayLabel} (TODAY)" else day.dayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary
                    )
                    Text(
                        text = "${day.workoutCount} workouts • ${day.formattedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }
            }

            // Recovery Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(day.recoveryStatus.color.copy(alpha = 0.15f))
                    .border(1.dp, day.recoveryStatus.color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = when (day.recoveryStatus) {
                        HrvRecoveryStatus.OPTIMAL_PRIMED -> Icons.Default.CheckCircle
                        HrvRecoveryStatus.BALANCED_READY -> Icons.Default.FitnessCenter
                        HrvRecoveryStatus.MODERATE_FATIGUE -> Icons.Default.Info
                        HrvRecoveryStatus.STRAINED_RECOVERY -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = day.recoveryStatus.color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = day.recoveryStatus.shortLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = day.recoveryStatus.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4-Stat Scrubber Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BiometricStatBox(
                modifier = Modifier.weight(1f),
                label = "Distance",
                value = String.format("%.1f", day.totalDistanceMeters / 1000.0),
                unit = "km",
                accentColor = NeonGreen
            )
            BiometricStatBox(
                modifier = Modifier.weight(1f),
                label = "HRV (RMSSD)",
                value = "${day.hrvRmssdMs}",
                unit = "ms",
                accentColor = PulseTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BiometricStatBox(
                modifier = Modifier.weight(1f),
                label = "Avg Pace",
                value = if (day.avgPaceSecPerKm > 0) formatPace(day.avgPaceSecPerKm) else "--:--",
                unit = "/km",
                accentColor = SpeedColor
            )
            BiometricStatBox(
                modifier = Modifier.weight(1f),
                label = "Calories",
                value = "${day.caloriesBurned}",
                unit = "kcal",
                accentColor = NeonCoral
            )
        }
    }
}

@Composable
private fun WeeklyGoalProgressCard(summary: WeeklyPerformanceSummary) {
    CyberCard(
        borderColor = PulseTheme.colors.primary.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WEEKLY VOLUME GOAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", summary.totalDistanceKm)} / ${String.format("%.1f", summary.targetWeeklyKm)} KM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PulseTheme.colors.surfaceElevated)
                    .border(2.dp, PulseTheme.colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${summary.distanceGoalPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { (summary.distanceGoalPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PulseTheme.colors.primary,
            trackColor = PulseTheme.colors.border
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${summary.workoutCount} Workouts Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTheme.colors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${summary.totalCalories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun HrvRecoveryReadinessCard(
    summary: WeeklyPerformanceSummary,
    todayTrend: DayPerformanceTrend?
) {
    val hrv = todayTrend?.hrvRmssdMs ?: summary.avgHrvRmssd
    val recovery = todayTrend?.recoveryStatus ?: summary.currentRecoveryStatus

    CyberCard(
        borderColor = recovery.color.copy(alpha = 0.4f),
        glowColor = recovery.color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(recovery.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = recovery.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AUTONOMIC HRV & RECOVERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = recovery.color,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "RMSSD: $hrv ms • 7-Day Baseline: ${summary.avgHrvRmssd} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTheme.colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when (recovery) {
                HrvRecoveryStatus.OPTIMAL_PRIMED -> "Parasympathetic tone is dominant. Cardiovascular recovery is primed for threshold intervals or high-aerobic volume."
                HrvRecoveryStatus.BALANCED_READY -> "Optimal autonomic balance detected. Normal baseline recovery ready for standard aerobic training."
                HrvRecoveryStatus.MODERATE_FATIGUE -> "Elevated sympathetic activation. Moderate cardiovascular load recommended; favor Zone 2 aerobic volume."
                HrvRecoveryStatus.STRAINED_RECOVERY -> "High cumulative strain detected. Active recovery, mobility routines, and sleep optimization advised."
            },
            style = MaterialTheme.typography.bodySmall,
            color = PulseTheme.colors.textPrimary,
            lineHeight = 18.sp
        )
    }
}
