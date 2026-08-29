package com.example.model

import com.example.ui.theme.*
import java.util.Calendar

data class ScheduledWorkoutReminder(
    val id: Long = System.currentTimeMillis(),
    val title: String = "Morning Workout",
    val workoutType: WorkoutType = WorkoutType.RUNNING,
    val hour: Int = 7,
    val minute: Int = 0,
    val daysOfWeek: List<Int> = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
    val targetDurationMin: Int = 35,
    val isEnabled: Boolean = true,
    val focusRationale: String = "Zone 2 Base Building",
    val customMotivationCue: String? = null
) {
    fun getFormattedTime(): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val m = String.format("%02d", minute)
        val amPm = if (hour >= 12) "PM" else "AM"
        return "$h:$m $amPm"
    }

    fun getDaysSummary(): String {
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek.containsAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) && daysOfWeek.size == 5) {
            return "Weekdays (Mon-Fri)"
        }
        if (daysOfWeek.containsAll(listOf(Calendar.SATURDAY, Calendar.SUNDAY)) && daysOfWeek.size == 2) {
            return "Weekends (Sat-Sun)"
        }
        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}

data class DailyNotificationPreference(
    val notificationsMasterEnabled: Boolean = true,
    val morningBriefingEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val eveningRecapEnabled: Boolean = true,
    val eveningHour: Int = 20,
    val eveningMinute: Int = 0,
    val inactivityAlertEnabled: Boolean = true,
    val inactivityThresholdDays: Int = 2,
    val aiPersonalizedTone: String = "Scientific & High Performance",
    val smartTrendsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

data class NotificationHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "WORKOUT_REMINDER",
    val workoutType: String? = null
)
