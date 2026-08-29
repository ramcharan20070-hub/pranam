package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class HrvRecoveryStatus(
    val title: String,
    val shortLabel: String,
    val advice: String,
    val color: Color
) {
    OPTIMAL_PRIMED(
        title = "Primed for Peak Performance",
        shortLabel = "Primed",
        advice = "High parasympathetic tone. Your autonomic nervous system is fully restored. Ideal for high-intensity intervals or long tempo runs.",
        color = NeonCyan
    ),
    BALANCED_READY(
        title = "Optimal Autonomic Balance",
        shortLabel = "Balanced",
        advice = "Normal baseline recovery. Ready for standard aerobic base building and targeted cardiovascular efforts.",
        color = NeonGreen
    ),
    MODERATE_FATIGUE(
        title = "Elevated Sympathetic Stress",
        shortLabel = "Moderate Stress",
        advice = "Elevated physiological strain detected. Favor Zone 2 aerobic volume or technical mobility over max-effort intervals.",
        color = NeonAmber
    ),
    STRAINED_RECOVERY(
        title = "High Strain / Low Recovery",
        shortLabel = "Needs Recovery",
        advice = "Autonomic suppression detected from cumulative training load. Recommend active recovery walk, sleep optimization, or rest.",
        color = NeonCoral
    );

    companion object {
        fun fromRmssd(rmssd: Int): HrvRecoveryStatus {
            return when {
                rmssd >= 70 -> OPTIMAL_PRIMED
                rmssd >= 56 -> BALANCED_READY
                rmssd >= 44 -> MODERATE_FATIGUE
                else -> STRAINED_RECOVERY
            }
        }
    }
}

enum class TrendsMetricMode(val title: String, val unit: String) {
    DISTANCE_VOLUME("Distance & Volume", "km"),
    HRV_RECOVERY("HRV & Recovery", "ms"),
    PACE_INTENSITY("Pace & Heart Rate", "/km & bpm")
}

enum class TrendsTimeframe(val days: Int, val label: String) {
    WEEK_7_DAYS(7, "7 Days"),
    DAYS_14(14, "14 Days"),
    DAYS_30(30, "30 Days")
}

data class DayPerformanceTrend(
    val dateMillis: Long,
    val dayLabel: String, // "Mon", "Tue", etc.
    val formattedDate: String, // "Wed, Aug 13"
    val isToday: Boolean = false,
    val totalDistanceMeters: Double = 0.0,
    val workoutCount: Int = 0,
    val totalDurationSeconds: Long = 0L,
    val caloriesBurned: Int = 0,
    val avgPaceSecPerKm: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val hrvRmssdMs: Int = 62, // RMSSD in ms
    val hrvSdnnMs: Int = 70, // SDNN in ms
    val recoveryStatus: HrvRecoveryStatus = HrvRecoveryStatus.BALANCED_READY,
    val workouts: List<WorkoutSessionEntity> = emptyList()
)

data class WeeklyPerformanceSummary(
    val totalDistanceKm: Double,
    val targetWeeklyKm: Double,
    val distanceGoalPercent: Int,
    val totalCalories: Int,
    val totalActiveMinutes: Long,
    val workoutCount: Int,
    val avgHrvRmssd: Int,
    val hrvBaselineDiff: Int, // e.g. +4 ms
    val currentRecoveryStatus: HrvRecoveryStatus,
    val avgWeeklyPaceSecPerKm: Int,
    val avgWeeklyHeartRate: Int,
    val trainingStressBalance: String // "Optimal", "High Strain", "Fresh"
)
