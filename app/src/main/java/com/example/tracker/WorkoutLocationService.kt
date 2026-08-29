package com.example.tracker

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.LiveBiometricState
import com.example.model.WorkoutType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class WorkoutLocationService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    companion object {
        const val CHANNEL_ID = "pulsetrack_workout_telemetry"
        const val CHANNEL_NAME = "Active Workout Telemetry"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.example.action.START_TRACKING"
        const val ACTION_PAUSE = "com.example.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.example.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_TRACKING"

        const val EXTRA_WORKOUT_TYPE = "extra_workout_type"

        fun startService(context: Context, workoutType: WorkoutType) {
            val intent = Intent(context, WorkoutLocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_TYPE, workoutType.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, WorkoutLocationService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, WorkoutLocationService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WorkoutLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                val workoutTypeName = intent?.getStringExtra(EXTRA_WORKOUT_TYPE)
                val workoutType = WorkoutType.fromString(workoutTypeName)
                startForegroundWithNotification(workoutType)
                observeTrackerState()
            }
            ACTION_PAUSE -> {
                FitnessTrackerEngine.instance?.pauseTracking()
                updateNotification()
            }
            ACTION_RESUME -> {
                FitnessTrackerEngine.instance?.resumeTracking()
                updateNotification()
            }
            ACTION_STOP -> {
                stopForegroundTracking()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification(workoutType: WorkoutType) {
        val notification = buildNotification(
            state = FitnessTrackerEngine.instance?.state?.value ?: LiveBiometricState(workoutType = workoutType)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeTrackerState() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            FitnessTrackerEngine.instance?.state?.collectLatest { state ->
                if (!state.isTracking) {
                    stopForegroundTracking()
                } else {
                    updateNotification(state)
                }
            }
        }
    }

    private fun updateNotification(state: LiveBiometricState? = FitnessTrackerEngine.instance?.state?.value) {
        val st = state ?: return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(st))
    }

    private fun buildNotification(state: LiveBiometricState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause / Resume Toggle Action
        val toggleActionIntent = Intent(this, WorkoutLocationService::class.java).apply {
            action = if (state.isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingToggle = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (state.isPaused) "Resume" else "Pause"

        // Stop Action
        val stopActionIntent = Intent(this, WorkoutLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            2,
            stopActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = formatDuration(state.durationSeconds)
        val distanceKm = String.format("%.2f km", state.distanceMeters / 1000.0)
        val paceFormatted = formatPace(state.currentPaceSecPerKm) + "/km"
        val hrFormatted = if (state.currentHeartRate > 0) "${state.currentHeartRate} bpm" else "-- bpm"
        val hrSourceText = if (state.hrSource.contains("BLE") || state.hrSource.contains("Smartwatch")) " [${state.hrSource}]" else ""

        val statusPrefix = if (state.isPaused) "[PAUSED] " else "● LIVE: "
        val title = "$statusPrefix${state.workoutType.title} • $durationFormatted"
        val text = "📍 $distanceKm  |  ⚡ $paceFormatted  |  ❤️ $hrFormatted (${state.currentZone.title})$hrSourceText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, toggleActionTitle, pendingToggle)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent foreground GPS location tracking during workouts"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Pranam::WorkoutTrackingWakeLock"
            )?.apply {
                acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) { }
        wakeLock = null
    }

    private fun stopForegroundTracking() {
        updateJob?.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    private fun formatPace(paceSecPerKm: Int): String {
        if (paceSecPerKm <= 0 || paceSecPerKm > 1800) return "--:--"
        val mins = paceSecPerKm / 60
        val secs = paceSecPerKm % 60
        return String.format("%d:%02d", mins, secs)
    }
}
