package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.model.PersonalizedRecommendation
import com.example.model.UserProfileEntity
import com.example.model.WorkoutCatalogEntity
import com.example.ui.theme.*

@Composable
fun PersonalizedRecommendationsCard(
    recommendations: List<PersonalizedRecommendation>,
    userProfile: UserProfileEntity,
    onStartWorkout: (WorkoutCatalogEntity) -> Unit,
    onEditFitnessSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

    val topPick = recommendations.firstOrNull()
    var selectedRecommendation by remember(recommendations) { mutableStateOf(topPick) }

    CyberCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = PulseTheme.colors.primary.copy(alpha = 0.5f),
        glowColor = PulseTheme.colors.primary
    ) {
        // --- Header with User Context ---
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PulseTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PERSONALIZED WORKOUTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PulseTheme.colors.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Calibrated for ${userProfile.name} • ${userProfile.mainGoal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseTheme.colors.textMuted
                    )
                }
            }

            IconButton(
                onClick = onEditFitnessSetup,
                modifier = Modifier.testTag("edit_fitness_setup_shortcut")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Adjust Setup",
                    tint = PulseTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Recommendation Selector Carousel ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recommendations) { rec ->
                val isSelected = selectedRecommendation?.workout?.workoutId == rec.workout.workoutId
                val isTop = rec.isTodayTopPick

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PulseTheme.colors.primary.copy(alpha = 0.2f)
                            else PulseTheme.colors.surfaceElevated
                        )
                        .border(
                            1.dp,
                            if (isSelected) PulseTheme.colors.primary else if (isTop) NeonGreen.copy(alpha = 0.6f) else PulseTheme.colors.border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedRecommendation = rec }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTop) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = rec.workout.workoutName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PulseTheme.colors.primary else PulseTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (rec.matchScore >= 80) NeonGreen.copy(alpha = 0.2f) else PulseTheme.colors.border)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${rec.matchScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (rec.matchScore >= 80) NeonGreen else PulseTheme.colors.textMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Active Recommendation Details Card ---
        selectedRecommendation?.let { rec ->
            val workout = rec.workout

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PulseTheme.colors.surfaceElevated)
                    .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                // Title and Match Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workout.workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.textPrimary
                        )
                        Text(
                            text = rec.matchTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PulseTheme.colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${rec.matchScore}% Match",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTheme.colors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTag(icon = Icons.Default.Timer, text = "${workout.durationMin} Min")
                    MetricTag(icon = Icons.Default.LocalFireDepartment, text = "~${workout.caloriesEst} kcal")
                    MetricTag(icon = Icons.Default.FitnessCenter, text = workout.equipment)
                    MetricTag(icon = Icons.Default.SignalCellularAlt, text = workout.difficulty)
                }

                if (workout.targetZones.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PulseTheme.colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target Zone: ${workout.targetZones}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start button
                Button(
                    onClick = { onStartWorkout(workout) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("start_recommended_workout_${workout.workoutId}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseTheme.colors.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start ${workout.workoutName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PulseTheme.colors.background)
            .border(1.dp, PulseTheme.colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PulseTheme.colors.textMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = PulseTheme.colors.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
