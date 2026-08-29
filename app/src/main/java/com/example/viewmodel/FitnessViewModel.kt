package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiFitnessService
import com.example.ai.PostWorkoutAnalysis
import com.example.data.AppDatabase
import com.example.data.WorkoutRepository
import com.example.model.*
import com.example.tracker.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = WorkoutRepository(db.workoutDao())
    val trackerEngine = FitnessTrackerEngine(application)
    val geminiService = GeminiFitnessService()
    val smartwatchManager = SmartwatchSyncManager(application)
    val googleFitManager = GoogleFitHealthManager(application)
    val notificationPreferencesManager = NotificationPreferencesManager(application)
    val themePreferencesManager = ThemePreferencesManager(application)

    val liveState: StateFlow<LiveBiometricState> = trackerEngine.state
    val appThemeMode: StateFlow<AppThemeMode> = themePreferencesManager.themeMode

    // Notification & Scheduled Reminders flows
    val scheduledReminders: StateFlow<List<ScheduledWorkoutReminder>> = notificationPreferencesManager.reminders
    val dailyNotificationPrefs: StateFlow<DailyNotificationPreference> = notificationPreferencesManager.dailyPreferences
    val notificationHistory: StateFlow<List<NotificationHistoryItem>> = notificationPreferencesManager.notificationHistory

    // Smartwatch & BLE Heart Rate flows
    val discoveredWatches: StateFlow<List<SmartwatchDevice>> = smartwatchManager.discoveredDevices
    val connectedWatch: StateFlow<SmartwatchDevice?> = smartwatchManager.connectedWatch
    val isWatchScanning: StateFlow<Boolean> = smartwatchManager.isScanning
    val latestBlePayload: StateFlow<BleHeartRatePayload?> = smartwatchManager.latestBlePayload

    // Google Fit & Daily Health flows
    val dailyStats: StateFlow<DailyFitStats> = googleFitManager.dailyStats
    val isSyncingWithGoogleFit: StateFlow<Boolean> = googleFitManager.isSyncingWithGoogleFit
    val lastSyncResult: StateFlow<GoogleFitSyncResult?> = googleFitManager.lastSyncResult

    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfileEntity()
        )

    val allSessions: StateFlow<List<WorkoutSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSuggestions: StateFlow<List<WorkoutSuggestionEntity>> = repository.allSuggestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalDistanceMeters: StateFlow<Double> = repository.totalDistanceMeters
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCaloriesBurned: StateFlow<Int> = repository.totalCaloriesBurned
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDurationSeconds: StateFlow<Long> = repository.totalDurationSeconds
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val allWorkouts: StateFlow<List<WorkoutCatalogEntity>> = repository.allWorkouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val personalizedRecommendations: StateFlow<List<PersonalizedRecommendation>> = combine(
        userProfile,
        allWorkouts,
        allSessions
    ) { profile, catalog, sessions ->
        WorkoutRecommendationEngine.generatePersonalizedRecommendations(profile, catalog, sessions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _showSetupWizard = MutableStateFlow(false)
    val showSetupWizard: StateFlow<Boolean> = _showSetupWizard.asStateFlow()

    fun openSetupWizard() {
        _showSetupWizard.value = true
    }

    fun closeSetupWizard() {
        _showSetupWizard.value = false
    }

    // AI Coach Chat History
    private val _aiChatMessages = MutableStateFlow<List<AiCoachMessage>>(
        listOf(
            AiCoachMessage(
                text = "Welcome to Pranam AI. I'm your digital exercise physiologist and biometric coach. Start an activity to receive live tactical pacing cues, or ask me anything about your training, heart rate zones, and recovery.",
                isFromAi = true
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiCoachMessage>> = _aiChatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isGeneratingSuggestions = MutableStateFlow(false)
    val isGeneratingSuggestions: StateFlow<Boolean> = _isGeneratingSuggestions.asStateFlow()

    // Completed Session Modal
    private val _completedSessionSummary = MutableStateFlow<WorkoutSessionEntity?>(null)
    val completedSessionSummary: StateFlow<WorkoutSessionEntity?> = _completedSessionSummary.asStateFlow()

    private val _completedAnalysis = MutableStateFlow<PostWorkoutAnalysis?>(null)
    val completedAnalysis: StateFlow<PostWorkoutAnalysis?> = _completedAnalysis.asStateFlow()

    private val _isPostAnalysisLoading = MutableStateFlow(false)
    val isPostAnalysisLoading: StateFlow<Boolean> = _isPostAnalysisLoading.asStateFlow()

    // Simulation toggle state
    private val _isSimulationEnabled = MutableStateFlow(false)
    val isSimulationEnabled: StateFlow<Boolean> = _isSimulationEnabled.asStateFlow()

    init {
        // Ensure default profile is stored and catalog is seeded
        viewModelScope.launch {
            repository.ensureWorkoutsSeeded()
            val existing = repository.userProfile.firstOrNull()
            if (existing == null) {
                repository.updateProfile(UserProfileEntity(isSetupCompleted = false))
            }
        }

        // Bridge Smartwatch Live Telemetry to FitnessTrackerEngine
        smartwatchManager.setTelemetryListeners(
            onHeartRate = { hr, source ->
                trackerEngine.setExternalBiometrics(heartRate = hr, cadence = null, steps = null, source = source)
            },
            onCadence = { cadence ->
                trackerEngine.setExternalBiometrics(heartRate = null, cadence = cadence, steps = null)
            },
            onSteps = { steps ->
                trackerEngine.setExternalBiometrics(heartRate = null, cadence = null, steps = steps)
                if (steps > 0) {
                    googleFitManager.addManualSteps(steps)
                }
            }
        )

        // Automatically update daily energy expenditure when workout sessions change
        viewModelScope.launch {
            combine(userProfile, allSessions) { profile, sessions ->
                Pair(profile, sessions)
            }.collect { (profile, sessions) ->
                googleFitManager.calculateDailyEnergy(profile, sessions)
            }
        }
    }

    // --- Smartwatch Connectivity Controls ---
    fun startSmartwatchScan() {
        smartwatchManager.startScanning()
    }

    fun connectSmartwatch(device: SmartwatchDevice) {
        smartwatchManager.connectDevice(device)
    }

    fun disconnectSmartwatch(device: SmartwatchDevice) {
        smartwatchManager.disconnectDevice(device)
    }

    // --- Google Fit & Health Controls ---
    fun syncWithGoogleFit(onComplete: (GoogleFitSyncResult) -> Unit = {}) {
        val sessions = allSessions.value
        val profile = userProfile.value
        googleFitManager.syncWithGoogleFit(sessions, profile, onComplete)
    }

    fun addWaterIntake(amountMl: Int) {
        googleFitManager.addWater(amountMl)
    }

    fun resetWaterIntake() {
        googleFitManager.resetWater()
    }

    fun addDailySteps(steps: Int) {
        googleFitManager.addManualSteps(steps)
    }

    // --- Share Workout Report Handlers ---
    fun shareSessionTextReport(context: Context, session: WorkoutSessionEntity, analysis: PostWorkoutAnalysis? = null) {
        val report = WorkoutReportGenerator.generateFormattedTextReport(session, analysis)
        WorkoutReportGenerator.shareTextReport(context, report, "${session.workoutType} Session Report")
    }

    fun shareSessionImageSnapshot(context: Context, session: WorkoutSessionEntity, analysis: PostWorkoutAnalysis? = null) {
        val bitmap = WorkoutReportGenerator.generateWorkoutSnapshotBitmap(context, session, analysis)
        WorkoutReportGenerator.shareImageSnapshot(context, bitmap, "${session.workoutType} Session")
    }

    fun toggleSimulation(enabled: Boolean) {
        _isSimulationEnabled.value = enabled
        trackerEngine.setSimulation(enabled)
    }

    fun startWorkout(type: WorkoutType) {
        val profile = userProfile.value
        trackerEngine.startTracking(
            workoutType = type,
            userWeightKg = profile.weightKg,
            userMaxHr = profile.maxHeartRate,
            userRestingHr = profile.restingHeartRate
        )
        WorkoutLocationService.startService(getApplication<Application>(), type)
    }

    fun pauseWorkout() {
        trackerEngine.pauseTracking()
        WorkoutLocationService.pauseService(getApplication<Application>())
    }

    fun resumeWorkout() {
        val profile = userProfile.value
        trackerEngine.resumeTracking(
            userWeightKg = profile.weightKg,
            userMaxHr = profile.maxHeartRate,
            userRestingHr = profile.restingHeartRate
        )
        WorkoutLocationService.resumeService(getApplication<Application>())
    }

    fun finishWorkout() {
        WorkoutLocationService.stopService(getApplication<Application>())
        val finalState = trackerEngine.stopTracking()
        if (finalState.durationSeconds < 5 && finalState.distanceMeters < 10.0) {
            // Dismiss short accidental taps
            return
        }

        viewModelScope.launch {
            _isPostAnalysisLoading.value = true

            // Serialize Route Points
            val routeJson = JSONArray()
            finalState.routePoints.forEach { pt ->
                val obj = JSONObject()
                obj.put("lat", pt.latitude)
                obj.put("lng", pt.longitude)
                obj.put("alt", pt.altitude)
                obj.put("spd", pt.speedMps)
                obj.put("hr", pt.heartRate)
                obj.put("cad", pt.cadence)
                routeJson.put(obj)
            }

            // Serialize Zone Times
            val zonesJson = JSONObject()
            finalState.zoneTimesSeconds.forEach { (z, s) ->
                zonesJson.put(z.toString(), s)
            }

            // Resolve End & Start Location Names accurately
            val context = getApplication<Application>()
            val resolvedEndAddress = if (finalState.currentLat != 0.0 && finalState.currentLng != 0.0) {
                LocationAddressResolver.getAddressFromCoordinates(
                    context,
                    finalState.currentLat,
                    finalState.currentLng
                )
            } else {
                finalState.endLocationName.ifBlank { "Finish Point" }
            }

            val resolvedStartAddress = if (finalState.startLocationName.isNotBlank() && !finalState.startLocationName.startsWith("Locating")) {
                finalState.startLocationName
            } else if (finalState.startLat != 0.0 && finalState.startLng != 0.0) {
                LocationAddressResolver.getAddressFromCoordinates(
                    context,
                    finalState.startLat,
                    finalState.startLng
                )
            } else {
                "Starting Point"
            }

            val session = WorkoutSessionEntity(
                startTime = finalState.startTime,
                endTime = System.currentTimeMillis(),
                workoutType = finalState.workoutType.name,
                durationSeconds = finalState.durationSeconds,
                distanceMeters = finalState.distanceMeters,
                caloriesBurned = finalState.caloriesBurned,
                avgHeartRate = finalState.avgHeartRate,
                maxHeartRate = finalState.maxHeartRate,
                avgPaceSecPerKm = finalState.avgPaceSecPerKm,
                avgCadenceSpm = finalState.avgCadenceSpm,
                elevationGainMeters = finalState.elevationGainMeters,
                stepCount = finalState.stepCount,
                routePointsJson = routeJson.toString(),
                zoneTimesJson = zonesJson.toString(),
                aiCoachingSummary = "Analyzing performance...",
                aiRecoveryHours = 24,
                aiPerformanceScore = 85,
                aiTrainingStress = "Optimal",
                startLocationName = resolvedStartAddress,
                endLocationName = resolvedEndAddress,
                startLat = finalState.startLat,
                startLng = finalState.startLng,
                endLat = finalState.currentLat,
                endLng = finalState.currentLng
            )

            val profile = userProfile.value
            val analysis = geminiService.generatePostWorkoutDeepDive(session, profile)

            val finalSession = session.copy(
                aiCoachingSummary = analysis.coachingSummary,
                aiRecoveryHours = analysis.recoveryHours,
                aiPerformanceScore = analysis.performanceScore,
                aiTrainingStress = analysis.trainingStress
            )

            val sessionId = repository.saveSession(finalSession)
            if (finalState.stepCount > 0) {
                googleFitManager.addManualSteps(finalState.stepCount)
            }
            _completedSessionSummary.value = finalSession.copy(id = sessionId)
            _completedAnalysis.value = analysis
            _isPostAnalysisLoading.value = false

            // Auto-refresh recommendations based on the new session
            refreshAiWorkoutSuggestions()
        }
    }

    fun requestLiveAiCoaching() {
        if (!liveState.value.isTracking) return
        viewModelScope.launch {
            trackerEngine.setAiAnalyzing(true)
            val profile = userProfile.value
            val tip = geminiService.getLiveBiometricCoaching(liveState.value, profile)
            trackerEngine.setLiveAiTip(tip)
        }
    }

    fun refreshAiWorkoutSuggestions() {
        viewModelScope.launch {
            _isGeneratingSuggestions.value = true
            val profile = userProfile.value
            val recent = allSessions.value.take(5)
            val suggestions = geminiService.generatePersonalizedSuggestions(profile, recent)
            repository.saveSuggestions(suggestions)
            _isGeneratingSuggestions.value = false
        }
    }

    fun completeSuggestion(suggestionId: Long, isCompleted: Boolean = true) {
        viewModelScope.launch {
            repository.setSuggestionCompleted(suggestionId, isCompleted)
        }
    }

    fun sendCoachMessage(question: String) {
        if (question.isBlank()) return
        val userMsg = AiCoachMessage(text = question, isFromAi = false)
        _aiChatMessages.value = _aiChatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val profile = userProfile.value
            val latest = allSessions.value.firstOrNull()
            val aiResponse = geminiService.askCoach(question, profile, latest)
            val aiMsg = AiCoachMessage(text = aiResponse, isFromAi = true)
            _aiChatMessages.value = _aiChatMessages.value + aiMsg
            _isChatLoading.value = false
        }
    }

    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun saveFitnessSetup(profile: UserProfileEntity) {
        viewModelScope.launch {
            val updated = profile.copy(
                isSetupCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProfile(updated)
            _showSetupWizard.value = false
            // Refresh suggestions based on new profile
            refreshAiWorkoutSuggestions()
        }
    }

    fun resetFitnessSetup() {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(isSetupCompleted = false))
            _showSetupWizard.value = true
        }
    }

    fun startRecommendedWorkout(workout: WorkoutCatalogEntity) {
        val type = try {
            WorkoutType.valueOf(workout.workoutType)
        } catch (e: Exception) {
            WorkoutType.RUNNING
        }
        startWorkout(type)
    }

    // --- Notification & Scheduled Reminders Actions ---
    fun addOrUpdateScheduledReminder(reminder: ScheduledWorkoutReminder) {
        notificationPreferencesManager.addOrUpdateReminder(reminder)
        ReminderScheduler.scheduleWorkoutReminder(getApplication(), reminder)
    }

    fun toggleScheduledReminder(reminderId: Long, enabled: Boolean) {
        notificationPreferencesManager.toggleReminder(reminderId, enabled)
        val reminder = scheduledReminders.value.firstOrNull { it.id == reminderId }
        if (reminder != null) {
            ReminderScheduler.scheduleWorkoutReminder(getApplication(), reminder.copy(isEnabled = enabled))
        }
    }

    fun deleteScheduledReminder(reminderId: Long) {
        val reminder = scheduledReminders.value.firstOrNull { it.id == reminderId }
        if (reminder != null) {
            ReminderScheduler.cancelWorkoutReminder(getApplication(), reminder)
        }
        notificationPreferencesManager.deleteReminder(reminderId)
    }

    fun updateDailyNotificationPreferences(prefs: DailyNotificationPreference) {
        notificationPreferencesManager.saveDailyPreferences(prefs)
        ReminderScheduler.scheduleDailyBriefings(getApplication(), prefs)
    }

    fun testWorkoutReminderPush(workoutType: WorkoutType = WorkoutType.RUNNING) {
        ReminderScheduler.sendInstantTestWorkoutReminder(getApplication(), workoutType)
    }

    fun testMorningBriefingPush() {
        ReminderScheduler.sendInstantTestMorningBriefing(getApplication())
    }

    fun testEveningRecapPush() {
        ReminderScheduler.sendInstantTestEveningRecap(getApplication())
    }

    fun testInactivityAlertPush() {
        ReminderScheduler.sendInstantTestInactivityAlert(getApplication())
    }

    fun clearNotificationHistory() {
        notificationPreferencesManager.clearNotificationHistory()
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        themePreferencesManager.setThemeMode(mode)
    }

    fun toggleTheme() {
        themePreferencesManager.toggleTheme()
    }

    fun dismissCompletedSummary() {
        _completedSessionSummary.value = null
        _completedAnalysis.value = null
    }
}
