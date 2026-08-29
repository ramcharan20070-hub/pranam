package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color

enum class WorkoutType(
    val title: String,
    val metValue: Double,
    val typicalCadence: Int,
    val iconName: String,
    val colorHex: Long
) {
    RUNNING("Running", 9.8, 168, "DirectionsRun", 0xFF00E5FF),
    CYCLING("Cycling", 8.5, 85, "DirectionsBike", 0xFF00E676),
    TRAIL_RUN("Trail Run", 11.0, 160, "Terrain", 0xFFFFAB00),
    HIIT_SPRINT("HIIT Sprint", 12.5, 182, "FlashOn", 0xFFFF3366),
    POWER_WALK("Power Walk", 4.8, 125, "DirectionsWalk", 0xFF7C4DFF),
    HIKING("Mountain Hike", 7.5, 110, "Landscape", 0xFF2979FF),
    ROWING("Cardio Rowing", 9.0, 30, "Rowing", 0xFFFF4081);

    companion object {
        fun fromString(name: String?): WorkoutType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) }
                ?: RUNNING
        }
    }
}

enum class HeartRateZone(
    val zoneNumber: Int,
    val title: String,
    val minPercent: Int,
    val maxPercent: Int,
    val description: String
) {
    ZONE_1(1, "Warm Up", 50, 60, "Active recovery, gentle warm-up"),
    ZONE_2(2, "Fat Burn", 60, 70, "Base building, fat metabolism"),
    ZONE_3(3, "Aerobic", 70, 80, "Cardiovascular endurance & stamina"),
    ZONE_4(4, "Anaerobic", 80, 90, "Lactate threshold, speed endurance"),
    ZONE_5(5, "VO2 Max", 90, 100, "Maximum effort, peak performance");

    fun getColor(): Color {
        return when (this) {
            ZONE_1 -> Zone1WarmUp
            ZONE_2 -> Zone2FatBurn
            ZONE_3 -> Zone3Aerobic
            ZONE_4 -> Zone4Anaerobic
            ZONE_5 -> Zone5Peak
        }
    }

    companion object {
        fun fromHeartRate(currentHr: Int, maxHr: Int): HeartRateZone {
            val safeMax = if (maxHr > 50) maxHr else 190
            val pct = (currentHr.toFloat() / safeMax.toFloat()) * 100f
            return when {
                pct < 60f -> ZONE_1
                pct < 70f -> ZONE_2
                pct < 80f -> ZONE_3
                pct < 90f -> ZONE_4
                else -> ZONE_5
            }
        }
    }
}

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int = 120,
    val cadence: Int = 160
)

data class WorkoutSplit(
    val splitNumber: Int,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Int,
    val avgHeartRate: Int,
    val elevationDeltaMeters: Double
)

data class LiveBiometricState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val workoutType: WorkoutType = WorkoutType.RUNNING,
    val startTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Float = 0f,
    val currentPaceSecPerKm: Int = 0,
    val avgPaceSecPerKm: Int = 0,
    val currentHeartRate: Int = 75,
    val avgHeartRate: Int = 75,
    val maxHeartRate: Int = 75,
    val currentCadenceSpm: Int = 0,
    val avgCadenceSpm: Int = 0,
    val currentElevationMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val stepCount: Int = 0,
    val caloriesBurned: Int = 0,
    val currentZone: HeartRateZone = HeartRateZone.ZONE_1,
    val zoneTimesSeconds: Map<Int, Long> = mapOf(1 to 0L, 2 to 0L, 3 to 0L, 4 to 0L, 5 to 0L),
    val routePoints: List<GpsPoint> = emptyList(),
    val fatigueScore: Int = 12, // 0 - 100
    val staminaPercent: Int = 95, // 100 - 0%
    val liveAiTip: String? = null,
    val liveAiTipTimestamp: Long = 0L,
    val isAiAnalyzing: Boolean = false,
    val splits: List<WorkoutSplit> = emptyList(),
    val isGpsActive: Boolean = false,
    val hasGpsFix: Boolean = false,
    val hrSource: String = "Standby",
    val hasLiveHrSensor: Boolean = false,
    val startLocationName: String = "",
    val endLocationName: String = "",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val workoutType: String = WorkoutType.RUNNING.name,
    val durationSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val caloriesBurned: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val avgPaceSecPerKm: Int = 0,
    val avgCadenceSpm: Int = 0,
    val elevationGainMeters: Double = 0.0,
    val stepCount: Int = 0,
    val routePointsJson: String = "[]",
    val zoneTimesJson: String = "{}",
    val aiCoachingSummary: String = "",
    val aiRecoveryHours: Int = 24,
    val aiPerformanceScore: Int = 85,
    val aiTrainingStress: String = "Optimal",
    val startLocationName: String = "Starting Point",
    val endLocationName: String = "Finish Point",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Rahul",
    val age: Int = 25,
    val gender: String = "Male", // "Male", "Female", "Other", "Prefer not to say"
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val fitnessLevel: String = "Beginner", // "Beginner", "Intermediate", "Advanced"
    val mainGoal: String = "Weight Loss", // "Lose Weight", "Gain Muscle", "Improve Fitness", "Increase Strength", "Improve Endurance", "Stay Active"
    val activityLevel: String = "Moderately Active", // "Sedentary", "Lightly Active", "Moderately Active", "Very Active"
    val workoutDays: Int = 5, // 1 to 7
    val workoutDuration: Int = 30, // 10, 20, 30, 45, 60
    val equipment: String = "No Equipment", // "No Equipment", "Dumbbells", "Resistance Bands", "Gym Equipment", "Full Gym"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSetupCompleted: Boolean = false,
    val maxHeartRate: Int = 195,
    val restingHeartRate: Int = 62,
    val fitnessGoal: String = "Weight Loss",
    val experienceLevel: String = "Beginner",
    val targetWeeklyKm: Double = 25.0,
    val targetWeeklyCalories: Int = 2500
)

@Entity(tableName = "workouts")
data class WorkoutCatalogEntity(
    @PrimaryKey(autoGenerate = true)
    val workoutId: Long = 0L,
    val workoutName: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val goal: String, // "Lose Weight", "Gain Muscle", "Improve Fitness", "Increase Strength", "Improve Endurance", "Stay Active"
    val equipment: String, // "No Equipment", "Dumbbells", "Resistance Bands", "Gym Equipment", "Full Gym"
    val durationMin: Int,
    val caloriesEst: Int,
    val workoutType: String, // "RUNNING", "CYCLING", "HIIT_SPRINT", "POWER_WALK", "TRAIL_RUN", "ROWING", "HIKING"
    val description: String,
    val targetZones: String,
    val intervals: String = ""
)

data class PersonalizedRecommendation(
    val workout: WorkoutCatalogEntity,
    val matchScore: Int, // 0 - 100%
    val matchTag: String, // e.g. "Optimal for Weight Loss & No Equipment"
    val rationale: String,
    val isTodayTopPick: Boolean = false
)

@Entity(tableName = "workout_suggestions")
data class WorkoutSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val workoutType: String,
    val targetDurationMin: Int,
    val targetZone: String,
    val intensity: String, // "Low Recovery", "Moderate Aerobic", "High Intensity", "Threshold"
    val rationale: String,
    val recommendedIntervals: String,
    val dateRecommended: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

data class AiCoachMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromAi: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String? = null
)
