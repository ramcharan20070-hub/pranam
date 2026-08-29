package com.example.tracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.DailyNotificationPreference
import com.example.model.ScheduledWorkoutReminder
import com.example.model.WorkoutType
import java.util.Calendar

object ReminderScheduler {

    fun scheduleWorkoutReminder(context: Context, reminder: ScheduledWorkoutReminder) {
        if (!reminder.isEnabled) {
            cancelWorkoutReminder(context, reminder)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            action = WorkoutAlarmReceiver.ACTION_TRIGGER_WORKOUT_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminder.id)
            putExtra(NotificationHelper.EXTRA_WORKOUT_TYPE, reminder.workoutType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextTriggerTime(reminder.hour, reminder.minute, reminder.daysOfWeek)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelWorkoutReminder(context: Context, reminder: ScheduledWorkoutReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            action = WorkoutAlarmReceiver.ACTION_TRIGGER_WORKOUT_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(context: Context, reminder: ScheduledWorkoutReminder, snoozeMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            action = WorkoutAlarmReceiver.ACTION_TRIGGER_WORKOUT_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminder.id)
            putExtra(NotificationHelper.EXTRA_WORKOUT_TYPE, reminder.workoutType.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id + 999).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleDailyBriefings(context: Context, prefs: DailyNotificationPreference) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Morning Briefing
        if (prefs.morningBriefingEnabled) {
            val morningIntent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
                action = WorkoutAlarmReceiver.ACTION_TRIGGER_MORNING_BRIEFING
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                2001,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val morningTime = getDailyTriggerTime(prefs.morningHour, prefs.morningMinute)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, morningTime, morningPending)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Evening Recap
        if (prefs.eveningRecapEnabled) {
            val eveningIntent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
                action = WorkoutAlarmReceiver.ACTION_TRIGGER_EVENING_RECAP
            }
            val eveningPending = PendingIntent.getBroadcast(
                context,
                2002,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val eveningTime = getDailyTriggerTime(prefs.eveningHour, prefs.eveningMinute)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, eveningTime, eveningPending)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rescheduleAll(context: Context) {
        val prefManager = NotificationPreferencesManager(context)
        val reminders = prefManager.reminders.value
        val dailyPrefs = prefManager.dailyPreferences.value

        reminders.forEach { scheduleWorkoutReminder(context, it) }
        scheduleDailyBriefings(context, dailyPrefs)
    }

    // --- Immediate Push Notification Test Triggers for Development & Demo ---
    fun sendInstantTestWorkoutReminder(context: Context, workoutType: WorkoutType = WorkoutType.RUNNING) {
        val testReminder = ScheduledWorkoutReminder(
            id = 9999L,
            title = "Scheduled ${workoutType.title}",
            workoutType = workoutType,
            targetDurationMin = 40,
            focusRationale = "Aerobic Endurance & Zone 2/3 Cardio",
            customMotivationCue = "Target steady 140 BPM pulse and consistent stride frequency."
        )
        NotificationHelper.showWorkoutReminderNotification(
            context = context,
            reminder = testReminder,
            personalizedCue = "Target steady 140 BPM pulse and consistent stride frequency."
        )

        val prefManager = NotificationPreferencesManager(context)
        prefManager.addNotificationHistory(
            com.example.model.NotificationHistoryItem(
                title = "Push Reminder: ${workoutType.title}",
                message = "40 min scheduled workout • Aerobic Base",
                category = "WORKOUT_REMINDER",
                workoutType = workoutType.name
            )
        )
    }

    fun sendInstantTestMorningBriefing(context: Context) {
        val googleFitManager = GoogleFitHealthManager(context)
        val dailyStats = googleFitManager.dailyStats.value
        NotificationHelper.showMorningBriefingNotification(
            context = context,
            dailyStats = dailyStats,
            weeklyKmCompleted = 18.5,
            weeklyKmTarget = 30.0,
            readinessScore = 91,
            motivationalQuote = "Peak physiological recovery detected. Perfect day for tempo intervals!"
        )

        val prefManager = NotificationPreferencesManager(context)
        prefManager.addNotificationHistory(
            com.example.model.NotificationHistoryItem(
                title = "Morning Progress Briefing",
                message = "Readiness 91% • 18.5/30 km weekly target achieved",
                category = "DAILY_BRIEFING"
            )
        )
    }

    fun sendInstantTestEveningRecap(context: Context) {
        val googleFitManager = GoogleFitHealthManager(context)
        val dailyStats = googleFitManager.dailyStats.value
        NotificationHelper.showEveningRecapNotification(
            context = context,
            dailyStats = dailyStats,
            burnedKcal = 640,
            steps = 8920,
            stepGoal = 10000
        )

        val prefManager = NotificationPreferencesManager(context)
        prefManager.addNotificationHistory(
            com.example.model.NotificationHistoryItem(
                title = "Evening Progress Wrap-Up",
                message = "8,920 steps (89%) • 640 kcal burned today",
                category = "TREND_UPDATE"
            )
        )
    }

    fun sendInstantTestInactivityAlert(context: Context) {
        NotificationHelper.showInactivityNudgeNotification(
            context = context,
            daysInactive = 2,
            suggestedWorkoutType = WorkoutType.POWER_WALK
        )

        val prefManager = NotificationPreferencesManager(context)
        prefManager.addNotificationHistory(
            com.example.model.NotificationHistoryItem(
                title = "Inactivity Reminder (2 Days)",
                message = "Suggested: 20 min active recovery walk to maintain fitness habit.",
                category = "RECOVERY_ALERT"
            )
        )
    }

    private fun getNextTriggerTime(targetHour: Int, targetMinute: Int, daysOfWeek: List<Int>): Long {
        val now = Calendar.getInstance()
        var closestDiff = Long.MAX_VALUE
        var nextTime = now.timeInMillis

        for (day in daysOfWeek) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, day)
            }

            if (candidate.timeInMillis <= now.timeInMillis) {
                // If in the past this week, advance by 7 days
                candidate.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val diff = candidate.timeInMillis - now.timeInMillis
            if (diff in 1..<closestDiff) {
                closestDiff = diff
                nextTime = candidate.timeInMillis
            }
        }
        return if (closestDiff == Long.MAX_VALUE) now.timeInMillis + 86400000L else nextTime
    }

    private fun getDailyTriggerTime(targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
