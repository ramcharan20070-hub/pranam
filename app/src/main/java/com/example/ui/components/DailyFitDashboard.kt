package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyFitStats
import com.example.model.GoogleFitSyncResult
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyFitDashboard(
    viewModel: FitnessViewModel,
    onOpenSmartwatchHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyStats by viewModel.dailyStats.collectAsState()
    val isGoogleFitSyncing by viewModel.isSyncingWithGoogleFit.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    val connectedWatch by viewModel.connectedWatch.collectAsState()

    var showSyncConfirmation by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // --- 2-Column Responsive Layout for Tablets/Expanded screens ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1: Sync status, Smartwatch card, Activity Rings
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Google Fit Cloud Sync Card
                    CyberCard(
                        borderColor = if (dailyStats.isGoogleFitSynced) NeonGreen.copy(alpha = 0.5f) else PulseTheme.colors.primary.copy(alpha = 0.5f),
                        glowColor = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.primary
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
                                        .background(if (dailyStats.isGoogleFitSynced) NeonGreen.copy(alpha = 0.15f) else PulseTheme.colors.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "GOOGLE FIT & HEALTH CONNECT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseTheme.colors.textPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = if (dailyStats.isGoogleFitSynced) "Cloud Sync Active • Real-time Data" else "Sync Pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.textMuted
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.syncWithGoogleFit {
                                        showSyncConfirmation = true
                                    }
                                },
                                enabled = !isGoogleFitSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dailyStats.isGoogleFitSynced) PulseTheme.colors.surfaceElevated else PulseTheme.colors.primary,
                                    contentColor = if (dailyStats.isGoogleFitSynced) PulseTheme.colors.primary else Color.Black
                                ),
                                border = if (dailyStats.isGoogleFitSynced) BorderStroke(1.dp, PulseTheme.colors.primary) else null,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sync_google_fit_button")
                            ) {
                                if (isGoogleFitSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PulseTheme.colors.primary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (showSyncConfirmation && lastSyncResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ ${lastSyncResult!!.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Smartwatch Quick Access Bar
                    CyberCard(
                        borderColor = if (connectedWatch != null) NeonGreen.copy(alpha = 0.4f) else PulseTheme.colors.border
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = if (connectedWatch != null) NeonGreen else PulseTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (connectedWatch != null) connectedWatch!!.name else "SMARTWATCH & SENSORS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = if (connectedWatch != null) "Connected • ${connectedWatch!!.batteryPercent}% Battery • HR Streaming" else "Pair Garmin, WearOS, Apple Watch, Polar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (connectedWatch != null) NeonGreen else PulseTheme.colors.textMuted
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenSmartwatchHub,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("manage_watches_button")
                            ) {
                                Text(
                                    text = if (connectedWatch != null) "Manage" else "Pair Watch",
                                    color = PulseTheme.colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Tri-Metric Radial Activity Rings
                    CyberCard {
                        Text(
                            text = "DAILY ACTIVITY & TARGETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val stepFrac = (dailyStats.stepsCount.toFloat() / dailyStats.stepGoal).coerceIn(0f, 1f)
                            RadialProgressGauge(
                                progress = stepFrac,
                                valueText = "${dailyStats.stepsCount}",
                                labelText = "STEPS",
                                targetText = "/ ${dailyStats.stepGoal}",
                                ringColor = NeonCyan,
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk
                            )

                            val calFrac = (dailyStats.activeCaloriesBurned.toFloat() / dailyStats.calorieGoal).coerceIn(0f, 1f)
                            RadialProgressGauge(
                                progress = calFrac,
                                valueText = "${dailyStats.activeCaloriesBurned}",
                                labelText = "ACTIVE KCAL",
                                targetText = "/ ${dailyStats.calorieGoal}",
                                ringColor = NeonCoral,
                                icon = Icons.Default.LocalFireDepartment
                            )

                            val hpFrac = (dailyStats.heartPoints.toFloat() / dailyStats.heartPointsGoal).coerceIn(0f, 1f)
                            RadialProgressGauge(
                                progress = hpFrac,
                                valueText = "${dailyStats.heartPoints}",
                                labelText = "HEART PTS",
                                targetText = "/ ${dailyStats.heartPointsGoal}",
                                ringColor = NeonGreen,
                                icon = Icons.Default.Favorite
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.addDailySteps(500) },
                                modifier = Modifier.testTag("add_500_steps_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PulseTheme.colors.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add 500 Steps", color = PulseTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Column 2: Metabolism, Hydration, Sleep
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Energy Balance & Basal Metabolism (BMR)
                    CyberCard {
                        Text(
                            text = "METABOLIC ENERGY EXPENDITURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted,
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
                                    text = "${dailyStats.totalCaloriesBurned} kcal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary
                                )
                                Text(text = "TOTAL DAILY BURN", style = MaterialTheme.typography.labelSmall, color = NeonAmber, fontSize = 9.sp)
                            }

                            Column {
                                Text(
                                    text = "${dailyStats.activeCaloriesBurned} kcal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCoral
                                )
                                Text(text = "ACTIVE EXERCISE", style = MaterialTheme.typography.labelSmall, color = PulseTheme.colors.textMuted, fontSize = 9.sp)
                            }

                            Column {
                                val bmr = dailyStats.totalCaloriesBurned - dailyStats.activeCaloriesBurned
                                Text(
                                    text = "$bmr kcal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                                Text(text = "BASAL METABOLIC RATE", style = MaterialTheme.typography.labelSmall, color = PulseTheme.colors.textMuted, fontSize = 9.sp)
                            }
                        }
                    }

                    // Hydration Tracker
                    CyberCard(
                        borderColor = Color(0xFF00B0FF).copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF00B0FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "HYDRATION TRACKER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00B0FF),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "${dailyStats.waterIntakeMl} / ${dailyStats.waterGoalMl} mL (${((dailyStats.waterIntakeMl.toFloat() / dailyStats.waterGoalMl) * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseTheme.colors.textPrimary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.addWaterIntake(250) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF).copy(alpha = 0.2f), contentColor = Color(0xFF00B0FF)),
                                    border = BorderStroke(1.dp, Color(0xFF00B0FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("add_250ml_water_button")
                                ) {
                                    Text("+250mL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.addWaterIntake(500) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("add_500ml_water_button")
                                ) {
                                    Text("+500mL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val waterProgress = (dailyStats.waterIntakeMl.toFloat() / dailyStats.waterGoalMl).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { waterProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00B0FF),
                            trackColor = PulseTheme.colors.border
                        )
                    }

                    // Sleep & Recovery Score
                    CyberCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SLEEP & READINESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dailyStats.sleepHours} Hours Restorative Sleep",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${dailyStats.sleepQualityScore}% READINESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- Phone Vertical Flow ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Google Fit Cloud Sync Card
                CyberCard(
                    borderColor = if (dailyStats.isGoogleFitSynced) NeonGreen.copy(alpha = 0.5f) else PulseTheme.colors.primary.copy(alpha = 0.5f),
                    glowColor = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.primary
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
                                    .background(if (dailyStats.isGoogleFitSynced) NeonGreen.copy(alpha = 0.15f) else PulseTheme.colors.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GOOGLE FIT & HEALTH CONNECT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (dailyStats.isGoogleFitSynced) "Cloud Sync Active • Real-time Data" else "Sync Pending",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.textMuted
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.syncWithGoogleFit {
                                    showSyncConfirmation = true
                                }
                            },
                            enabled = !isGoogleFitSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dailyStats.isGoogleFitSynced) PulseTheme.colors.surfaceElevated else PulseTheme.colors.primary,
                                contentColor = if (dailyStats.isGoogleFitSynced) PulseTheme.colors.primary else Color.Black
                            ),
                            border = if (dailyStats.isGoogleFitSynced) BorderStroke(1.dp, PulseTheme.colors.primary) else null,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sync_google_fit_button")
                        ) {
                            if (isGoogleFitSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PulseTheme.colors.primary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showSyncConfirmation && lastSyncResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ ${lastSyncResult!!.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen,
                            fontSize = 11.sp
                        )
                    }
                }

                // Smartwatch Quick Access Bar
                CyberCard(
                    borderColor = if (connectedWatch != null) NeonGreen.copy(alpha = 0.4f) else PulseTheme.colors.border
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = if (connectedWatch != null) NeonGreen else PulseTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (connectedWatch != null) connectedWatch!!.name else "SMARTWATCH & SENSORS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary
                                )
                                Text(
                                    text = if (connectedWatch != null) "Connected • ${connectedWatch!!.batteryPercent}% Battery • HR Streaming" else "Pair Garmin, WearOS, Apple Watch, Polar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (connectedWatch != null) NeonGreen else PulseTheme.colors.textMuted
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenSmartwatchHub,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("manage_watches_button")
                        ) {
                            Text(
                                text = if (connectedWatch != null) "Manage" else "Pair Watch",
                                color = PulseTheme.colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tri-Metric Radial Activity Rings
                CyberCard {
                    Text(
                        text = "DAILY ACTIVITY & TARGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepFrac = (dailyStats.stepsCount.toFloat() / dailyStats.stepGoal).coerceIn(0f, 1f)
                        RadialProgressGauge(
                            progress = stepFrac,
                            valueText = "${dailyStats.stepsCount}",
                            labelText = "STEPS",
                            targetText = "/ ${dailyStats.stepGoal}",
                            ringColor = NeonCyan,
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk
                        )

                        val calFrac = (dailyStats.activeCaloriesBurned.toFloat() / dailyStats.calorieGoal).coerceIn(0f, 1f)
                        RadialProgressGauge(
                            progress = calFrac,
                            valueText = "${dailyStats.activeCaloriesBurned}",
                            labelText = "ACTIVE KCAL",
                            targetText = "/ ${dailyStats.calorieGoal}",
                            ringColor = NeonCoral,
                            icon = Icons.Default.LocalFireDepartment
                        )

                        val hpFrac = (dailyStats.heartPoints.toFloat() / dailyStats.heartPointsGoal).coerceIn(0f, 1f)
                        RadialProgressGauge(
                            progress = hpFrac,
                            valueText = "${dailyStats.heartPoints}",
                            labelText = "HEART PTS",
                            targetText = "/ ${dailyStats.heartPointsGoal}",
                            ringColor = NeonGreen,
                            icon = Icons.Default.Favorite
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.addDailySteps(500) },
                            modifier = Modifier.testTag("add_500_steps_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PulseTheme.colors.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add 500 Steps", color = PulseTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Energy Balance & Basal Metabolism (BMR)
                CyberCard {
                    Text(
                        text = "METABOLIC ENERGY EXPENDITURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted,
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
                                text = "${dailyStats.totalCaloriesBurned} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.colors.textPrimary
                            )
                            Text(text = "TOTAL DAILY BURN", style = MaterialTheme.typography.labelSmall, color = NeonAmber, fontSize = 9.sp)
                        }

                        Column {
                            Text(
                                text = "${dailyStats.activeCaloriesBurned} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonCoral
                            )
                            Text(text = "ACTIVE EXERCISE", style = MaterialTheme.typography.labelSmall, color = PulseTheme.colors.textMuted, fontSize = 9.sp)
                        }

                        Column {
                            val bmr = dailyStats.totalCaloriesBurned - dailyStats.activeCaloriesBurned
                            Text(
                                text = "$bmr kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Text(text = "BASAL METABOLIC RATE", style = MaterialTheme.typography.labelSmall, color = PulseTheme.colors.textMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Hydration Tracker
                CyberCard(
                    borderColor = Color(0xFF00B0FF).copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF00B0FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "HYDRATION TRACKER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00B0FF),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${dailyStats.waterIntakeMl} / ${dailyStats.waterGoalMl} mL (${((dailyStats.waterIntakeMl.toFloat() / dailyStats.waterGoalMl) * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.addWaterIntake(250) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF).copy(alpha = 0.2f), contentColor = Color(0xFF00B0FF)),
                                border = BorderStroke(1.dp, Color(0xFF00B0FF)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_250ml_water_button")
                            ) {
                                Text("+250mL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.addWaterIntake(500) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_500ml_water_button")
                            ) {
                                Text("+500mL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val waterProgress = (dailyStats.waterIntakeMl.toFloat() / dailyStats.waterGoalMl).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00B0FF),
                        trackColor = PulseTheme.colors.border
                    )
                }

                // Sleep & Recovery Score
                CyberCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SLEEP & READINESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dailyStats.sleepHours} Hours Restorative Sleep",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${dailyStats.sleepQualityScore}% READINESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadialProgressGauge(
    progress: Float,
    valueText: String,
    labelText: String,
    targetText: String,
    ringColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 7.dp.toPx()
                // Track
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Progress
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ringColor,
            fontSize = 9.sp
        )
        Text(
            text = targetText,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 8.sp
        )
    }
}
