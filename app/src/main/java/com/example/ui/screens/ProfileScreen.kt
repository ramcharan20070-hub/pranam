package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.model.AppThemeMode
import com.example.model.UserProfileEntity
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel

@Composable
fun ProfileScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.userProfile.collectAsState()
    val totalDistMeters by viewModel.totalDistanceMeters.collectAsState()
    val totalCalories by viewModel.totalCaloriesBurned.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()

    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var ageText by remember(currentProfile) { mutableStateOf(currentProfile.age.toString()) }
    var gender by remember(currentProfile) { mutableStateOf(currentProfile.gender) }
    var weightText by remember(currentProfile) { mutableStateOf(currentProfile.weightKg.toString()) }
    var heightText by remember(currentProfile) { mutableStateOf(currentProfile.heightCm.toString()) }
    var fitnessLevel by remember(currentProfile) { mutableStateOf(currentProfile.fitnessLevel) }
    var mainGoal by remember(currentProfile) { mutableStateOf(currentProfile.mainGoal) }
    var activityLevel by remember(currentProfile) { mutableStateOf(currentProfile.activityLevel) }
    var workoutDaysText by remember(currentProfile) { mutableStateOf(currentProfile.workoutDays.toString()) }
    var workoutDurationText by remember(currentProfile) { mutableStateOf(currentProfile.workoutDuration.toString()) }
    var equipment by remember(currentProfile) { mutableStateOf(currentProfile.equipment) }

    var restingHrText by remember(currentProfile) { mutableStateOf(currentProfile.restingHeartRate.toString()) }
    var maxHrText by remember(currentProfile) { mutableStateOf(currentProfile.maxHeartRate.toString()) }
    var targetKmText by remember(currentProfile) { mutableStateOf(currentProfile.targetWeeklyKm.toString()) }

    var saveSuccessMessage by remember { mutableStateOf(false) }
    var showSmartwatchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val connectedWatch by viewModel.connectedWatch.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val isSyncingWithGoogleFit by viewModel.isSyncingWithGoogleFit.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    val scheduledReminders by viewModel.scheduledReminders.collectAsState()
    val dailyNotificationPrefs by viewModel.dailyNotificationPrefs.collectAsState()

    val goals = listOf(
        "Weight Loss",
        "Gain Muscle",
        "Improve Fitness",
        "Increase Strength",
        "Improve Endurance",
        "Stay Active"
    )

    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val genders = listOf("Male", "Female", "Other", "Prefer not to say")
    val activities = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active")
    val equipmentList = listOf("No Equipment", "Dumbbells", "Resistance Bands", "Gym Equipment", "Full Gym")

    // Real-time BMI calculation
    val currentBmi = remember(heightText, weightText) {
        val h = heightText.toDoubleOrNull() ?: 175.0
        val w = weightText.toDoubleOrNull() ?: 70.0
        val hMeter = (h / 100.0).coerceAtLeast(0.5)
        w / (hMeter * hMeter)
    }

    val bmiCategory = remember(currentBmi) {
        when {
            currentBmi < 18.5 -> "Underweight"
            currentBmi < 25.0 -> "Normal Weight"
            currentBmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    // Estimated VO2 Max = 15.3 * (HRmax / HRrest)
    val estimatedVo2Max = remember(maxHrText, restingHrText) {
        val maxHr = maxHrText.toIntOrNull() ?: 190
        val restHr = restingHrText.toIntOrNull() ?: 55
        if (restHr > 30) {
            String.format("%.1f", 15.3 * (maxHr.toDouble() / restHr.toDouble()))
        } else "50.0"
    }

    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PulseTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ATHLETE PROFILE & SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Themes, biometrics & customized training zones",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted
                )
            }
        }

        // --- Dual Themes Selector Card ---
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PulseTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = PulseTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "APP THEME & APPEARANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Switch between Cyber Black and High-Tech White",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = appThemeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("theme_select_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    AppThemeMode.CYBER_BLACK -> Icons.Default.DarkMode
                                    AppThemeMode.HIGH_TECH_WHITE -> Icons.Default.LightMode
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.CYBER_BLACK -> "Black"
                                    AppThemeMode.HIGH_TECH_WHITE -> "White"
                                    AppThemeMode.SYSTEM -> "System"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // --- VO2 Max & Cardiovascular Capacity Card ---
        CyberCard(
            borderColor = NeonGreen.copy(alpha = 0.4f),
            glowColor = NeonGreen
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESTIMATED VO2 MAX",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$estimatedVo2Max mL/kg/min",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary
                    )
                    Text(
                        text = "Classification: Superior • High Aerobic Efficiency",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTheme.colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PulseTheme.colors.surfaceElevated)
                        .border(2.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // --- Weekly Targets Progress ---
        CyberCard {
            Text(
                text = "WEEKLY TRAINING VOLUME PROGRESS",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val currentDistKm = totalDistMeters / 1000.0
            val targetDistKm = targetKmText.toDoubleOrNull() ?: 35.0
            val progressFraction = (currentDistKm / targetDistKm).coerceIn(0.0, 1.0).toFloat()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f / %.1f KM", currentDistKm, targetDistKm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PulseTheme.colors.primary,
                trackColor = PulseTheme.colors.border
            )
        }

        // --- Fitness Setup Wizard Quick Launcher ---
        CyberCard(
            borderColor = PulseTheme.colors.primary.copy(alpha = 0.5f),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PulseTheme.colors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = PulseTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FITNESS SETUP WIZARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Walk through the 4-step personalization wizard",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openSetupWizard() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.primary, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("launch_setup_wizard_button")
                ) {
                    Text("Launch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Physical Metrics Form ---
        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHYSIOLOGICAL ATTRIBUTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "ID #${currentProfile.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Athlete Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_athlete_name"),
                colors = dynamicOutlinedColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = {
                        ageText = it
                        val a = it.toIntOrNull()
                        if (a != null && a in 10..99) {
                            maxHrText = "${220 - a}"
                        }
                    },
                    label = { Text("Age (Years)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_athlete_age"),
                    colors = dynamicOutlinedColors()
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Gender") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_athlete_gender"),
                    colors = dynamicOutlinedColors()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_athlete_height"),
                    colors = dynamicOutlinedColors()
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_athlete_weight"),
                    colors = dynamicOutlinedColors()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BMI Real-time preview card inside physical metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PulseTheme.colors.background)
                    .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("BMI: %.1f", currentBmi),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "Category: $bmiCategory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentBmi in 18.5..24.9) NeonGreen else PulseTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = restingHrText,
                    onValueChange = { restingHrText = it },
                    label = { Text("Resting HR (BPM)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_resting_hr"),
                    colors = dynamicOutlinedColors()
                )

                OutlinedTextField(
                    value = maxHrText,
                    onValueChange = { maxHrText = it },
                    label = { Text("Max HR (BPM)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_max_hr"),
                    colors = dynamicOutlinedColors()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = workoutDaysText,
                    onValueChange = { workoutDaysText = it },
                    label = { Text("Workout Days / Wk") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_workout_days"),
                    colors = dynamicOutlinedColors()
                )

                OutlinedTextField(
                    value = workoutDurationText,
                    onValueChange = { workoutDurationText = it },
                    label = { Text("Duration (min)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_workout_duration"),
                    colors = dynamicOutlinedColors()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = targetKmText,
                onValueChange = { targetKmText = it },
                label = { Text("Target Weekly Distance (km)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_target_weekly_km"),
                colors = dynamicOutlinedColors()
            )
        }

        // --- Fitness Goal Selector ---
        CyberCard {
            Text(
                text = "PRIMARY FITNESS GOAL",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(goals) { goal ->
                    val isSel = mainGoal.equals(goal, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) PulseTheme.colors.primary.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (isSel) PulseTheme.colors.primary else PulseTheme.colors.border, RoundedCornerShape(12.dp))
                            .clickable { mainGoal = goal }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- Experience Level Selector ---
        CyberCard {
            Text(
                text = "EXPERIENCE LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { lvl ->
                    val isSel = fitnessLevel.equals(lvl, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) NeonGreen.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (isSel) NeonGreen else PulseTheme.colors.border, RoundedCornerShape(10.dp))
                            .clickable { fitnessLevel = lvl }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lvl,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) NeonGreen else PulseTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- Equipment Selection Card ---
        CyberCard {
            Text(
                text = "AVAILABLE WORKOUT EQUIPMENT",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(equipmentList) { eq ->
                    val isSel = equipment.equals(eq, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) PulseTheme.colors.primary.copy(alpha = 0.18f) else PulseTheme.colors.surfaceElevated)
                            .border(1.dp, if (isSel) PulseTheme.colors.primary else PulseTheme.colors.border, RoundedCornerShape(10.dp))
                            .clickable { equipment = eq }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = eq,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- Connected Wearables & Smartwatches ---
        CyberCard(
            borderColor = if (connectedWatch != null) NeonGreen.copy(alpha = 0.5f) else PulseTheme.colors.border
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
                            .background(if (connectedWatch != null) NeonGreen.copy(alpha = 0.15f) else PulseTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = if (connectedWatch != null) NeonGreen else PulseTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (connectedWatch != null) connectedWatch!!.name else "SMARTWATCH INTEGRATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (connectedWatch != null) "Connected • ${connectedWatch!!.brand.brandName}" else "Supports Garmin, WearOS, Apple Watch, Polar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectedWatch != null) NeonGreen else PulseTheme.colors.textMuted
                        )
                    }
                }

                Button(
                    onClick = { showSmartwatchDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("profile_pair_watch_button")
                ) {
                    Text(
                        text = if (connectedWatch != null) "Manage" else "Pair Watch",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- Google Fit & Health Connect Cloud Integration ---
        CyberCard(
            borderColor = if (dailyStats.isGoogleFitSynced) NeonGreen.copy(alpha = 0.5f) else PulseTheme.colors.border
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
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GOOGLE FIT CLOUD SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (dailyStats.isGoogleFitSynced) "Daily Steps & Calories Synced" else "Sync Steps, BMR & Heart Points",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyStats.isGoogleFitSynced) NeonGreen else PulseTheme.colors.textMuted
                        )
                    }
                }

                Button(
                    onClick = { viewModel.syncWithGoogleFit() },
                    enabled = !isSyncingWithGoogleFit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.surfaceElevated, contentColor = PulseTheme.colors.primary),
                    border = BorderStroke(1.dp, PulseTheme.colors.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("profile_sync_fit_button")
                ) {
                    if (isSyncingWithGoogleFit) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTheme.colors.primary, strokeWidth = 2.dp)
                    } else {
                        Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (lastSyncResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ ${lastSyncResult!!.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen,
                    fontSize = 11.sp
                )
            }
        }

        // --- Push Notifications & Timely Reminders Hub ---
        CyberCard(
            borderColor = if (dailyNotificationPrefs.notificationsMasterEnabled) PulseTheme.colors.primary.copy(alpha = 0.5f) else PulseTheme.colors.border
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
                            .background(PulseTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PulseTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NOTIFICATIONS & REMINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        val activeCount = scheduledReminders.count { it.isEnabled }
                        Text(
                            text = "$activeCount Active Alarms • Daily Briefings & Trend Alerts",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.primary
                        )
                    }
                }

                Button(
                    onClick = { showNotificationsDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("profile_configure_notifications_button")
                ) {
                    Text(
                        text = "Configure",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (saveSuccessMessage) {
            Text(
                text = "Profile parameters updated successfully.",
                style = MaterialTheme.typography.labelMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Save Profile Button ---
        CyberButton(
            text = "SAVE ATHLETE PROFILE",
            onClick = {
                val parsedAge = ageText.toIntOrNull() ?: currentProfile.age
                val parsedDays = workoutDaysText.toIntOrNull() ?: currentProfile.workoutDays
                val parsedDuration = workoutDurationText.toIntOrNull() ?: currentProfile.workoutDuration
                val updated = currentProfile.copy(
                    name = name,
                    age = parsedAge,
                    gender = gender,
                    weightKg = weightText.toDoubleOrNull() ?: currentProfile.weightKg,
                    heightCm = heightText.toDoubleOrNull() ?: currentProfile.heightCm,
                    fitnessLevel = fitnessLevel,
                    mainGoal = mainGoal,
                    activityLevel = activityLevel,
                    workoutDays = parsedDays,
                    workoutDuration = parsedDuration,
                    equipment = equipment,
                    restingHeartRate = restingHrText.toIntOrNull() ?: currentProfile.restingHeartRate,
                    maxHeartRate = maxHrText.toIntOrNull() ?: currentProfile.maxHeartRate,
                    fitnessGoal = mainGoal,
                    experienceLevel = fitnessLevel,
                    targetWeeklyKm = targetKmText.toDoubleOrNull() ?: currentProfile.targetWeeklyKm
                )
                viewModel.saveUserProfile(updated)
                saveSuccessMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            color = PulseTheme.colors.primary,
            icon = Icons.Default.Save,
            testTag = "save_profile_button"
        )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSmartwatchDialog) {
        com.example.ui.components.SmartwatchConnectDialog(
            viewModel = viewModel,
            onDismiss = { showSmartwatchDialog = false }
        )
    }

    if (showNotificationsDialog) {
        com.example.ui.components.NotificationScheduleDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }
}

@Composable
private fun dynamicOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PulseTheme.colors.primary,
    unfocusedBorderColor = PulseTheme.colors.border,
    focusedLabelColor = PulseTheme.colors.primary,
    unfocusedLabelColor = PulseTheme.colors.textMuted,
    focusedContainerColor = PulseTheme.colors.surfaceElevated,
    unfocusedContainerColor = PulseTheme.colors.surfaceElevated,
    focusedTextColor = PulseTheme.colors.textPrimary,
    unfocusedTextColor = PulseTheme.colors.textPrimary
)
