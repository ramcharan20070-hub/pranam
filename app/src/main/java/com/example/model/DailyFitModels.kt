package com.example.model

data class DailyFitStats(
    val dateMillis: Long = System.currentTimeMillis(),
    val formattedDate: String = "Today",
    val stepsCount: Int = 0,
    val stepGoal: Int = 10000,
    val activeCaloriesBurned: Int = 0,
    val totalCaloriesBurned: Int = 0, // BMR + Active
    val calorieGoal: Int = 650,
    val heartPoints: Int = 0,
    val heartPointsGoal: Int = 30,
    val moveMinutes: Int = 0,
    val moveMinutesGoal: Int = 60,
    val distanceKm: Double = 0.0,
    val waterIntakeMl: Int = 0,
    val waterGoalMl: Int = 2500,
    val sleepHours: Double = 8.0,
    val sleepQualityScore: Int = 85, // out of 100
    val isGoogleFitSynced: Boolean = false,
    val lastFitSyncTimestamp: Long = 0L
)

data class GoogleFitSyncResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long,
    val sessionsSynced: Int,
    val stepsSynced: Int,
    val caloriesSynced: Int
)
