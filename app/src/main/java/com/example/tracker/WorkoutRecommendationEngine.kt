package com.example.tracker

import com.example.model.PersonalizedRecommendation
import com.example.model.UserProfileEntity
import com.example.model.WorkoutCatalogEntity
import com.example.model.WorkoutSessionEntity
import kotlin.math.abs
import kotlin.math.max

object WorkoutRecommendationEngine {

    fun generatePersonalizedRecommendations(
        profile: UserProfileEntity,
        catalog: List<WorkoutCatalogEntity>,
        recentSessions: List<WorkoutSessionEntity> = emptyList()
    ): List<PersonalizedRecommendation> {
        if (catalog.isEmpty()) return emptyList()

        val heightM = (profile.heightCm / 100.0).coerceAtLeast(1.0)
        val bmi = profile.weightKg / (heightM * heightM)
        val isHighBmi = bmi >= 28.0

        val lastSession = recentSessions.maxByOrNull { it.startTime }
        val hoursSinceLastSession = if (lastSession != null) {
            (System.currentTimeMillis() - lastSession.endTime) / (1000 * 60 * 60)
        } else 48L

        val lastWasHard = lastSession?.let { it.avgHeartRate > 155 || it.durationSeconds > 2400 } ?: false
        val needsActiveRecovery = lastWasHard && hoursSinceLastSession < 20

        val scored = catalog.map { workout ->
            var score = 50 // baseline

            // 1. Goal Alignment (up to +25)
            val goalMatch = isGoalMatching(profile.mainGoal, profile.fitnessGoal, workout.goal, workout.workoutName)
            score += goalMatch

            // 2. Equipment Compatibility (up to +20, or -30 if equipment missing)
            val equipMatch = evaluateEquipment(profile.equipment, workout.equipment)
            score += equipMatch

            // 3. Fitness Level Alignment (up to +15, or -20 if too advanced for novice)
            val levelScore = evaluateLevel(profile.fitnessLevel.ifBlank { profile.experienceLevel }, workout.difficulty)
            score += levelScore

            // 4. Workout Duration Match (up to +15)
            val targetDuration = if (profile.workoutDuration > 0) profile.workoutDuration else 30
            val durationDiff = abs(workout.durationMin - targetDuration)
            val durationScore = when {
                durationDiff <= 5 -> 15
                durationDiff <= 10 -> 10
                durationDiff <= 20 -> 5
                else -> 0
            }
            score += durationScore

            // 5. Safety & Low-Impact Consideration for High BMI Beginner
            val isBeginner = profile.fitnessLevel.equals("Beginner", ignoreCase = true) || profile.experienceLevel.equals("Beginner", ignoreCase = true)
            if (isHighBmi && isBeginner) {
                if (workout.workoutType == "POWER_WALK" || workout.difficulty.equals("Beginner", ignoreCase = true)) {
                    score += 10
                } else if (workout.workoutType == "HIIT_SPRINT" || workout.workoutType == "TRAIL_RUN") {
                    score -= 15
                }
            }

            // 6. Recovery balance after recent hard training
            if (needsActiveRecovery) {
                if (workout.difficulty.equals("Beginner", ignoreCase = true) || workout.goal.contains("Active", ignoreCase = true) || workout.workoutType == "POWER_WALK") {
                    score += 15
                } else if (workout.difficulty.equals("Advanced", ignoreCase = true)) {
                    score -= 20
                }
            }

            val finalScore = score.coerceIn(15, 99)

            val tag = generateMatchTag(profile, workout, isHighBmi, needsActiveRecovery)
            val rationale = generateRationale(profile, workout, finalScore, isHighBmi, needsActiveRecovery)

            PersonalizedRecommendation(
                workout = workout,
                matchScore = finalScore,
                matchTag = tag,
                rationale = rationale,
                isTodayTopPick = false
            )
        }

        val sorted = scored.sortedByDescending { it.matchScore }
        return if (sorted.isNotEmpty()) {
            sorted.mapIndexed { index, rec ->
                if (index == 0) rec.copy(isTodayTopPick = true) else rec
            }
        } else {
            emptyList()
        }
    }

    private fun isGoalMatching(userGoal: String, legacyGoal: String, workoutGoal: String, workoutName: String): Int {
        val combinedUserGoal = "$userGoal $legacyGoal".lowercase()
        val combinedWorkout = "$workoutGoal $workoutName".lowercase()

        return when {
            combinedUserGoal.contains("weight") || combinedUserGoal.contains("fat") -> {
                if (combinedWorkout.contains("weight") || combinedWorkout.contains("fat") || combinedWorkout.contains("hiit") || combinedWorkout.contains("aerobic")) 25
                else if (combinedWorkout.contains("fitness") || combinedWorkout.contains("endurance")) 15
                else 5
            }
            combinedUserGoal.contains("muscle") || combinedUserGoal.contains("gain") -> {
                if (combinedWorkout.contains("muscle") || combinedWorkout.contains("hypertrophy") || combinedWorkout.contains("strength")) 25
                else if (combinedWorkout.contains("conditioning") || combinedWorkout.contains("full body")) 15
                else 5
            }
            combinedUserGoal.contains("strength") -> {
                if (combinedWorkout.contains("strength") || combinedWorkout.contains("muscle") || combinedWorkout.contains("power")) 25
                else if (combinedWorkout.contains("fitness")) 15
                else 5
            }
            combinedUserGoal.contains("endurance") || combinedUserGoal.contains("stamina") || combinedUserGoal.contains("vo2") -> {
                if (combinedWorkout.contains("endurance") || combinedWorkout.contains("stamina") || combinedWorkout.contains("run") || combinedWorkout.contains("cycling")) 25
                else if (combinedWorkout.contains("aerobic") || combinedWorkout.contains("hiit")) 15
                else 5
            }
            combinedUserGoal.contains("active") || combinedUserGoal.contains("health") -> {
                if (combinedWorkout.contains("active") || combinedWorkout.contains("mobility") || combinedWorkout.contains("recovery") || combinedWorkout.contains("aerobic")) 25
                else 15
            }
            else -> {
                // Improve Fitness general
                if (combinedWorkout.contains("fitness") || combinedWorkout.contains("full body") || combinedWorkout.contains("aerobic")) 25
                else 15
            }
        }
    }

    private fun evaluateEquipment(userEquipment: String, workoutEquipment: String): Int {
        val userEq = userEquipment.lowercase()
        val workEq = workoutEquipment.lowercase()

        return when {
            userEq.contains("full gym") -> 20 // User has access to everything
            userEq.contains("gym") && !userEq.contains("no") -> {
                if (workEq.contains("full gym")) 15 else 20
            }
            userEq.contains("dumbbell") -> {
                if (workEq.contains("none") || workEq.contains("no equip") || workEq.contains("dumbbell")) 20
                else if (workEq.contains("resistance")) 10
                else -25 // Requires gym
            }
            userEq.contains("band") -> {
                if (workEq.contains("none") || workEq.contains("no equip") || workEq.contains("resistance")) 20
                else if (workEq.contains("dumbbell")) 5
                else -25
            }
            else -> {
                // No Equipment / Bodyweight
                if (workEq.contains("none") || workEq.contains("no equip")) 20
                else -30 // Equipment not available
            }
        }
    }

    private fun evaluateLevel(userLevel: String, workoutLevel: String): Int {
        val uLvl = userLevel.lowercase()
        val wLvl = workoutLevel.lowercase()

        return when {
            uLvl.contains("beginner") -> {
                when {
                    wLvl.contains("beginner") -> 15
                    wLvl.contains("intermediate") -> 5
                    else -> -20 // Advanced is too intense for day 1
                }
            }
            uLvl.contains("intermediate") -> {
                when {
                    wLvl.contains("intermediate") -> 15
                    wLvl.contains("beginner") -> 10
                    else -> 8
                }
            }
            else -> {
                // Advanced / Elite
                when {
                    wLvl.contains("advanced") -> 15
                    wLvl.contains("intermediate") -> 12
                    else -> 5
                }
            }
        }
    }

    private fun generateMatchTag(
        profile: UserProfileEntity,
        workout: WorkoutCatalogEntity,
        isHighBmi: Boolean,
        needsActiveRecovery: Boolean
    ): String {
        return when {
            needsActiveRecovery && (workout.difficulty.equals("Beginner", true) || workout.goal.contains("Active", true)) ->
                "⚡ Active Recovery Pick (Post-Workout Reset)"
            isHighBmi && workout.workoutType == "POWER_WALK" ->
                "🛡️ Joint-Friendly & High Calorie Burn"
            workout.goal.contains(profile.mainGoal, ignoreCase = true) && workout.equipment.contains(profile.equipment, ignoreCase = true) ->
                "🎯 Exact Goal & Equipment Match (${profile.mainGoal})"
            workout.difficulty.equals(profile.fitnessLevel, ignoreCase = true) ->
                "⚡ Calibrated for ${profile.fitnessLevel} • ${workout.durationMin} Min"
            else ->
                "🔥 Recommended for ${profile.mainGoal}"
        }
    }

    private fun generateRationale(
        profile: UserProfileEntity,
        workout: WorkoutCatalogEntity,
        score: Int,
        isHighBmi: Boolean,
        needsActiveRecovery: Boolean
    ): String {
        val name = profile.name.ifBlank { "Athlete" }
        val equipText = if (workout.equipment == "No Equipment") "bodyweight only (no equipment required)" else "using ${workout.equipment}"
        val targetText = "${workout.durationMin} minutes in ${workout.targetZones}"

        return when {
            needsActiveRecovery ->
                "Recommended for $name today to facilitate neuromuscular recovery and metabolic clearance after prior high exertion without adding fatigue."
            isHighBmi && workout.workoutType == "POWER_WALK" ->
                "Optimized for $name's starting point: low impact on knee and hip joints while maintaining sustained Zone 2 fat oxidation."
            score >= 85 ->
                "Top match for your $profile.mainGoal goal and $profile.fitnessLevel fitness level, perfectly fitting your $targetText with $equipText."
            score >= 70 ->
                "Well-balanced stimulus aligned with your target training duration of ${workout.durationMin} min and available gear."
            else ->
                "Supplementary workout option to diversify movement patterns and stimulate different muscle groups."
        }
    }
}
