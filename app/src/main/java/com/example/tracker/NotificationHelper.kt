package com.example.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.DailyFitStats
import com.example.model.ScheduledWorkoutReminder
import com.example.model.WorkoutType

object NotificationHelper {

    const val CHANNEL_WORKOUT_REMINDERS = "channel_workout_reminders"
    const val CHANNEL_DAILY_PROGRESS = "channel_daily_progress"
    const val CHANNEL_AI_COACHING = "channel_ai_coaching"

    const val ACTION_START_WORKOUT = "com.example.ACTION_START_WORKOUT"
    const val ACTION_SNOOZE_WORKOUT = "com.example.ACTION_SNOOZE_WORKOUT"
    const val ACTION_VIEW_TRENDS = "com.example.ACTION_VIEW_TRENDS"
    const val ACTION_LOG_WATER = "com.example.ACTION_LOG_WATER"

    const val EXTRA_WORKOUT_TYPE = "extra_workout_type"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_NAV_DESTINATION = "extra_nav_destination"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Workout Reminders Channel (High Importance)
            val reminderChannel = NotificationChannel(
                CHANNEL_WORKOUT_REMINDERS,
                "Scheduled Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts for your scheduled running, cycling, and fitness sessions"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            // 2. Daily Progress & Trends Channel (Default Importance)
            val dailyProgressChannel = NotificationChannel(
                CHANNEL_DAILY_PROGRESS,
                "Daily Progress & Activity Trends",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning goal briefings, evening recaps, and activity milestone updates"
                enableLights(true)
                lightColor = Color.GREEN
                setShowBadge(true)
            }

            // 3. AI Coaching & Recovery Channel (Default Importance)
            val aiCoachingChannel = NotificationChannel(
                CHANNEL_AI_COACHING,
                "AI Physiological Coaching & Recovery",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recovery score recommendations, training stress warnings, and inactivity alerts"
                enableLights(true)
                lightColor = Color.YELLOW
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, dailyProgressChannel, aiCoachingChannel)
            )
        }
    }

    // --- 1. Push Notification: Scheduled Workout Reminder ---
    fun showWorkoutReminderNotification(
        context: Context,
        reminder: ScheduledWorkoutReminder,
        personalizedCue: String
    ) {
        createNotificationChannels(context)

        val notificationId = (reminder.id % 100000).toInt()

        // Tap Intent -> Opens Tracker directly with chosen workout
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_DESTINATION, "tracker")
            putExtra(EXTRA_WORKOUT_TYPE, reminder.workoutType.name)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "START NOW"
        val startWorkoutIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_START_WORKOUT
            putExtra(EXTRA_NAV_DESTINATION, "tracker")
            putExtra(EXTRA_WORKOUT_TYPE, reminder.workoutType.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val startPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            startWorkoutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "SNOOZE 15M"
        val snoozeIntent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_WORKOUT
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_WORKOUT_TYPE, reminder.workoutType.name)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ Scheduled Workout: ${reminder.workoutType.title}"
        val body = "${reminder.targetDurationMin} min session planned • $personalizedCue"

        val builder = NotificationCompat.Builder(context, CHANNEL_WORKOUT_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.parseColor("#00E5FF")) // Cyber Neon Cyan
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "START NOW", startPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "SNOOZE 15M", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- 2. Push Notification: Daily Morning Briefing ---
    fun showMorningBriefingNotification(
        context: Context,
        dailyStats: DailyFitStats,
        weeklyKmCompleted: Double,
        weeklyKmTarget: Double,
        readinessScore: Int,
        motivationalQuote: String
    ) {
        createNotificationChannels(context)
        val notificationId = 1001

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_DESTINATION, "tracker")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kmRemaining = (weeklyKmTarget - weeklyKmCompleted).coerceAtLeast(0.0)
        val title = "☀️ Morning Briefing • Readiness: $readinessScore%"
        val body = "Weekly Progress: ${String.format("%.1f", weeklyKmCompleted)} / ${weeklyKmTarget.toInt()} km (${String.format("%.1f", kmRemaining)} km to goal).\n$motivationalQuote"

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Readiness $readinessScore% • ${String.format("%.1f", weeklyKmCompleted)}/${weeklyKmTarget.toInt()} km weekly target")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.parseColor("#00E676")) // Neon Green
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- 3. Push Notification: Daily Evening Activity Recap ---
    fun showEveningRecapNotification(
        context: Context,
        dailyStats: DailyFitStats,
        burnedKcal: Int,
        steps: Int,
        stepGoal: Int
    ) {
        createNotificationChannels(context)
        val notificationId = 1002

        val viewTrendsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_DESTINATION, "history")
        }
        val trendsPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            viewTrendsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pct = ((steps.toFloat() / stepGoal) * 100).toInt()
        val title = "🌙 Daily Progress Wrap-Up: $steps Steps"
        val body = "You reached $pct% of your step target ($steps / $stepGoal) and torched $burnedKcal total calories today. Keep the momentum going!"

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.parseColor("#FFD600")) // Neon Amber
            .setAutoCancel(true)
            .setContentIntent(trendsPendingIntent)
            .addAction(android.R.drawable.ic_menu_sort_by_size, "VIEW TRENDS", trendsPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- 4. Push Notification: Inactivity & Recovery Alert ---
    fun showInactivityNudgeNotification(
        context: Context,
        daysInactive: Int,
        suggestedWorkoutType: WorkoutType = WorkoutType.POWER_WALK
    ) {
        createNotificationChannels(context)
        val notificationId = 1003

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_DESTINATION, "tracker")
            putExtra(EXTRA_WORKOUT_TYPE, suggestedWorkoutType.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🔥 Inactivity Alert: $daysInactive Days Resting"
        val body = "Your cardiovascular endurance peaks with consistent weekly stimulus. How about a brisk 20-min ${suggestedWorkoutType.title} to restart your streak?"

        val builder = NotificationCompat.Builder(context, CHANNEL_AI_COACHING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.parseColor("#FF3D71")) // Neon Coral
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "START ${suggestedWorkoutType.title.uppercase()}", pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
