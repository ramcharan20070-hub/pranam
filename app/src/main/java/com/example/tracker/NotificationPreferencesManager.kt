package com.example.tracker

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class NotificationPreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pulsetrack_notifications_prefs", Context.MODE_PRIVATE)

    private val _reminders = MutableStateFlow<List<ScheduledWorkoutReminder>>(emptyList())
    val reminders: StateFlow<List<ScheduledWorkoutReminder>> = _reminders.asStateFlow()

    private val _dailyPreferences = MutableStateFlow(DailyNotificationPreference())
    val dailyPreferences: StateFlow<DailyNotificationPreference> = _dailyPreferences.asStateFlow()

    private val _notificationHistory = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
    val notificationHistory: StateFlow<List<NotificationHistoryItem>> = _notificationHistory.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        // Load Daily Preferences
        val masterEnabled = prefs.getBoolean("master_enabled", true)
        val morningEnabled = prefs.getBoolean("morning_enabled", true)
        val morningHour = prefs.getInt("morning_hour", 8)
        val morningMinute = prefs.getInt("morning_minute", 0)
        val eveningEnabled = prefs.getBoolean("evening_enabled", true)
        val eveningHour = prefs.getInt("evening_hour", 20)
        val eveningMinute = prefs.getInt("evening_minute", 0)
        val inactivityEnabled = prefs.getBoolean("inactivity_enabled", true)
        val inactivityDays = prefs.getInt("inactivity_days", 2)
        val aiTone = prefs.getString("ai_tone", "Scientific & High Performance") ?: "Scientific & High Performance"
        val smartTrends = prefs.getBoolean("smart_trends", true)

        _dailyPreferences.value = DailyNotificationPreference(
            notificationsMasterEnabled = masterEnabled,
            morningBriefingEnabled = morningEnabled,
            morningHour = morningHour,
            morningMinute = morningMinute,
            eveningRecapEnabled = eveningEnabled,
            eveningHour = eveningHour,
            eveningMinute = eveningMinute,
            inactivityAlertEnabled = inactivityEnabled,
            inactivityThresholdDays = inactivityDays,
            aiPersonalizedTone = aiTone,
            smartTrendsEnabled = smartTrends
        )

        // Load Scheduled Workout Reminders
        val remindersJson = prefs.getString("reminders_json", null)
        if (remindersJson != null) {
            try {
                val list = mutableListOf<ScheduledWorkoutReminder>()
                val array = JSONArray(remindersJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val daysArray = obj.getJSONArray("days")
                    val days = mutableListOf<Int>()
                    for (d in 0 until daysArray.length()) {
                        days.add(daysArray.getInt(d))
                    }

                    list.add(
                        ScheduledWorkoutReminder(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            workoutType = WorkoutType.valueOf(obj.optString("type", WorkoutType.RUNNING.name)),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute"),
                            daysOfWeek = days,
                            targetDurationMin = obj.optInt("duration", 30),
                            isEnabled = obj.optBoolean("enabled", true),
                            focusRationale = obj.optString("rationale", "Aerobic Conditioning"),
                            customMotivationCue = if (obj.has("customCue") && !obj.isNull("customCue")) obj.getString("customCue") else null
                        )
                    )
                }
                _reminders.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                setDefaultReminders()
            }
        } else {
            setDefaultReminders()
        }

        // Load Notification History
        val historyJson = prefs.getString("history_json", null)
        if (historyJson != null) {
            try {
                val list = mutableListOf<NotificationHistoryItem>()
                val array = JSONArray(historyJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        NotificationHistoryItem(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            message = obj.getString("message"),
                            timestamp = obj.getLong("timestamp"),
                            category = obj.getString("category"),
                            workoutType = if (obj.has("workoutType") && !obj.isNull("workoutType")) obj.getString("workoutType") else null
                        )
                    )
                }
                _notificationHistory.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setDefaultReminders() {
        val defaults = listOf(
            ScheduledWorkoutReminder(
                id = 1001L,
                title = "Morning Tempo Run",
                workoutType = WorkoutType.RUNNING,
                hour = 7,
                minute = 0,
                daysOfWeek = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
                targetDurationMin = 35,
                isEnabled = true,
                focusRationale = "Aerobic Base & Lactate Threshold",
                customMotivationCue = "Target steady Zone 3 pacing with quick cadence."
            ),
            ScheduledWorkoutReminder(
                id = 1002L,
                title = "Evening Cycling Surge",
                workoutType = WorkoutType.CYCLING,
                hour = 18,
                minute = 30,
                daysOfWeek = listOf(Calendar.TUESDAY, Calendar.THURSDAY),
                targetDurationMin = 45,
                isEnabled = true,
                focusRationale = "Leg Turnover & Power Intervals",
                customMotivationCue = "Maintain smooth 85+ RPM cadence throughout."
            ),
            ScheduledWorkoutReminder(
                id = 1003L,
                title = "Weekend Long Endurance",
                workoutType = WorkoutType.RUNNING,
                hour = 8,
                minute = 0,
                daysOfWeek = listOf(Calendar.SATURDAY),
                targetDurationMin = 60,
                isEnabled = true,
                focusRationale = "Zone 2 Mitochondrial Base",
                customMotivationCue = "Keep heart rate under 145 BPM for pure fat oxidation."
            )
        )
        saveReminders(defaults)
    }

    fun saveReminders(newList: List<ScheduledWorkoutReminder>) {
        _reminders.value = newList
        val array = JSONArray()
        newList.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("title", reminder.title)
                put("type", reminder.workoutType.name)
                put("hour", reminder.hour)
                put("minute", reminder.minute)
                val daysArr = JSONArray()
                reminder.daysOfWeek.forEach { daysArr.put(it) }
                put("days", daysArr)
                put("duration", reminder.targetDurationMin)
                put("enabled", reminder.isEnabled)
                put("rationale", reminder.focusRationale)
                if (reminder.customMotivationCue != null) {
                    put("customCue", reminder.customMotivationCue)
                }
            }
            array.put(obj)
        }
        prefs.edit().putString("reminders_json", array.toString()).apply()
    }

    fun addOrUpdateReminder(reminder: ScheduledWorkoutReminder) {
        val current = _reminders.value.toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            current[index] = reminder
        } else {
            current.add(reminder)
        }
        saveReminders(current)
    }

    fun toggleReminder(reminderId: Long, enabled: Boolean) {
        val current = _reminders.value.map {
            if (it.id == reminderId) it.copy(isEnabled = enabled) else it
        }
        saveReminders(current)
    }

    fun deleteReminder(reminderId: Long) {
        val current = _reminders.value.filter { it.id != reminderId }
        saveReminders(current)
    }

    fun saveDailyPreferences(prefsState: DailyNotificationPreference) {
        _dailyPreferences.value = prefsState
        prefs.edit()
            .putBoolean("master_enabled", prefsState.notificationsMasterEnabled)
            .putBoolean("morning_enabled", prefsState.morningBriefingEnabled)
            .putInt("morning_hour", prefsState.morningHour)
            .putInt("morning_minute", prefsState.morningMinute)
            .putBoolean("evening_enabled", prefsState.eveningRecapEnabled)
            .putInt("evening_hour", prefsState.eveningHour)
            .putInt("evening_minute", prefsState.eveningMinute)
            .putBoolean("inactivity_enabled", prefsState.inactivityAlertEnabled)
            .putInt("inactivity_days", prefsState.inactivityThresholdDays)
            .putString("ai_tone", prefsState.aiPersonalizedTone)
            .putBoolean("smart_trends", prefsState.smartTrendsEnabled)
            .apply()
    }

    fun addNotificationHistory(item: NotificationHistoryItem) {
        val updated = (listOf(item) + _notificationHistory.value).take(30)
        _notificationHistory.value = updated
        val array = JSONArray()
        updated.forEach { hist ->
            val obj = JSONObject().apply {
                put("id", hist.id)
                put("title", hist.title)
                put("message", hist.message)
                put("timestamp", hist.timestamp)
                put("category", hist.category)
                if (hist.workoutType != null) {
                    put("workoutType", hist.workoutType)
                }
            }
            array.put(obj)
        }
        prefs.edit().putString("history_json", array.toString()).apply()
    }

    fun clearNotificationHistory() {
        _notificationHistory.value = emptyList()
        prefs.edit().remove("history_json").apply()
    }
}
