package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfileEntity
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel

@Composable
fun ProfileFitnessSetupScreen(
    viewModel: FitnessViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialProfile by viewModel.userProfile.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }

    // Form fields
    var name by remember(initialProfile) { mutableStateOf(initialProfile.name.ifBlank { "Rahul" }) }
    var age by remember(initialProfile) { mutableIntStateOf(if (initialProfile.age > 0) initialProfile.age else 25) }
    var gender by remember(initialProfile) { mutableStateOf(initialProfile.gender.ifBlank { "Male" }) }
    var heightCm by remember(initialProfile) { mutableDoubleStateOf(if (initialProfile.heightCm > 50.0) initialProfile.heightCm else 175.0) }
    var weightKg by remember(initialProfile) { mutableDoubleStateOf(if (initialProfile.weightKg > 20.0) initialProfile.weightKg else 70.0) }
    
    var fitnessLevel by remember(initialProfile) { mutableStateOf(initialProfile.fitnessLevel.ifBlank { "Beginner" }) }
    var mainGoal by remember(initialProfile) { mutableStateOf(initialProfile.mainGoal.ifBlank { "Weight Loss" }) }
    var activityLevel by remember(initialProfile) { mutableStateOf(initialProfile.activityLevel.ifBlank { "Moderately Active" }) }
    
    var workoutDays by remember(initialProfile) { mutableIntStateOf(if (initialProfile.workoutDays in 1..7) initialProfile.workoutDays else 5) }
    var workoutDuration by remember(initialProfile) { mutableIntStateOf(if (initialProfile.workoutDuration > 0) initialProfile.workoutDuration else 30) }
    var equipment by remember(initialProfile) { mutableStateOf(initialProfile.equipment.ifBlank { "No Equipment" }) }

    // Physiological Calculations
    val bmi = remember(heightCm, weightKg) {
        val hMeter = (heightCm / 100.0).coerceAtLeast(0.5)
        weightKg / (hMeter * hMeter)
    }

    val bmiCategory = remember(bmi) {
        when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal Weight"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    val estimatedMaxHr = remember(age) { (220 - age).coerceIn(140, 210) }
    val estimatedRestingHr = remember(fitnessLevel) {
        when (fitnessLevel) {
            "Beginner" -> 68
            "Intermediate" -> 58
            "Advanced" -> 50
            else -> 62
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Top Step Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_pranam_logo),
                        contentDescription = "Pranam Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRANAM SETUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PulseTheme.colors.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Personalized Biometric Profiling",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val finalized = UserProfileEntity(
                            name = name,
                            age = age,
                            gender = gender,
                            heightCm = heightCm,
                            weightKg = weightKg,
                            fitnessLevel = fitnessLevel,
                            mainGoal = mainGoal,
                            activityLevel = activityLevel,
                            workoutDays = workoutDays,
                            workoutDuration = workoutDuration,
                            equipment = equipment,
                            isSetupCompleted = true,
                            maxHeartRate = estimatedMaxHr,
                            restingHeartRate = estimatedRestingHr,
                            fitnessGoal = mainGoal,
                            experienceLevel = fitnessLevel,
                            targetWeeklyKm = (workoutDays * 4.5),
                            targetWeeklyCalories = (workoutDays * workoutDuration * 8)
                        )
                        viewModel.saveFitnessSetup(finalized)
                        onFinish()
                    },
                    modifier = Modifier.testTag("skip_setup_button")
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Progress Indicator ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (step in 1..4) {
                    val isPassed = step <= currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isCurrent -> PulseTheme.colors.primary
                                    isPassed -> NeonGreen
                                    else -> PulseTheme.colors.border
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STEP $currentStep OF 4",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = when (currentStep) {
                        1 -> "Basics"
                        2 -> "Biometrics & BMI"
                        3 -> "Goals & Level"
                        else -> "Schedule & Gear"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Multi-Step Content View ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> Step1Basics(
                        name = name,
                        onNameChange = { name = it },
                        age = age,
                        onAgeChange = { age = it },
                        gender = gender,
                        onGenderChange = { gender = it }
                    )
                    2 -> Step2Biometrics(
                        heightCm = heightCm,
                        onHeightChange = { heightCm = it },
                        weightKg = weightKg,
                        onWeightChange = { weightKg = it },
                        bmi = bmi,
                        bmiCategory = bmiCategory,
                        maxHr = estimatedMaxHr,
                        restingHr = estimatedRestingHr
                    )
                    3 -> Step3GoalsAndLevel(
                        fitnessLevel = fitnessLevel,
                        onLevelChange = { fitnessLevel = it },
                        mainGoal = mainGoal,
                        onGoalChange = { mainGoal = it },
                        activityLevel = activityLevel,
                        onActivityChange = { activityLevel = it }
                    )
                    4 -> Step4ScheduleAndEquipment(
                        workoutDays = workoutDays,
                        onDaysChange = { workoutDays = it },
                        duration = workoutDuration,
                        onDurationChange = { workoutDuration = it },
                        equipment = equipment,
                        onEquipmentChange = { equipment = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Health Guidance Disclaimer Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PulseTheme.colors.surfaceElevated.copy(alpha = 0.6f))
                    .border(1.dp, PulseTheme.colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = PulseTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personalization optimizes exercise pacing and volume, not medical diagnosis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Bottom Navigation Controls ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("setup_back_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PulseTheme.colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseTheme.colors.textPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Back", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 4) {
                            currentStep += 1
                        } else {
                            val finalized = UserProfileEntity(
                                name = name.ifBlank { "Rahul" },
                                age = age,
                                gender = gender,
                                heightCm = heightCm,
                                weightKg = weightKg,
                                fitnessLevel = fitnessLevel,
                                mainGoal = mainGoal,
                                activityLevel = activityLevel,
                                workoutDays = workoutDays,
                                workoutDuration = workoutDuration,
                                equipment = equipment,
                                isSetupCompleted = true,
                                maxHeartRate = estimatedMaxHr,
                                restingHeartRate = estimatedRestingHr,
                                fitnessGoal = mainGoal,
                                experienceLevel = fitnessLevel,
                                targetWeeklyKm = (workoutDays * 4.5),
                                targetWeeklyCalories = (workoutDays * workoutDuration * 8)
                            )
                            viewModel.saveFitnessSetup(finalized)
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .weight(if (currentStep > 1) 1.5f else 1f)
                        .height(52.dp)
                        .testTag(if (currentStep == 4) "complete_setup_button" else "setup_next_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 4) NeonGreen else PulseTheme.colors.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (currentStep == 4) "Personalize & Start" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentStep == 4) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1Basics(
    name: String,
    onNameChange: (String) -> Unit,
    age: Int,
    onAgeChange: (Int) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit
) {
    val genders = listOf("Male", "Female", "Other", "Prefer not to say")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Welcome! Let's get to know you.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PulseTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "We use your details to configure pacing zones and workout difficulty.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTheme.colors.textSecondary
            )
        }

        CyberCard {
            Text(
                text = "WHAT SHOULD WE CALL YOU?",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Your Name or Nickname") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_name_input"),
                colors = setupOutlinedColors()
            )
        }

        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUR AGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Used for 220-age heart rate zone calibration",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }

                Text(
                    text = "$age yrs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PulseTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = age.toFloat(),
                onValueChange = { onAgeChange(it.toInt()) },
                valueRange = 14f..85f,
                steps = 70,
                colors = SliderDefaults.colors(
                    thumbColor = PulseTheme.colors.primary,
                    activeTrackColor = PulseTheme.colors.primary,
                    inactiveTrackColor = PulseTheme.colors.border
                ),
                modifier = Modifier.testTag("setup_age_slider")
            )
        }

        CyberCard {
            Text(
                text = "GENDER (OPTIONAL)",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(110.dp)
            ) {
                items(genders) { g ->
                    val isSelected = gender == g
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.2f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onGenderChange(g) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2Biometrics(
    heightCm: Double,
    onHeightChange: (Double) -> Unit,
    weightKg: Double,
    onWeightChange: (Double) -> Unit,
    bmi: Double,
    bmiCategory: String,
    maxHr: Int,
    restingHr: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Body Biometrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PulseTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Accurate metrics calibrate real-time calorie burn and energy expenditure.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTheme.colors.textSecondary
            )
        }

        // Height Card
        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${heightCm.toInt()} cm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PulseTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = heightCm.toFloat(),
                onValueChange = { onHeightChange(it.toDouble()) },
                valueRange = 130f..220f,
                steps = 90,
                colors = SliderDefaults.colors(
                    thumbColor = PulseTheme.colors.primary,
                    activeTrackColor = PulseTheme.colors.primary,
                    inactiveTrackColor = PulseTheme.colors.border
                ),
                modifier = Modifier.testTag("setup_height_slider")
            )
        }

        // Weight Card
        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format("%.1f kg", weightKg),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PulseTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = weightKg.toFloat(),
                onValueChange = { onWeightChange(it.toDouble()) },
                valueRange = 40f..160f,
                steps = 120,
                colors = SliderDefaults.colors(
                    thumbColor = PulseTheme.colors.primary,
                    activeTrackColor = PulseTheme.colors.primary,
                    inactiveTrackColor = PulseTheme.colors.border
                ),
                modifier = Modifier.testTag("setup_weight_slider")
            )
        }

        // Live Calculated BMI & Heart Rate Preview
        CyberCard(
            borderColor = NeonGreen.copy(alpha = 0.4f),
            glowColor = NeonGreen
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CALCULATED BMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = String.format("%.1f", bmi),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = PulseTheme.colors.textPrimary
                    )
                    Text(
                        text = "Classification: $bmiCategory",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (bmi in 18.5..24.9) NeonGreen else PulseTheme.colors.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MAX HR: $maxHr BPM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REST HR: $restingHr BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun Step3GoalsAndLevel(
    fitnessLevel: String,
    onLevelChange: (String) -> Unit,
    mainGoal: String,
    onGoalChange: (String) -> Unit,
    activityLevel: String,
    onActivityChange: (String) -> Unit
) {
    val levels = listOf(
        Pair("Beginner", "Starting fresh or returning to routine"),
        Pair("Intermediate", "Exercising regularly 2-3x per week"),
        Pair("Advanced", "High stamina, endurance, or strength focus")
    )

    val goals = listOf(
        Triple("Weight Loss", Icons.AutoMirrored.Filled.DirectionsRun, "Maximize calorie deficit & fat oxidation"),
        Triple("Gain Muscle", Icons.Default.FitnessCenter, "Hypertrophy & compound resistance"),
        Triple("Improve Fitness", Icons.Default.Favorite, "Cardio efficiency & all-round vitality"),
        Triple("Increase Strength", Icons.Default.Bolt, "Power output & muscular stamina"),
        Triple("Improve Endurance", Icons.Default.Timer, "Lactate threshold & aerobic base"),
        Triple("Stay Active", Icons.AutoMirrored.Filled.DirectionsWalk, "Consistent daily low-impact movement")
    )

    val activities = listOf(
        "Sedentary", "Lightly Active", "Moderately Active", "Very Active"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Goals & Experience",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PulseTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select what matters to you most right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTheme.colors.textSecondary
            )
        }

        // Fitness Level Selector
        CyberCard {
            Text(
                text = "CURRENT FITNESS LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { (lvl, desc) ->
                    val isSelected = fitnessLevel.equals(lvl, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onLevelChange(lvl) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lvl,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textPrimary
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTheme.colors.textMuted
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PulseTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Primary Goal Selector
        CyberCard {
            Text(
                text = "PRIMARY FITNESS GOAL",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { (gName, icon, gDesc) ->
                    val isSelected = mainGoal.equals(gName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) NeonGreen.copy(alpha = 0.18f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) NeonGreen else PulseTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onGoalChange(gName) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else PulseTheme.colors.border.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) NeonGreen else PulseTheme.colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonGreen else PulseTheme.colors.textPrimary
                            )
                            Text(
                                text = gDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTheme.colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Daily Activity Baseline
        CyberCard {
            Text(
                text = "NORMAL DAILY ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activities.forEach { act ->
                    val isSelected = activityLevel.equals(act, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.2f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onActivityChange(act) }
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = act.replace(" Active", ""),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4ScheduleAndEquipment(
    workoutDays: Int,
    onDaysChange: (Int) -> Unit,
    duration: Int,
    onDurationChange: (Int) -> Unit,
    equipment: String,
    onEquipmentChange: (String) -> Unit
) {
    val durationOptions = listOf(10, 20, 30, 45, 60)
    val equipmentOptions = listOf(
        Pair("No Equipment", "Bodyweight exercises only"),
        Pair("Dumbbells", "Free weights & adjustable dumbbells"),
        Pair("Resistance Bands", "Elastic resistance loop & tube bands"),
        Pair("Gym Equipment", "Standard cardio & machine gear"),
        Pair("Full Gym", "Barbells, cables, racks, rowers & turf")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Schedule & Gear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PulseTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "We will match workout recommendations strictly to your available gear and time.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTheme.colors.textSecondary
            )
        }

        // Workout Days per Week
        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKOUT DAYS / WEEK",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$workoutDays Days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PulseTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (d in 1..7) {
                    val isSelected = workoutDays == d
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.25f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onDaysChange(d) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$d",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Target Duration
        CyberCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SESSION DURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$duration Min",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PulseTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationOptions.forEach { opt ->
                    val isSelected = duration == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) NeonGreen.copy(alpha = 0.25f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonGreen else PulseTheme.colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onDurationChange(opt) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$opt m",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonGreen else PulseTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Available Equipment
        CyberCard {
            Text(
                text = "AVAILABLE EQUIPMENT",
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                equipmentOptions.forEach { (eqName, eqDesc) ->
                    val isSelected = equipment.equals(eqName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.18f)
                                else PulseTheme.colors.surfaceElevated
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onEquipmentChange(eqName) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = eqName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textPrimary
                            )
                            Text(
                                text = eqDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTheme.colors.textMuted
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PulseTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun setupOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PulseTheme.colors.primary,
    unfocusedBorderColor = PulseTheme.colors.border,
    focusedLabelColor = PulseTheme.colors.primary,
    unfocusedLabelColor = PulseTheme.colors.textMuted,
    focusedContainerColor = PulseTheme.colors.surfaceElevated,
    unfocusedContainerColor = PulseTheme.colors.surfaceElevated,
    focusedTextColor = PulseTheme.colors.textPrimary,
    unfocusedTextColor = PulseTheme.colors.textPrimary
)
