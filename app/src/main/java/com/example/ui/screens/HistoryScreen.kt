package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import androidx.compose.ui.window.Dialog
import com.example.model.GpsPoint
import com.example.model.WorkoutSessionEntity
import com.example.model.WorkoutType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    val totalDistMeters by viewModel.totalDistanceMeters.collectAsState()
    val totalCalories by viewModel.totalCaloriesBurned.collectAsState()
    val totalDurationSec by viewModel.totalDurationSeconds.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Trends & HRV, 1: Workout Logs
    var selectedSessionForDetail by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- Header ---
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = PulseTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANALYTICS & PERFORMANCE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Weekly performance curves, HRV trends & GPS telemetry logs",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted
                )
            }
        }

        // --- Top View Mode Switcher ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PulseTheme.colors.surfaceElevated)
                    .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) PulseTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 8.dp)
                        .testTag("tab_trends_hrv"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = if (activeTab == 0) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Trends & HRV",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == 0) PulseTheme.colors.primary else PulseTheme.colors.textMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) PulseTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 8.dp)
                        .testTag("tab_workout_logs"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = if (activeTab == 1) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Session Logs (${sessions.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == 1) PulseTheme.colors.primary else PulseTheme.colors.textMuted
                        )
                    }
                }
            }
        }

        if (activeTab == 0) {
            // --- Interactive Performance Trends & HRV Data Visualization Suite ---
            item {
                WeeklyPerformanceAndHrvView(viewModel = viewModel)
            }
        }

        // --- Aggregate Lifetime Statistics Banner ---
        item {
            CyberCard(
                borderColor = PulseTheme.colors.borderBright
            ) {
                Text(
                    text = "LIFETIME TOTALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = String.format("%.1f", totalDistMeters / 1000.0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = "TOTAL KM",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "$totalCalories",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = "CALORIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        val hours = totalDurationSec / 3600
                        val mins = (totalDurationSec % 3600) / 60
                        Text(
                            text = "${hours}h ${mins}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = "ACTIVE TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "${sessions.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = "SESSIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeTab == 1) {
            // --- Session Log List ---
            item {
                Text(
                    text = "RECORDED SESSIONS (${sessions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = PulseTheme.colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No workouts recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseTheme.colors.textMuted
                            )
                            Text(
                                text = "Complete your first GPS workout to view full biometric breakdowns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTheme.colors.textMuted
                            )
                        }
                    }
                }
            } else {
                items(sessions) { session ->
                    WorkoutHistoryCard(
                        session = session,
                        onClick = { selectedSessionForDetail = session }
                    )
                }
            }
        }
    }
}

    // Detailed Session Modal Dialog
    selectedSessionForDetail?.let { sessionDetail ->
        SessionDetailDialog(
            session = sessionDetail,
            onDismiss = { selectedSessionForDetail = null },
            onDelete = {
                viewModel.deleteSession(sessionDetail.id)
                selectedSessionForDetail = null
            }
        )
    }
}

@Composable
fun WorkoutHistoryCard(
    session: WorkoutSessionEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(session.startTime) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(session.startTime))
    }

    val type = WorkoutType.fromString(session.workoutType)

    CyberCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PulseTheme.colors.surfaceElevated)
                        .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getActivityIcon(type),
                        contentDescription = null,
                        tint = PulseTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = type.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PulseTheme.colors.textMuted
            )
        }

        if (session.startLocationName.isNotBlank() || session.endLocationName.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(PulseTheme.colors.surfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${session.startLocationName.ifBlank { "Start" }} → ${session.endLocationName.ifBlank { "Finish" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = formatDistance(session.distanceMeters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "DISTANCE (KM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontSize = 9.sp
                )
            }

            Column {
                Text(
                    text = formatDuration(session.durationSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "DURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontSize = 9.sp
                )
            }

            Column {
                Text(
                    text = formatPace(session.avgPaceSecPerKm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "AVG PACE (/KM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontSize = 9.sp
                )
            }

            Column {
                Text(
                    text = "${session.avgHeartRate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Zone3Aerobic
                )
                Text(
                    text = "AVG HR (BPM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    session: WorkoutSessionEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(session.startTime) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(session.startTime))
    }

    val type = WorkoutType.fromString(session.workoutType)

    // Parse route points from JSON
    val routePoints = remember(session.routePointsJson) {
        try {
            val list = mutableListOf<GpsPoint>()
            val array = JSONArray(session.routePointsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GpsPoint(
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
            emptyList<GpsPoint>()
        }
    }

    // Parse zone distribution
    val zoneTimes = remember(session.zoneTimesJson) {
        try {
            val map = mutableMapOf<Int, Long>()
            val obj = JSONObject(session.zoneTimesJson)
            for (zoneNum in 1..5) {
                map[zoneNum] = obj.optLong(zoneNum.toString(), 0L)
            }
            map
        } catch (e: Exception) {
            emptyMap<Int, Long>()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PulseTheme.colors.surfaceCard),
            border = BorderStroke(1.dp, PulseTheme.colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = type.title.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PulseTheme.colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GPS Route Overview & Google Maps
                if (routePoints.isNotEmpty()) {
                    GoogleMapRouteView(
                        routePoints = routePoints,
                        isTracking = false,
                        hasGpsFix = true,
                        allowExpand = true,
                        startLocationName = session.startLocationName,
                        endLocationName = session.endLocationName
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2x3 Primary Telemetry Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        value = formatPace(session.avgPaceSecPerKm),
                        unit = "/km",
                        accentColor = SpeedColor
                    )
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Avg HR",
                        value = "${session.avgHeartRate}",
                        unit = "bpm",
                        accentColor = Zone3Aerobic
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Calories",
                        value = "${session.caloriesBurned}",
                        unit = "kcal",
                        accentColor = NeonCoral
                    )
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Steps",
                        value = if (session.stepCount > 0) String.format("%,d", session.stepCount) else "0",
                        unit = "steps",
                        accentColor = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiometricStatBox(
                        modifier = Modifier.weight(1f),
                        label = "Cadence",
                        value = "${session.avgCadenceSpm}",
                        unit = "spm",
                        accentColor = CadenceColor
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

                // Heart Rate Zone Stacked Bar
                if (zoneTimes.isNotEmpty()) {
                    HeartRateZoneDistributionBar(zoneTimesSeconds = zoneTimes)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // AI Exercise Physiologist Deep Dive
                CyberCard(
                    borderColor = PulseTheme.colors.primary.copy(alpha = 0.4f),
                    backgroundColor = PulseTheme.colors.surfaceElevated
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PulseTheme.colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI PHYSIOLOGY ANALYSIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = session.aiCoachingSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTheme.colors.textPrimary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recovery Required: ${session.aiRecoveryHours}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stress: ${session.aiTrainingStress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Share Workout Report Options
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = com.example.tracker.WorkoutReportGenerator.generateFormattedTextReport(session, null)
                            com.example.tracker.WorkoutReportGenerator.shareTextReport(context, text, "${session.workoutType.uppercase()} Summary")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("history_share_text_button"),
                        border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseTheme.colors.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Text", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val bitmap = com.example.tracker.WorkoutReportGenerator.generateWorkoutSnapshotBitmap(context, session, null)
                            com.example.tracker.WorkoutReportGenerator.shareImageSnapshot(context, bitmap, session.workoutType)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("history_share_image_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.2f),
                            contentColor = NeonGreen
                        ),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Image", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Session Button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral),
                    border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Session Log", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
