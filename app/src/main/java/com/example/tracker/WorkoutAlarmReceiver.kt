package com.example.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.model.NotificationHistoryItem
import com.example.model.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_WORKOUT_REMINDER = "com.example.ACTION_TRIGGER_WORKOUT_REMINDER"
        const val ACTION_TRIGGER_MORNING_BRIEFING = "com.example.ACTION_TRIGGER_MORNING_BRIEFING"
        const val ACTION_TRIGGER_EVENING_RECAP = "com.example.ACTION_TRIGGER_EVENING_RECAP"
        const val ACTION_TRIGGER_INACTIVITY_CHECK = "com.example.ACTION_TRIGGER_INACTIVITY_CHECK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefManager = NotificationPreferencesManager(context)
                val dailyPrefs = prefManager.dailyPreferences.value

                if (!dailyPrefs.notificationsMasterEnabled && action != NotificationHelper.ACTION_SNOOZE_WORKOUT) {
                    pendingResult.finish()
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                val profile = db.workoutDao().getUserProfile().firstOrNull()
                val weeklyKmTarget = profile?.targetWeeklyKm?.toDouble() ?: 25.0
                val totalDistanceMeters = db.workoutDao().getTotalDistanceMeters().firstOrNull() ?: 0.0
                val weeklyKmCompleted = totalDistanceMeters / 1000.0

                when (action) {
                    ACTION_TRIGGER_WORKOUT_REMINDER -> {
                        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
                        val reminder = prefManager.reminders.value.firstOrNull { it.id == reminderId }
                        if (reminder != null && reminder.isEnabled) {
                            val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                            if (reminder.daysOfWeek.contains(todayDayOfWeek)) {
                                val cue = reminder.customMotivationCue ?: "Stay consistent! Every session expands your mitochondrial aerobic base."
                                NotificationHelper.showWorkoutReminderNotification(
                                    context = context,
                                    reminder = reminder,
                                    personalizedCue = cue
                                )
                                prefManager.addNotificationHistory(
                                    NotificationHistoryItem(
                                        title = "Scheduled Workout: ${reminder.workoutType.title}",
                                        message = "${reminder.targetDurationMin} min • $cue",
                                        category = "WORKOUT_REMINDER",
                                        workoutType = reminder.workoutType.name
                                    )
                                )
                            }
                            // Reschedule for future occurrence
                            ReminderScheduler.scheduleWorkoutReminder(context, reminder)
                        }
                    }

                    NotificationHelper.ACTION_SNOOZE_WORKOUT -> {
                        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
                        val reminder = prefManager.reminders.value.firstOrNull { it.id == reminderId }
                        if (reminder != null) {
                            ReminderScheduler.scheduleSnooze(context, reminder, 15)
                        }
                    }

                    ACTION_TRIGGER_MORNING_BRIEFING -> {
                        if (dailyPrefs.morningBriefingEnabled) {
                            val googleFitManager = GoogleFitHealthManager(context)
                            val dailyStats = googleFitManager.dailyStats.value
                            val motivationalQuotes = listOf(
                                "Your recovery score is optimal today. Seize the physiological window for quality endurance!",
                                "Consistency compounds: ${String.format("%.1f", weeklyKmCompleted)} km logged this week so far!",
                                "Hydrate early and set a steady pacing rhythm for today's physical challenges."
                            )
                            val quote = motivationalQuotes.random()

                            NotificationHelper.showMorningBriefingNotification(
                                context = context,
                                dailyStats = dailyStats,
                                weeklyKmCompleted = weeklyKmCompleted,
                                weeklyKmTarget = weeklyKmTarget,
                                readinessScore = dailyStats.sleepQualityScore,
                                motivationalQuote = quote
                            )

                            prefManager.addNotificationHistory(
                                NotificationHistoryItem(
                                    title = "Morning Briefing",
                                    message = "Readiness ${dailyStats.sleepQualityScore}% • $weeklyKmCompleted / ${weeklyKmTarget.toInt()} km target",
                                    category = "DAILY_BRIEFING"
                                )
                            )
                            ReminderScheduler.scheduleDailyBriefings(context, dailyPrefs)
                        }
                    }

                    ACTION_TRIGGER_EVENING_RECAP -> {
                        if (dailyPrefs.eveningRecapEnabled) {
                            val googleFitManager = GoogleFitHealthManager(context)
                            val dailyStats = googleFitManager.dailyStats.value

                            NotificationHelper.showEveningRecapNotification(
                                context = context,
                                dailyStats = dailyStats,
                                burnedKcal = dailyStats.totalCaloriesBurned,
                                steps = dailyStats.stepsCount,
                                stepGoal = dailyStats.stepGoal
                            )

                            prefManager.addNotificationHistory(
                                NotificationHistoryItem(
                                    title = "Evening Progress Wrap-Up",
                                    message = "${dailyStats.stepsCount} steps • ${dailyStats.totalCaloriesBurned} kcal burned",
                                    category = "TREND_UPDATE"
                                )
                            )
                            ReminderScheduler.scheduleDailyBriefings(context, dailyPrefs)
                        }
                    }

                    ACTION_TRIGGER_INACTIVITY_CHECK -> {
                        if (dailyPrefs.inactivityAlertEnabled) {
                            val sessions = db.workoutDao().getAllSessions().firstOrNull() ?: emptyList()
                            val lastSession = sessions.firstOrNull()
                            val now = System.currentTimeMillis()
                            val daysSinceLast = if (lastSession != null) {
                                ((now - lastSession.endTime) / (1000 * 60 * 60 * 24)).toInt()
                            } else 3

                            if (daysSinceLast >= dailyPrefs.inactivityThresholdDays) {
                                NotificationHelper.showInactivityNudgeNotification(
                                    context = context,
                                    daysInactive = daysSinceLast,
                                    suggestedWorkoutType = WorkoutType.POWER_WALK
                                )
                                prefManager.addNotificationHistory(
                                    NotificationHistoryItem(
                                        title = "Inactivity Reminder ($daysSinceLast Days)",
                                        message = "Suggested: 20 min active recovery walk to maintain fitness habit.",
                                        category = "RECOVERY_ALERT"
                                    )
                                )
                            }
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED -> {
                        // Re-register all scheduled alarms after reboot
                        ReminderScheduler.rescheduleAll(context)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
