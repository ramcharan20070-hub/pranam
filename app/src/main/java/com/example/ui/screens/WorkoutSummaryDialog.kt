package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.PostWorkoutAnalysis
import com.example.model.WorkoutSessionEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WorkoutSummaryDialog(
    session: WorkoutSessionEntity,
    analysis: PostWorkoutAnalysis?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val routePoints = androidx.compose.runtime.remember(session.routePointsJson) {
        try {
            val list = mutableListOf<com.example.model.GpsPoint>()
            val array = org.json.JSONArray(session.routePointsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.model.GpsPoint(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        altitude = obj.optDouble("alt", 0.0),
                        speedMps = obj.optDouble("spd", 0.0).toFloat(),
                        timestamp = obj.optLong("time", session.startTime),
                        heartRate = obj.optInt("hr", session.avgHeartRate),
                        cadence = obj.optInt("cad", session.avgCadenceSpm)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList<com.example.model.GpsPoint>()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("workout_summary_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonGreen)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SESSION COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${session.workoutType.uppercase()} WORKOUT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Glowing AI Performance Score Circle
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated)
                        .border(
                            3.dp,
                            Brush.sweepGradient(listOf(NeonCyan, NeonGreen, NeonCoral, NeonCyan)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${session.aiPerformanceScore}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "AI SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Route Map if points available
                if (routePoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GoogleMapRouteView(
                        routePoints = routePoints,
                        isTracking = false,
                        hasGpsFix = true,
                        allowExpand = false,
                        startLocationName = session.startLocationName,
                        endLocationName = session.endLocationName
                    )
                }

                if (session.startLocationName.isNotBlank() || session.endLocationName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberCard(backgroundColor = CyberSurfaceElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "START POINT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    text = session.startLocationName.ifBlank { "GPS Start Point" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonCoral)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "END POINT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCoral,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    text = session.endLocationName.ifBlank { "GPS Finish Point" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Core Telemetry Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Distance",
                        value = formatDistance(session.distanceMeters),
                        unit = "km",
                        accentColor = NeonGreen
                    )
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Duration",
                        value = formatDuration(session.durationSeconds),
                        unit = "",
                        accentColor = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Avg Pace",
                        value = formatPace(session.avgPaceSecPerKm),
                        unit = "/km",
                        accentColor = SpeedColor
                    )
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Steps",
                        value = if (session.stepCount > 0) String.format("%,d", session.stepCount) else "0",
                        unit = "steps",
                        accentColor = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Burned",
                        value = "${session.caloriesBurned}",
                        unit = "kcal",
                        accentColor = CalorieColor
                    )
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Elevation",
                        value = String.format("+%.0f", session.elevationGainMeters),
                        unit = "m",
                        accentColor = ElevationColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Exercise Physiologist Deep Dive Card
                CyberCard(
                    borderColor = NeonCyan.copy(alpha = 0.5f),
                    backgroundColor = CyberSurfaceElevated
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI PHYSIOLOGICAL EVALUATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generating deep telemetry breakdown...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    } else {
                        Text(
                            text = session.aiCoachingSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Recovery and Stress Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Recovery: ${session.aiRecoveryHours} Hours",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = session.aiTrainingStress,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (analysis?.strengths?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "KEY STRENGTHS:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            analysis.strengths.forEach { s ->
                                Text(
                                    text = "• $s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Share Workout Options ---
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = com.example.tracker.WorkoutReportGenerator.generateFormattedTextReport(session, analysis)
                            com.example.tracker.WorkoutReportGenerator.shareTextReport(context, text, "${session.workoutType.uppercase()} Summary")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_text_report_button"),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Text", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val bitmap = com.example.tracker.WorkoutReportGenerator.generateWorkoutSnapshotBitmap(context, session, analysis)
                            com.example.tracker.WorkoutReportGenerator.shareImageSnapshot(context, bitmap, session.workoutType)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_image_report_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.2f),
                            contentColor = NeonGreen
                        ),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Image", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                CyberButton(
                    text = "SAVE & CONTINUE",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    icon = Icons.Default.Done,
                    testTag = "dismiss_summary_button"
                )
            }
        }
    }
}
