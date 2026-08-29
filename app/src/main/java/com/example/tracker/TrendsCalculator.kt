package com.example.tracker

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object TrendsCalculator {

    fun generateTrends(
        sessions: List<WorkoutSessionEntity>,
        timeframe: TrendsTimeframe,
        userProfile: UserProfileEntity
    ): List<DayPerformanceTrend> {
        val days = timeframe.days
        val calendar = Calendar.getInstance()
        
        // Reset to midnight of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

        val result = mutableListOf<DayPerformanceTrend>()

        // Generate days in chronological order (from (days - 1) days ago up to today)
        for (i in (days - 1) downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = todayMidnight - (i * 24 * 60 * 60 * 1000L)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1L
            val isToday = (i == 0)

            val daySessions = sessions.filter { it.startTime in startOfDay..endOfDay }

            val dayDistance = daySessions.sumOf { it.distanceMeters }
            val dayCalories = daySessions.sumOf { it.caloriesBurned }
            val dayDuration = daySessions.sumOf { it.durationSeconds }
            val workoutCount = daySessions.size

            val avgPace = if (daySessions.isNotEmpty()) {
                val validPaces = daySessions.map { it.avgPaceSecPerKm }.filter { it > 0 }
                if (validPaces.isNotEmpty()) (validPaces.average()).toInt() else 0
            } else 0

            val avgHr = if (daySessions.isNotEmpty()) {
                val validHrs = daySessions.map { it.avgHeartRate }.filter { it > 0 }
                if (validHrs.isNotEmpty()) (validHrs.average()).toInt() else 0
            } else 0

            val maxHr = if (daySessions.isNotEmpty()) {
                daySessions.maxOfOrNull { it.maxHeartRate } ?: 0
            } else 0

            // Heart Rate Variability (HRV) RMSSD physiological modeling:
            // Base RMSSD is around 64ms for fit adults.
            // Hard training creates transient sympathetic load (lowering RMSSD to ~48-54ms).
            // Rest and recovery days elevate parasympathetic tone (raising RMSSD to ~68-80ms).
            val pseudoNoise = (sin((dayCal.get(Calendar.DAY_OF_YEAR) * 0.7)) * 6).toInt()
            val volumeImpact = when {
                dayDistance > 10000 -> -10
                dayDistance > 5000 -> -5
                dayDistance > 0 -> -2
                else -> +6 // Rest day parasympathetic rebound
            }
            val baseRmssd = 64 + pseudoNoise + volumeImpact
            val clampedRmssd = baseRmssd.coerceIn(38, 88)
            val sdnn = (clampedRmssd * 1.15).toInt().coerceIn(45, 105)

            val status = HrvRecoveryStatus.fromRmssd(clampedRmssd)

            result.add(
                DayPerformanceTrend(
                    dateMillis = startOfDay,
                    dayLabel = dayFormat.format(Date(startOfDay)),
                    formattedDate = fullDateFormat.format(Date(startOfDay)),
                    isToday = isToday,
                    totalDistanceMeters = dayDistance,
                    workoutCount = workoutCount,
                    totalDurationSeconds = dayDuration,
                    caloriesBurned = dayCalories,
                    avgPaceSecPerKm = avgPace,
                    avgHeartRate = avgHr,
                    maxHeartRate = maxHr,
                    hrvRmssdMs = clampedRmssd,
                    hrvSdnnMs = sdnn,
                    recoveryStatus = status,
                    workouts = daySessions
                )
            )
        }

        return result
    }

    fun computeSummary(
        trends: List<DayPerformanceTrend>,
        userProfile: UserProfileEntity
    ): WeeklyPerformanceSummary {
        // Last 7 days slice for weekly summary
        val recent7 = trends.takeLast(7)
        val totalDistanceKm = recent7.sumOf { it.totalDistanceMeters } / 1000.0
        val targetWeeklyKm = if (userProfile.targetWeeklyKm > 0) userProfile.targetWeeklyKm else 35.0
        val goalPercent = min(200, ((totalDistanceKm / targetWeeklyKm) * 100).toInt())
        val totalCalories = recent7.sumOf { it.caloriesBurned }
        val totalActiveMinutes = recent7.sumOf { it.totalDurationSeconds } / 60
        val totalWorkouts = recent7.sumOf { it.workoutCount }

        val avgHrv = if (recent7.isNotEmpty()) (recent7.map { it.hrvRmssdMs }.average()).toInt() else 64
        val baseline = 60
        val hrvDiff = avgHrv - baseline

        val todayTrend = recent7.lastOrNull()
        val currentStatus = todayTrend?.recoveryStatus ?: HrvRecoveryStatus.BALANCED_READY

        val activePaces = recent7.map { it.avgPaceSecPerKm }.filter { it > 0 }
        val avgWeeklyPace = if (activePaces.isNotEmpty()) activePaces.average().toInt() else 315 // ~5:15/km

        val activeHrs = recent7.map { it.avgHeartRate }.filter { it > 0 }
        val avgWeeklyHr = if (activeHrs.isNotEmpty()) activeHrs.average().toInt() else 145

        val tsb = when {
            totalDistanceKm > targetWeeklyKm * 1.2 -> "High Strain"
            totalDistanceKm >= targetWeeklyKm * 0.7 -> "Optimal"
            else -> "Fresh / Base"
        }

        return WeeklyPerformanceSummary(
            totalDistanceKm = totalDistanceKm,
            targetWeeklyKm = targetWeeklyKm,
            distanceGoalPercent = goalPercent,
            totalCalories = totalCalories,
            totalActiveMinutes = totalActiveMinutes,
            workoutCount = totalWorkouts,
            avgHrvRmssd = avgHrv,
            hrvBaselineDiff = hrvDiff,
            currentRecoveryStatus = currentStatus,
            avgWeeklyPaceSecPerKm = avgWeeklyPace,
            avgWeeklyHeartRate = avgWeeklyHr,
            trainingStressBalance = tsb
        )
    }
}
