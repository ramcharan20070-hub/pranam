package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveBiometricState
import com.example.model.WorkoutType
import com.example.tracker.BleHeartRatePayload
import com.example.tracker.BlePermissionHelper
import com.example.tracker.BleTrackingPermissionDialog
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveTrackerScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.liveState.collectAsState()
    val isSimulation by viewModel.isSimulationEnabled.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val connectedWatch by viewModel.connectedWatch.collectAsState()
    val latestBlePayload by viewModel.latestBlePayload.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val personalizedRecs by viewModel.personalizedRecommendations.collectAsState()

    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.RUNNING) }
    var showSmartwatchDialog by remember { mutableStateOf(false) }
    var showBlePermissionDialog by remember { mutableStateOf(false) }
    var showActiveWorkoutBackDialog by remember { mutableStateOf(false) }
    var trackerTab by remember { mutableStateOf(0) } // 0: Live Tracker, 1: Daily Fit & Google Fit

    // BackHandler: Protect active tracking from accidental back navigation
    BackHandler(enabled = state.isTracking) {
        showActiveWorkoutBackDialog = true
    }

    if (showActiveWorkoutBackDialog) {
        AlertDialog(
            onDismissRequest = { showActiveWorkoutBackDialog = false },
            title = {
                Text(
                    text = "Workout in Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "You have an active ${state.workoutType.title} session running (${state.durationSeconds / 60}m ${state.durationSeconds % 60}s). What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActiveWorkoutBackDialog = false
                        viewModel.finishWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                ) {
                    Text("Finish & Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showActiveWorkoutBackDialog = false
                            if (state.isPaused) {
                                viewModel.resumeWorkout()
                            } else {
                                viewModel.pauseWorkout()
                            }
                        }
                    ) {
                        Text(if (state.isPaused) "Resume" else "Pause", color = PulseTheme.colors.primary)
                    }
                    TextButton(
                        onClick = { showActiveWorkoutBackDialog = false }
                    ) {
                        Text("Continue", color = PulseTheme.colors.textMuted)
                    }
                }
            },
            containerColor = PulseTheme.colors.surfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Direct permission launcher callback
    val startTrackingWithPermissions: () -> Unit = {
        if (!BlePermissionHelper.hasAllRequiredPermissions(context)) {
            showBlePermissionDialog = true
        } else {
            viewModel.startWorkout(selectedWorkoutType)
        }
    }

    // Runtime Permission Handler for GPS & Sensors
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 12.dp)
        ) {
            // --- Header Status Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!state.isTracking) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_pranam_logo),
                                contentDescription = "Pranam Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isPaused) NeonAmber else NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (state.isTracking) (if (state.isPaused) "SESSION PAUSED" else "LIVE TRACKING") else "PRANAM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = if (state.isTracking) state.workoutType.title.uppercase() else "Select activity & start session",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Theme Toggle Quick Chip (Black Theme <-> White Theme)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PulseTheme.colors.surfaceElevated)
                            .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(20.dp))
                            .clickable { viewModel.toggleTheme() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("theme_quick_toggle")
                    ) {
                        Icon(
                            imageVector = if (PulseTheme.colors.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Theme",
                            tint = if (PulseTheme.colors.isDark) NeonCyan else NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (PulseTheme.colors.isDark) "DARK" else "LIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Smartwatch Quick Connect / Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (connectedWatch != null) NeonGreen.copy(alpha = 0.2f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (connectedWatch != null) NeonGreen else PulseTheme.colors.border, RoundedCornerShape(20.dp))
                            .clickable { showSmartwatchDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("smartwatch_status_chip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = if (connectedWatch != null) NeonGreen else PulseTheme.colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (connectedWatch != null) {
                                val hr = connectedWatch?.liveHeartRateBpm
                                val steps = connectedWatch?.liveStepCount
                                when {
                                    hr != null && steps != null -> "$hr BPM • ${steps}st"
                                    hr != null -> "$hr BPM"
                                    steps != null -> "${steps}st"
                                    else -> "WATCH"
                                }
                            } else "WATCH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectedWatch != null) NeonGreen else PulseTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Simulation / Demo Mode Switch Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSimulation) PulseTheme.colors.primary.copy(alpha = 0.2f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (isSimulation) PulseTheme.colors.primary else PulseTheme.colors.border, RoundedCornerShape(20.dp))
                            .clickable { viewModel.toggleSimulation(!isSimulation) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("simulation_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSimulation) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = if (isSimulation) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSimulation) "SIM" else "SIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSimulation) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Sub-Navigation Tabs (When Not Actively Tracking) ---
            if (!state.isTracking) {
                TabRow(
                    selectedTabIndex = trackerTab,
                    containerColor = PulseTheme.colors.surfaceElevated,
                    contentColor = PulseTheme.colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = trackerTab == 0,
                        onClick = { trackerTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Workout Tracker", fontWeight = if (trackerTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = PulseTheme.colors.primary,
                        unselectedContentColor = PulseTheme.colors.textMuted
                    )
                    Tab(
                        selected = trackerTab == 1,
                        onClick = { trackerTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fit & Google Sync", fontWeight = if (trackerTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = NeonGreen,
                        unselectedContentColor = PulseTheme.colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (trackerTab == 1 && !state.isTracking) {
                // Render Daily Fit & Google Fit Hub
                DailyFitDashboard(
                    viewModel = viewModel,
                    onOpenSmartwatchHub = { showSmartwatchDialog = true }
                )
            } else {
                if (isWideScreen) {
                    // --- Adaptive Tablet / Wide 2-Column Split View ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Column: Activity Picker & GPS Map Route Engine
                        Column(modifier = Modifier.weight(1.1f)) {
                            if (!state.isTracking) {
                                PersonalizedRecommendationsCard(
                                    recommendations = personalizedRecs,
                                    userProfile = userProfile,
                                    onStartWorkout = { item ->
                                        val mappedType = WorkoutType.fromString(item.workoutType)
                                        selectedWorkoutType = mappedType
                                        viewModel.startWorkout(mappedType)
                                    },
                                    onEditFitnessSetup = { viewModel.openSetupWizard() }
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "CHOOSE ACTIVITY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.textMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(WorkoutType.entries) { type ->
                                        val isSelected = selectedWorkoutType == type
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedWorkoutType = type }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                                .testTag("activity_${type.name.lowercase()}")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = getActivityIcon(type),
                                                    contentDescription = null,
                                                    tint = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = type.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelected) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Interactive GPS Route View
                            GoogleMapRouteView(
                                routePoints = state.routePoints,
                                isTracking = state.isTracking,
                                hasGpsFix = state.hasGpsFix,
                                allowExpand = true,
                                startLocationName = state.startLocationName,
                                endLocationName = state.endLocationName,
                                currentLat = state.currentLat,
                                currentLng = state.currentLng
                            )
                        }

                        // Right Column: Telemetry Matrix, AI Coach & Action Controls
                        Column(modifier = Modifier.weight(1f)) {
                            // Primary Live Telemetry HUD
                            CyberCard(
                                glowColor = if (state.isTracking) state.currentZone.getColor() else null
                            ) {
                                HeartRateTachometer(
                                    currentHr = state.currentHeartRate,
                                    maxHr = userProfile.maxHeartRate,
                                    currentZone = state.currentZone,
                                    hrSource = state.hrSource
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Duration",
                                        value = formatDuration(state.durationSeconds),
                                        unit = "",
                                        accentColor = PulseTheme.colors.primary,
                                        icon = Icons.Default.Timer,
                                        testTag = "telemetry_duration"
                                    )
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Distance",
                                        value = formatDistance(state.distanceMeters),
                                        unit = "km",
                                        accentColor = NeonGreen,
                                        icon = Icons.Default.Straighten,
                                        testTag = "telemetry_distance"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Pace",
                                        value = formatPace(state.currentPaceSecPerKm),
                                        unit = "/km",
                                        accentColor = SpeedColor,
                                        icon = Icons.Default.Speed,
                                        testTag = "telemetry_pace"
                                    )
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Steps",
                                        value = if (state.stepCount > 0) String.format("%,d", state.stepCount) else "0",
                                        unit = "steps",
                                        accentColor = NeonCyan,
                                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                        testTag = "telemetry_steps"
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
                                        value = "${state.caloriesBurned}",
                                        unit = "kcal",
                                        accentColor = CalorieColor,
                                        icon = Icons.Default.LocalFireDepartment,
                                        testTag = "telemetry_calories"
                                    )
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Cadence",
                                        value = if (state.currentCadenceSpm > 0) "${state.currentCadenceSpm}" else "--",
                                        unit = "spm",
                                        accentColor = CadenceColor,
                                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                        testTag = "telemetry_cadence"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BiometricStatBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Elevation",
                                        value = String.format("+%.0f", state.elevationGainMeters),
                                        unit = "m",
                                        accentColor = ElevationColor,
                                        icon = Icons.Default.Terrain,
                                        testTag = "telemetry_elevation"
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                StaminaFatigueGauge(
                                    staminaPercent = state.staminaPercent,
                                    fatigueScore = state.fatigueScore
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                HeartRateZoneDistributionBar(zoneTimesSeconds = state.zoneTimesSeconds)

                                Spacer(modifier = Modifier.height(14.dp))

                                // Active BLE Heart Rate Monitor Status
                                BleLiveTelemetryBadge(
                                    connectedWatch = connectedWatch,
                                    latestPayload = latestBlePayload,
                                    isTracking = state.isTracking,
                                    onConnectClick = { showSmartwatchDialog = true },
                                    onDisconnectClick = { connectedWatch?.let { viewModel.disconnectSmartwatch(it) } }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Tactical AI Coach Card
                            CyberCard(
                                borderColor = PulseTheme.colors.primary.copy(alpha = 0.4f),
                                glowColor = PulseTheme.colors.primary
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(PulseTheme.colors.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PulseTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "REAL-TIME AI TACTICAL COACH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseTheme.colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    if (state.isAiAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = PulseTheme.colors.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val tipText = state.liveAiTip
                                    ?: if (state.isTracking) "Analyzing live biometric stream... Target steady Zone 2/3 aerobic respiration."
                                    else "Start a workout to receive live real-time physiological pacing feedback and tactical interval adjustments."

                                Text(
                                    text = tipText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PulseTheme.colors.textPrimary,
                                    lineHeight = 20.sp
                                )

                                if (state.isTracking) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.requestLiveAiCoaching() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("request_ai_cue_button"),
                                        border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseTheme.colors.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Get Instant Tactical Cue",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons
                            if (!state.isTracking) {
                                CyberButton(
                                    text = "START ${selectedWorkoutType.title.uppercase()}",
                                    onClick = startTrackingWithPermissions,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonGreen,
                                    icon = Icons.Default.PlayArrow,
                                    testTag = "start_workout_button"
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (state.isPaused) {
                                        CyberButton(
                                            text = "RESUME",
                                            onClick = { viewModel.resumeWorkout() },
                                            modifier = Modifier.weight(1f),
                                            color = NeonGreen,
                                            icon = Icons.Default.PlayArrow,
                                            testTag = "resume_workout_button"
                                        )
                                    } else {
                                        CyberButton(
                                            text = "PAUSE",
                                            onClick = { viewModel.pauseWorkout() },
                                            modifier = Modifier.weight(1f),
                                            color = NeonAmber,
                                            icon = Icons.Default.Pause,
                                            testTag = "pause_workout_button"
                                        )
                                    }

                                    CyberButton(
                                        text = "FINISH",
                                        onClick = { viewModel.finishWorkout() },
                                        modifier = Modifier.weight(1f),
                                        color = NeonCoral,
                                        icon = Icons.Default.Stop,
                                        testTag = "finish_workout_button"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // --- Single Column Vertical Phone Flow ---
                    if (!state.isTracking) {
                        PersonalizedRecommendationsCard(
                            recommendations = personalizedRecs,
                            userProfile = userProfile,
                            onStartWorkout = { item ->
                                val mappedType = WorkoutType.fromString(item.workoutType)
                                selectedWorkoutType = mappedType
                                viewModel.startWorkout(mappedType)
                            },
                            onEditFitnessSetup = { viewModel.openSetupWizard() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CHOOSE ACTIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(WorkoutType.entries) { type ->
                                val isSelected = selectedWorkoutType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedWorkoutType = type }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("activity_${type.name.lowercase()}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getActivityIcon(type),
                                            contentDescription = null,
                                            tint = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = type.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- Live Interactive Google Map / GPS Route View ---
                    GoogleMapRouteView(
                        routePoints = state.routePoints,
                        isTracking = state.isTracking,
                        hasGpsFix = state.hasGpsFix,
                        allowExpand = true,
                        startLocationName = state.startLocationName,
                        endLocationName = state.endLocationName,
                        currentLat = state.currentLat,
                        currentLng = state.currentLng
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Primary Live Telemetry HUD ---
                    CyberCard(
                        glowColor = if (state.isTracking) state.currentZone.getColor() else null
                    ) {
                        // Heart Rate Tachometer & Zone Meter
                        HeartRateTachometer(
                            currentHr = state.currentHeartRate,
                            maxHr = userProfile.maxHeartRate,
                            currentZone = state.currentZone,
                            hrSource = state.hrSource
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4-Quadrant Primary Telemetry Matrix
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Duration",
                                value = formatDuration(state.durationSeconds),
                                unit = "",
                                accentColor = PulseTheme.colors.primary,
                                icon = Icons.Default.Timer,
                                testTag = "telemetry_duration"
                            )
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Distance",
                                value = formatDistance(state.distanceMeters),
                                unit = "km",
                                accentColor = NeonGreen,
                                icon = Icons.Default.Straighten,
                                testTag = "telemetry_distance"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Pace",
                                value = formatPace(state.currentPaceSecPerKm),
                                unit = "/km",
                                accentColor = SpeedColor,
                                icon = Icons.Default.Speed,
                                testTag = "telemetry_pace"
                            )
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Steps",
                                value = if (state.stepCount > 0) String.format("%,d", state.stepCount) else "0",
                                unit = "steps",
                                accentColor = NeonCyan,
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                testTag = "telemetry_steps"
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
                                value = "${state.caloriesBurned}",
                                unit = "kcal",
                                accentColor = CalorieColor,
                                icon = Icons.Default.LocalFireDepartment,
                                testTag = "telemetry_calories"
                            )
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Cadence",
                                value = if (state.currentCadenceSpm > 0) "${state.currentCadenceSpm}" else "--",
                                unit = "spm",
                                accentColor = CadenceColor,
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                testTag = "telemetry_cadence"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BiometricStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Elevation",
                                value = String.format("+%.0f", state.elevationGainMeters),
                                unit = "m",
                                accentColor = ElevationColor,
                                icon = Icons.Default.Terrain,
                                testTag = "telemetry_elevation"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stamina & Fatigue Battery Bar
                        StaminaFatigueGauge(
                            staminaPercent = state.staminaPercent,
                            fatigueScore = state.fatigueScore
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Time in Zones Distribution Stack
                        HeartRateZoneDistributionBar(zoneTimesSeconds = state.zoneTimesSeconds)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active BLE Heart Rate Monitor Status
                        BleLiveTelemetryBadge(
                            connectedWatch = connectedWatch,
                            latestPayload = latestBlePayload,
                            isTracking = state.isTracking,
                            onConnectClick = { showSmartwatchDialog = true },
                            onDisconnectClick = { connectedWatch?.let { viewModel.disconnectSmartwatch(it) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Real-Time AI Tactical Coach Card ---
                    CyberCard(
                        borderColor = PulseTheme.colors.primary.copy(alpha = 0.4f),
                        glowColor = PulseTheme.colors.primary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PulseTheme.colors.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PulseTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REAL-TIME AI TACTICAL COACH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (state.isAiAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PulseTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val tipText = state.liveAiTip
                            ?: if (state.isTracking) "Analyzing live biometric stream... Target steady Zone 2/3 aerobic respiration."
                            else "Start a workout to receive live real-time physiological pacing feedback and tactical interval adjustments."

                        Text(
                            text = tipText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseTheme.colors.textPrimary,
                            lineHeight = 20.sp
                        )

                        if (state.isTracking) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.requestLiveAiCoaching() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("request_ai_cue_button"),
                                border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseTheme.colors.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Get Instant Tactical Cue",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Action Buttons ---
                    if (!state.isTracking) {
                        CyberButton(
                            text = "START ${selectedWorkoutType.title.uppercase()}",
                            onClick = startTrackingWithPermissions,
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonGreen,
                            icon = Icons.Default.PlayArrow,
                            testTag = "start_workout_button"
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (state.isPaused) {
                                CyberButton(
                                    text = "RESUME",
                                    onClick = { viewModel.resumeWorkout() },
                                    modifier = Modifier.weight(1f),
                                    color = NeonGreen,
                                    icon = Icons.Default.PlayArrow,
                                    testTag = "resume_workout_button"
                                )
                            } else {
                                CyberButton(
                                    text = "PAUSE",
                                    onClick = { viewModel.pauseWorkout() },
                                    modifier = Modifier.weight(1f),
                                    color = NeonAmber,
                                    icon = Icons.Default.Pause,
                                    testTag = "pause_workout_button"
                                )
                            }

                            CyberButton(
                                text = "FINISH",
                                onClick = { viewModel.finishWorkout() },
                                modifier = Modifier.weight(1f),
                                color = NeonCoral,
                                icon = Icons.Default.Stop,
                                testTag = "finish_workout_button"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // BLE & GPS Permission Request Dialog
    if (showBlePermissionDialog) {
        BleTrackingPermissionDialog(
            onDismiss = { showBlePermissionDialog = false },
            onPermissionsGranted = {
                showBlePermissionDialog = false
                viewModel.startWorkout(selectedWorkoutType)
            }
        )
    }

    // Smartwatch Connection Dialog Modal
    if (showSmartwatchDialog) {
        SmartwatchConnectDialog(
            viewModel = viewModel,
            onDismiss = { showSmartwatchDialog = false }
        )
    }
}

@Composable
fun BleLiveTelemetryBadge(
    connectedWatch: com.example.model.SmartwatchDevice?,
    latestPayload: BleHeartRatePayload?,
    isTracking: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (connectedWatch != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .testTag("ble_hr_telemetry_badge"),
            colors = CardDefaults.cardColors(containerColor = PulseTheme.colors.surfaceElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = "BLE Monitor Connected",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = connectedWatch.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "BLE ECG",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                        Text(
                            text = if (latestPayload?.batteryPercent != null) {
                                "Battery: ${latestPayload.batteryPercent}% • Auto-reconnect on"
                            } else {
                                "${connectedWatch.brand.brandName} • Live Active Stream"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (connectedWatch.liveHeartRateBpm != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = NeonCoral,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${connectedWatch.liveHeartRateBpm}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCoral
                                )
                                Text(
                                    text = " BPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                            if (latestPayload?.hrvRmssdMs != null) {
                                Text(
                                    text = "HRV: ${String.format("%.0f", latestPayload.hrvRmssdMs)} ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onDisconnectClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect BLE",
                                tint = PulseTheme.colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    } else if (!isTracking) {
        // Subtle discoverable prompt to connect BLE sensor
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                .clickable { onConnectClick() }
                .testTag("ble_hr_connect_prompt"),
            colors = CardDefaults.cardColors(containerColor = PulseTheme.colors.surfaceElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = PulseTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connect BLE Chest Strap / Heart Rate Monitor",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "Pair",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

fun getActivityIcon(type: WorkoutType) = when (type) {
    WorkoutType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    WorkoutType.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    WorkoutType.TRAIL_RUN -> Icons.Default.Terrain
    WorkoutType.HIIT_SPRINT -> Icons.Default.FlashOn
    WorkoutType.POWER_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    WorkoutType.HIKING -> Icons.Default.Landscape
    WorkoutType.ROWING -> Icons.Default.Rowing
}
