package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkoutCatalogEntity
import com.example.model.WorkoutSuggestionEntity
import com.example.model.WorkoutType
import com.example.ui.components.CyberCard
import com.example.ui.components.PersonalizedRecommendationsCard
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel

@Composable
fun AiCoachScreen(
    viewModel: FitnessViewModel,
    onStartWorkout: (WorkoutType) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions by viewModel.allSuggestions.collectAsState()
    val personalizedRecs by viewModel.personalizedRecommendations.collectAsState()
    val chatMessages by viewModel.aiChatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val isGeneratingSuggestions by viewModel.isGeneratingSuggestions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val handleStartCatalogWorkout: (WorkoutCatalogEntity) -> Unit = { catalogItem ->
        val mappedType = WorkoutType.fromString(catalogItem.workoutType)
        onStartWorkout(mappedType)
    }

    var userQueryText by remember { mutableStateOf("") }
    val quickQuestions = listOf(
        "How do I optimize my Zone 2 aerobic base?",
        "What should my target cadence be for a 5K?",
        "How do I accelerate heart rate recovery post-run?",
        "Tips for downhill trail running biomechanics"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // --- 2-Column Wide / Tablet Layout ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Readiness & Personalized Workout Suggestions
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = PulseTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI READINESS & PROGRAMMING",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.colors.textPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    item {
                        CyberCard(
                            borderColor = PulseTheme.colors.primary.copy(alpha = 0.5f),
                            glowColor = PulseTheme.colors.primary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DAILY RECOVERY & READINESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseTheme.colors.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Optimal Readiness (92%)",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Resting HR: ${userProfile.restingHeartRate} BPM • Aerobic capacity primed. Physiological strain index is balanced for moderate-to-high intensity stimulus.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PulseTheme.colors.textSecondary,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(PulseTheme.colors.surfaceElevated)
                                        .border(3.dp, Brush.sweepGradient(listOf(PulseTheme.colors.primary, NeonGreen, PulseTheme.colors.primary)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "92",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                        Text(
                                            text = "SCORE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseTheme.colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        PersonalizedRecommendationsCard(
                            recommendations = personalizedRecs,
                            userProfile = userProfile,
                            onStartWorkout = handleStartCatalogWorkout,
                            onEditFitnessSetup = { viewModel.openSetupWizard() }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI WORKOUT SUGGESTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            TextButton(
                                onClick = { viewModel.refreshAiWorkoutSuggestions() },
                                modifier = Modifier.testTag("refresh_ai_suggestions_button")
                            ) {
                                if (isGeneratingSuggestions) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = PulseTheme.colors.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = PulseTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Regenerate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (suggestions.isEmpty() && !isGeneratingSuggestions) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No suggestions yet. Tap Regenerate to create tailored AI workouts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PulseTheme.colors.textMuted
                                )
                            }
                        }
                    } else {
                        items(suggestions) { suggestion ->
                            WorkoutSuggestionCard(
                                suggestion = suggestion,
                                onStart = {
                                    val type = WorkoutType.fromString(suggestion.workoutType)
                                    onStartWorkout(type)
                                },
                                onToggleComplete = { completed ->
                                    viewModel.completeSuggestion(suggestion.id, completed)
                                }
                            )
                        }
                    }
                }

                // Right Column: Interactive AI Coach Chat
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "INTERACTIVE ATHLETIC COACH CHAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Prompt Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickQuestions) { q ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PulseTheme.colors.surfaceElevated)
                                    .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        userQueryText = q
                                        viewModel.sendCoachMessage(q)
                                        userQueryText = ""
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatMessages) { message ->
                            CoachMessageBubble(message = message)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CyberCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = userQueryText,
                                onValueChange = { userQueryText = it },
                                placeholder = {
                                    Text(
                                        text = "Ask your AI Coach anything...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PulseTheme.colors.textMuted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_coach_query_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PulseTheme.colors.primary,
                                    unfocusedBorderColor = PulseTheme.colors.border,
                                    focusedTextColor = PulseTheme.colors.textPrimary,
                                    unfocusedTextColor = PulseTheme.colors.textPrimary,
                                    focusedContainerColor = PulseTheme.colors.surfaceElevated,
                                    unfocusedContainerColor = PulseTheme.colors.surfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (userQueryText.isNotBlank()) {
                                        val textToSend = userQueryText
                                        userQueryText = ""
                                        viewModel.sendCoachMessage(textToSend)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PulseTheme.colors.primary)
                                    .testTag("ai_coach_send_button"),
                                enabled = !isChatLoading && userQueryText.isNotBlank()
                            ) {
                                if (isChatLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- Phone Vertical Flow ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = PulseTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI COACHING & SUGGESTIONS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Biometric readiness & tailored adaptive programming",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseTheme.colors.textMuted
                            )
                        }
                    }
                }

                // Daily Readiness & Biological Strain Card
                item {
                    CyberCard(
                        borderColor = PulseTheme.colors.primary.copy(alpha = 0.5f),
                        glowColor = PulseTheme.colors.primary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DAILY RECOVERY & READINESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Optimal Readiness (92%)",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Resting HR: ${userProfile.restingHeartRate} BPM • Aerobic capacity primed. Physiological strain index is balanced for moderate-to-high intensity stimulus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PulseTheme.colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(PulseTheme.colors.surfaceElevated)
                                    .border(3.dp, Brush.sweepGradient(listOf(PulseTheme.colors.primary, NeonGreen, PulseTheme.colors.primary)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "92",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                    Text(
                                        text = "SCORE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseTheme.colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    PersonalizedRecommendationsCard(
                        recommendations = personalizedRecs,
                        userProfile = userProfile,
                        onStartWorkout = handleStartCatalogWorkout,
                        onEditFitnessSetup = { viewModel.openSetupWizard() }
                    )
                }

                // AI Generated Adaptive Programming Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI ADAPTIVE PROGRAMMING",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = { viewModel.refreshAiWorkoutSuggestions() },
                            modifier = Modifier.testTag("refresh_ai_suggestions_button")
                        ) {
                            if (isGeneratingSuggestions) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = PulseTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = PulseTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Regenerate",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (suggestions.isEmpty() && !isGeneratingSuggestions) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No suggestions yet. Tap Regenerate to create tailored AI workouts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseTheme.colors.textMuted
                            )
                        }
                    }
                } else {
                    items(suggestions) { suggestion ->
                        WorkoutSuggestionCard(
                            suggestion = suggestion,
                            onStart = {
                                val type = WorkoutType.fromString(suggestion.workoutType)
                                onStartWorkout(type)
                            },
                            onToggleComplete = { completed ->
                                viewModel.completeSuggestion(suggestion.id, completed)
                            }
                        )
                    }
                }

                // Interactive AI Athletic Coach Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "INTERACTIVE AI ATHLETIC COACH",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Quick Prompt Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickQuestions) { q ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PulseTheme.colors.surfaceElevated)
                                    .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        userQueryText = q
                                        viewModel.sendCoachMessage(q)
                                        userQueryText = ""
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Chat Message History
                items(chatMessages) { message ->
                    CoachMessageBubble(message = message)
                }

                // Interactive Query Input Bar
                item {
                    CyberCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = userQueryText,
                                onValueChange = { userQueryText = it },
                                placeholder = {
                                    Text(
                                        text = "Ask your AI Coach anything...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PulseTheme.colors.textMuted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_coach_query_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PulseTheme.colors.primary,
                                    unfocusedBorderColor = PulseTheme.colors.border,
                                    focusedTextColor = PulseTheme.colors.textPrimary,
                                    unfocusedTextColor = PulseTheme.colors.textPrimary,
                                    focusedContainerColor = PulseTheme.colors.surfaceElevated,
                                    unfocusedContainerColor = PulseTheme.colors.surfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (userQueryText.isNotBlank()) {
                                        val textToSend = userQueryText
                                        userQueryText = ""
                                        viewModel.sendCoachMessage(textToSend)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PulseTheme.colors.primary)
                                    .testTag("ai_coach_send_button"),
                                enabled = !isChatLoading && userQueryText.isNotBlank()
                            ) {
                                if (isChatLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSuggestionCard(
    suggestion: WorkoutSuggestionEntity,
    onStart: () -> Unit,
    onToggleComplete: (Boolean) -> Unit
) {
    CyberCard(
        borderColor = if (suggestion.isCompleted) NeonGreen.copy(alpha = 0.5f) else PulseTheme.colors.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTheme.colors.textPrimary
                )
                Text(
                    text = "${suggestion.workoutType} • ${suggestion.targetDurationMin} min • ${suggestion.intensity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.textMuted
                )
            }

            Checkbox(
                checked = suggestion.isCompleted,
                onCheckedChange = { onToggleComplete(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonGreen,
                    uncheckedColor = PulseTheme.colors.border
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Target Zone & Intensity Tag
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PulseTheme.colors.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = suggestion.targetZone,
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PulseTheme.colors.surfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = suggestion.intensity,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonAmber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = suggestion.rationale,
            style = MaterialTheme.typography.bodySmall,
            color = PulseTheme.colors.textSecondary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Interval Details
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PulseTheme.colors.surfaceElevated)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = PulseTheme.colors.textMuted,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = suggestion.recommendedIntervals,
                style = MaterialTheme.typography.bodySmall,
                color = PulseTheme.colors.textPrimary,
                fontSize = 12.sp
            )
        }

        if (!suggestion.isCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("start_suggested_workout_${suggestion.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.primary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Start Suggested Session",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CoachMessageBubble(message: com.example.model.AiCoachMessage) {
    val isAi = message.isFromAi
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        if (isAi) {
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
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isAi) PulseTheme.colors.surfaceElevated else PulseTheme.colors.primary)
                .border(
                    1.dp,
                    if (isAi) PulseTheme.colors.border else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAi) PulseTheme.colors.textPrimary else Color.Black,
                lineHeight = 18.sp
            )
        }
    }
}
