package com.example.tracker

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.example.model.GpsPoint
import com.example.model.HeartRateZone
import com.example.model.LiveBiometricState
import com.example.model.WorkoutSplit
import com.example.model.WorkoutType
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class FitnessTrackerEngine(private val context: Context) : SensorEventListener, LocationListener {
    companion object {
        @Volatile
        var instance: FitnessTrackerEngine? = null
            private set
    }

    init {
        instance = this
        fetchInitialDeviceLocation()
    }

    private val _state = MutableStateFlow(LiveBiometricState())
    val state: StateFlow<LiveBiometricState> = _state.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var locationCallback: LocationCallback? = null
    private var trackingScope: CoroutineScope? = null
    private var tickerJob: Job? = null

    // Tracking variables
    private var lastLocation: Location? = null
    private var startGpsLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var accumulatedDurationSeconds = 0L
    private var sessionStartTime = 0L
    private var isPaused = false

    // Location Address Strings
    private var startLocationAddress: String = "Locating start position..."
    private var endLocationAddress: String = "Tracking..."

    // Sensor step counters
    private var stepCountSinceStart = 0
    private var initialSensorSteps = -1
    private var lastStepTimestamp = 0L
    private var recentCadenceWindow = mutableListOf<Long>()

    // Biometrics calculation
    private var currentHr = 0
    private var maxHrRecorded = 0
    private var hrSum = 0L
    private var hrSampleCount = 0L
    private var currentCadence = 0
    private var cadenceSum = 0L
    private var cadenceSampleCount = 0L
    private var hrSourceDesc = "Standby"
    private var hasHardwareHrSensor = false

    private var currentElevation = 0.0
    private var startingElevation = 0.0
    private var totalElevationGain = 0.0

    private val zoneTimes = mutableMapOf(1 to 0L, 2 to 0L, 3 to 0L, 4 to 0L, 5 to 0L)
    private val routePoints = mutableListOf<GpsPoint>()
    private val splits = mutableListOf<WorkoutSplit>()
    private var splitStartDistance = 0.0
    private var splitStartDuration = 0L
    private var splitHrSum = 0L
    private var splitHrCount = 0

    // Simulation / Demo Mode (User toggleable)
    var isSimulationMode = false
    private var simAngle = 0.0
    private var simLat = 37.7749
    private var simLng = -122.4194

    @SuppressLint("MissingPermission")
    private fun fetchInitialDeviceLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    simLat = loc.latitude
                    simLng = loc.longitude
                    _state.value = _state.value.copy(
                        currentLat = loc.latitude,
                        currentLng = loc.longitude,
                        startLat = loc.latitude,
                        startLng = loc.longitude,
                        hasGpsFix = true
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        val addr = LocationAddressResolver.getAddressFromCoordinates(context, loc.latitude, loc.longitude)
                        startLocationAddress = addr
                        endLocationAddress = addr
                        _state.value = _state.value.copy(
                            startLocationName = addr,
                            endLocationName = addr
                        )
                    }
                }
            }
        } catch (e: Exception) { }
    }

    fun setSimulation(enabled: Boolean) {
        isSimulationMode = enabled
    }

    @SuppressLint("MissingPermission")
    fun startTracking(
        workoutType: WorkoutType,
        userWeightKg: Double = 72.0,
        userMaxHr: Int = 190,
        userRestingHr: Int = 54
    ) {
        if (_state.value.isTracking && !_state.value.isPaused) return

        if (isPaused) {
            isPaused = false
            _state.value = _state.value.copy(isPaused = false)
            startTicker(userWeightKg, userMaxHr, userRestingHr)
            return
        }

        // New Session Reset
        totalDistanceMeters = 0.0
        accumulatedDurationSeconds = 0L
        sessionStartTime = System.currentTimeMillis()
        isPaused = false
        stepCountSinceStart = 0
        initialSensorSteps = -1
        lastStepTimestamp = 0L
        recentCadenceWindow.clear()

        // Check if we have an active external watch or hardware sensor
        val initialHr = if (currentHr > 35) currentHr else (if (isSimulationMode) userRestingHr + 15 else 0)
        currentHr = initialHr
        maxHrRecorded = initialHr
        hrSum = if (initialHr > 0) initialHr.toLong() else 0L
        hrSampleCount = if (initialHr > 0) 1L else 0L
        currentCadence = if (isSimulationMode) workoutType.typicalCadence else 0
        cadenceSum = 0L
        cadenceSampleCount = 0L
        totalElevationGain = 0.0
        lastLocation = null
        startGpsLocation = null

        zoneTimes.clear()
        for (i in 1..5) zoneTimes[i] = 0L
        routePoints.clear()
        splits.clear()
        splitStartDistance = 0.0
        splitStartDuration = 0L
        splitHrSum = 0L
        splitHrCount = 0

        val initialZone = if (initialHr > 0) HeartRateZone.fromHeartRate(initialHr, userMaxHr) else HeartRateZone.ZONE_1

        _state.value = LiveBiometricState(
            isTracking = true,
            isPaused = false,
            workoutType = workoutType,
            startTime = sessionStartTime,
            currentHeartRate = currentHr,
            avgHeartRate = if (hrSampleCount > 0) (hrSum / hrSampleCount).toInt() else 0,
            maxHeartRate = maxHrRecorded,
            currentZone = initialZone,
            zoneTimesSeconds = zoneTimes.toMap(),
            routePoints = emptyList(),
            splits = emptyList(),
            startLocationName = startLocationAddress,
            endLocationName = "Active Session",
            startLat = simLat,
            startLng = simLng,
            currentLat = simLat,
            currentLng = simLng,
            hrSource = if (currentHr > 0) hrSourceDesc else (if (isSimulationMode) "AI Simulation Engine" else "Connecting Sensors..."),
            hasLiveHrSensor = currentHr > 0
        )

        registerSensors()
        startLocationUpdates()
        startTicker(userWeightKg, userMaxHr, userRestingHr)
    }

    fun pauseTracking() {
        if (!_state.value.isTracking || isPaused) return
        isPaused = true
        tickerJob?.cancel()
        _state.value = _state.value.copy(isPaused = true)
    }

    fun resumeTracking(userWeightKg: Double = 72.0, userMaxHr: Int = 190, userRestingHr: Int = 54) {
        if (!_state.value.isTracking || !isPaused) return
        isPaused = false
        _state.value = _state.value.copy(isPaused = false)
        startTicker(userWeightKg, userMaxHr, userRestingHr)
    }

    fun stopTracking(): LiveBiometricState {
        tickerJob?.cancel()
        unregisterSensors()
        stopLocationUpdates()

        val stride = getStrideLength(_state.value.workoutType)
        val calculatedSteps = if (stride > 0.0) (totalDistanceMeters / stride).toInt() else 0
        val effectiveSteps = if (stepCountSinceStart > 0) max(stepCountSinceStart, calculatedSteps) else calculatedSteps

        val finalState = _state.value.copy(
            isTracking = false,
            isPaused = false,
            routePoints = routePoints.toList(),
            distanceMeters = totalDistanceMeters,
            stepCount = effectiveSteps,
            durationSeconds = accumulatedDurationSeconds,
            startLocationName = startLocationAddress,
            endLocationName = endLocationAddress
        )
        _state.value = finalState
        return finalState
    }

    private fun startTicker(userWeightKg: Double, userMaxHr: Int, userRestingHr: Int) {
        tickerJob?.cancel()
        trackingScope?.cancel()
        trackingScope = CoroutineScope(Dispatchers.Default)

        tickerJob = trackingScope?.launch {
            while (isActive && _state.value.isTracking && !isPaused) {
                delay(1000L)
                accumulatedDurationSeconds++

                // Update Biometrics & Physics
                computeTick(userWeightKg, userMaxHr, userRestingHr)
            }
        }
    }

    fun getStrideLength(workout: WorkoutType, userHeightCm: Double = 178.0): Double {
        val heightRatio = (userHeightCm / 178.0).coerceIn(0.8, 1.3)
        return when (workout) {
            WorkoutType.RUNNING -> 0.82 * heightRatio
            WorkoutType.POWER_WALK -> 0.72 * heightRatio
            WorkoutType.HIKING -> 0.65 * heightRatio
            WorkoutType.TRAIL_RUN -> 0.78 * heightRatio
            WorkoutType.HIIT_SPRINT -> 0.96 * heightRatio
            WorkoutType.CYCLING -> 0.0 // Cycling doesn't count walking steps
            WorkoutType.ROWING -> 0.0
        }
    }

    private fun computeTick(userWeightKg: Double, userMaxHr: Int, userRestingHr: Int) {
        val workout = _state.value.workoutType
        val duration = accumulatedDurationSeconds

        // If in simulation mode OR explicit testing mode, generate realistic biomechanical movement
        if (isSimulationMode) {
            simulateStepPhysics(workout, duration, userMaxHr, userRestingHr)
        }

        val stride = getStrideLength(workout)
        val calculatedSteps = if (stride > 0.0) (totalDistanceMeters / stride).toInt() else 0
        val effectiveSteps = if (stepCountSinceStart > 0) max(stepCountSinceStart, calculatedSteps) else calculatedSteps

        // Zone updates (only if valid HR exists)
        val currentZone = if (currentHr > 0) HeartRateZone.fromHeartRate(currentHr, userMaxHr) else HeartRateZone.ZONE_1
        if (currentHr > 0) {
            val currentZoneSec = zoneTimes[currentZone.zoneNumber] ?: 0L
            zoneTimes[currentZone.zoneNumber] = currentZoneSec + 1L

            hrSum += currentHr
            hrSampleCount++
            maxHrRecorded = max(maxHrRecorded, currentHr)
        }
        val avgHr = if (hrSampleCount > 0) (hrSum / hrSampleCount).toInt() else 0

        // Average Cadence
        if (currentCadence > 0) {
            cadenceSum += currentCadence
            cadenceSampleCount++
        }
        val avgCadence = if (cadenceSampleCount > 0) (cadenceSum / cadenceSampleCount).toInt() else currentCadence

        // Speed & Pace calculation based on actual GPS distance or step cadence
        val currentSpeedMps = if (duration > 0) (totalDistanceMeters / duration).toFloat() else 0f
        val avgPaceSecPerKm = if (totalDistanceMeters > 30.0) {
            ((duration.toDouble() / (totalDistanceMeters / 1000.0))).toInt()
        } else {
            0
        }

        // Instantaneous pace based on recent speed
        val currentPaceSecPerKm = if (currentSpeedMps > 0.4f) {
            (1000f / currentSpeedMps).toInt()
        } else {
            avgPaceSecPerKm
        }

        // Calorie Burn: Standard scientific MET formula: Calories = (MET * 3.5 * weightKg / 200) * (durationSeconds / 60)
        val intensityMultiplier = when (currentZone) {
            HeartRateZone.ZONE_1 -> 0.85
            HeartRateZone.ZONE_2 -> 1.0
            HeartRateZone.ZONE_3 -> 1.15
            HeartRateZone.ZONE_4 -> 1.35
            HeartRateZone.ZONE_5 -> 1.60
        }
        val adjustedMet = workout.metValue * intensityMultiplier
        val calories = if (totalDistanceMeters > 5.0 || duration > 10) {
            ((adjustedMet * 3.5 * userWeightKg / 200.0) * (duration / 60.0)).toInt()
        } else 0

        // Stamina & Fatigue model
        val zone4_5_time = (zoneTimes[4] ?: 0L) + (zoneTimes[5] ?: 0L)
        val fatigue = min(100, ((duration / 60) * 1.0 + (zone4_5_time / 30) * 2.2).toInt())
        val stamina = max(5, 100 - fatigue)

        // Check for 1km splits
        val currentKm = (totalDistanceMeters / 1000.0).toInt()
        val splitIndex = splits.size + 1
        if (currentKm >= splitIndex && totalDistanceMeters - splitStartDistance >= 950.0) {
            val splitDuration = duration - splitStartDuration
            val splitAvgHr = if (splitHrCount > 0) (splitHrSum / splitHrCount).toInt() else currentHr
            val splitPace = splitDuration.toInt()
            splits.add(
                WorkoutSplit(
                    splitNumber = splitIndex,
                    durationSeconds = splitDuration,
                    avgPaceSecPerKm = splitPace,
                    avgHeartRate = splitAvgHr,
                    elevationDeltaMeters = 8.5
                )
            )
            splitStartDistance = totalDistanceMeters
            splitStartDuration = duration
            splitHrSum = 0L
            splitHrCount = 0
        } else {
            if (currentHr > 0) {
                splitHrSum += currentHr
                splitHrCount++
            }
        }

        val lastPt = routePoints.lastOrNull()

        _state.value = _state.value.copy(
            durationSeconds = duration,
            distanceMeters = totalDistanceMeters,
            stepCount = effectiveSteps,
            currentSpeedMps = currentSpeedMps,
            currentPaceSecPerKm = currentPaceSecPerKm,
            avgPaceSecPerKm = avgPaceSecPerKm,
            currentHeartRate = currentHr,
            avgHeartRate = avgHr,
            maxHeartRate = maxHrRecorded,
            currentCadenceSpm = currentCadence,
            avgCadenceSpm = avgCadence,
            currentElevationMeters = currentElevation,
            elevationGainMeters = totalElevationGain,
            caloriesBurned = calories,
            currentZone = currentZone,
            zoneTimesSeconds = zoneTimes.toMap(),
            routePoints = routePoints.toList(),
            currentLat = lastPt?.latitude ?: _state.value.currentLat,
            currentLng = lastPt?.longitude ?: _state.value.currentLng,
            fatigueScore = fatigue,
            staminaPercent = stamina,
            splits = splits.toList(),
            hrSource = if (currentHr > 0) hrSourceDesc else (if (isSimulationMode) "AI Simulation Engine" else "Awaiting Sensor..."),
            hasLiveHrSensor = currentHr > 0
        )
    }

    private fun simulateStepPhysics(workout: WorkoutType, duration: Long, userMaxHr: Int, userRestingHr: Int) {
        val baseSpeedMps = when (workout) {
            WorkoutType.RUNNING -> 3.2f // ~5:12 min/km
            WorkoutType.CYCLING -> 7.8f // ~28 km/h
            WorkoutType.TRAIL_RUN -> 2.7f
            WorkoutType.HIIT_SPRINT -> 4.5f
            WorkoutType.POWER_WALK -> 1.8f
            WorkoutType.HIKING -> 1.4f
            WorkoutType.ROWING -> 3.0f
        }

        // Add biomechanical micro-variance
        val speedVariation = sin(duration * 0.08).toFloat() * 0.4f
        val currentSpeed = max(0.8f, baseSpeedMps + speedVariation)
        totalDistanceMeters += currentSpeed

        // Cadence modeling
        val targetCadence = workout.typicalCadence
        val cadenceVariance = (sin(duration * 0.15) * 5).toInt()
        currentCadence = max(0, targetCadence + cadenceVariance)

        // Realistic Heart rate curve from user resting HR to target intensity
        val targetHrForIntensity = when (workout) {
            WorkoutType.HIIT_SPRINT -> (userMaxHr * 0.90).toInt()
            WorkoutType.RUNNING -> (userMaxHr * 0.78).toInt()
            WorkoutType.CYCLING -> (userMaxHr * 0.75).toInt()
            WorkoutType.TRAIL_RUN -> (userMaxHr * 0.82).toInt()
            WorkoutType.POWER_WALK -> (userMaxHr * 0.62).toInt()
            WorkoutType.HIKING -> (userMaxHr * 0.70).toInt()
            WorkoutType.ROWING -> (userMaxHr * 0.80).toInt()
        }

        val warmUpProgress = min(1.0, duration / 120.0)
        val baseHr = userRestingHr + ((targetHrForIntensity - userRestingHr) * warmUpProgress).toInt()
        val hrFluctuation = (sin(duration * 0.05) * 4).toInt() + (speedVariation * 6).toInt()
        currentHr = max(userRestingHr, min(userMaxHr, baseHr + hrFluctuation))
        hrSourceDesc = "AI Simulation Engine"

        // Elevation modeling
        val elevationDelta = (sin(duration * 0.03) * 0.6)
        currentElevation += elevationDelta
        if (elevationDelta > 0) totalElevationGain += elevationDelta

        // Simulated GPS Coordinate Trail (Curved athletic loop)
        simAngle += 0.02
        val radius = 0.0035 + (sin(simAngle * 0.5) * 0.001)
        val lat = simLat + radius * cos(simAngle)
        val lng = simLng + radius * sin(simAngle)

        if (routePoints.isEmpty() || duration % 2 == 0L) {
            routePoints.add(
                GpsPoint(
                    latitude = lat,
                    longitude = lng,
                    altitude = currentElevation,
                    speedMps = currentSpeed,
                    timestamp = System.currentTimeMillis(),
                    heartRate = currentHr,
                    cadence = currentCadence
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _state.value.isTracking && !_state.value.isPaused) {
                    processRealLocation(loc)
                }
            }
        } catch (e: Exception) { }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).setMinUpdateDistanceMeters(0.5f)
             .setMinUpdateIntervalMillis(500L)
             .setMaxUpdateDelayMillis(1500L)
             .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val locations = result.locations
                    if (locations.isNotEmpty()) {
                        for (loc in locations) {
                            processRealLocation(loc)
                        }
                    } else {
                        result.lastLocation?.let { processRealLocation(it) }
                    }
                }
            }

            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    it,
                    Looper.getMainLooper()
                )
            }
            _state.value = _state.value.copy(isGpsActive = true)
        } catch (e: Exception) {
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1.0f,
                    this,
                    Looper.getMainLooper()
                )
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2.0f,
                    this,
                    Looper.getMainLooper()
                )
                _state.value = _state.value.copy(isGpsActive = true)
            } catch (ex: Exception) { }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) { }
        _state.value = _state.value.copy(isGpsActive = false, hasGpsFix = false)
    }

    private fun processRealLocation(location: Location) {
        if (!_state.value.isTracking || isPaused) return

        // Filter high accuracy GPS fixes (ignore fixes with horizontal accuracy > 35m)
        val hasAccuracy = location.hasAccuracy()
        val accuracy = if (hasAccuracy) location.accuracy else 20.0f
        if (hasAccuracy && accuracy > 35.0f && lastLocation != null) {
            // Low accuracy satellite bounce - skip point addition to prevent erratic jitter
            return
        }

        val lastLoc = lastLocation
        var distToAdd = 0.0
        if (lastLoc != null) {
            val dist = lastLoc.distanceTo(location).toDouble()
            // Accept movement updates if distance > 0.5m
            if (dist > 0.5) {
                distToAdd = dist
                totalDistanceMeters += dist
                if (location.hasAltitude() && lastLoc.hasAltitude()) {
                    val altDelta = location.altitude - lastLoc.altitude
                    if (altDelta > 0) totalElevationGain += altDelta
                    currentElevation = location.altitude
                }
            }
        } else {
            startGpsLocation = location
            startingElevation = location.altitude
            currentElevation = location.altitude
            simLat = location.latitude
            simLng = location.longitude

            // Resolve initial start location address
            CoroutineScope(Dispatchers.IO).launch {
                val addr = LocationAddressResolver.getAddressFromCoordinates(context, location.latitude, location.longitude)
                startLocationAddress = addr
                _state.value = _state.value.copy(
                    startLocationName = addr,
                    startLat = location.latitude,
                    startLng = location.longitude
                )
            }
        }

        lastLocation = location

        val speedMps = if (location.hasSpeed() && location.speed > 0f) {
            location.speed
        } else if (distToAdd > 0) {
            (distToAdd / 1.0).toFloat()
        } else {
            0f
        }

        routePoints.add(
            GpsPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speedMps = speedMps,
                timestamp = if (location.time > 0) location.time else System.currentTimeMillis(),
                heartRate = currentHr,
                cadence = currentCadence
            )
        )

        val stride = getStrideLength(_state.value.workoutType)
        val calculatedSteps = if (stride > 0.0) (totalDistanceMeters / stride).toInt() else 0
        val effectiveSteps = if (stepCountSinceStart > 0) max(stepCountSinceStart, calculatedSteps) else calculatedSteps

        _state.value = _state.value.copy(
            hasGpsFix = true,
            currentLat = location.latitude,
            currentLng = location.longitude,
            distanceMeters = totalDistanceMeters,
            stepCount = effectiveSteps,
            elevationGainMeters = totalElevationGain,
            currentElevationMeters = currentElevation,
            routePoints = routePoints.toList()
        )

        // Periodically refresh reverse geocoded end location
        if (distToAdd > 10.0 || (endLocationAddress.startsWith("Tracking") && totalDistanceMeters > 5.0)) {
            CoroutineScope(Dispatchers.IO).launch {
                val addr = LocationAddressResolver.getAddressFromCoordinates(context, location.latitude, location.longitude)
                endLocationAddress = addr
                _state.value = _state.value.copy(endLocationName = addr)
            }
        }
    }

    private fun registerSensors() {
        try {
            val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor != null) {
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            }

            val hrSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            if (hrSensor != null) {
                hasHardwareHrSensor = true
                sensorManager?.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) { }
    }

    private fun unregisterSensors() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) { }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_state.value.isTracking || isPaused) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                val now = SystemClock.elapsedRealtime()
                stepCountSinceStart++
                if (lastStepTimestamp > 0) {
                    val deltaMs = now - lastStepTimestamp
                    if (deltaMs > 200) {
                        recentCadenceWindow.add(now)
                        recentCadenceWindow.removeAll { now - it > 5000L }
                        currentCadence = (recentCadenceWindow.size * 12).coerceIn(40, 240)
                    }
                }
                lastStepTimestamp = now
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val rawSteps = event.values.firstOrNull()?.toInt() ?: 0
                if (initialSensorSteps < 0) {
                    initialSensorSteps = rawSteps
                }
                stepCountSinceStart = rawSteps - initialSensorSteps
            }
            Sensor.TYPE_HEART_RATE -> {
                val hrVal = event.values.firstOrNull()?.toInt() ?: 0
                if (hrVal in 35..230) {
                    currentHr = hrVal
                    hrSourceDesc = "Phone Optical Sensor"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        processRealLocation(location)
    }

    fun setLiveAiTip(tip: String) {
        _state.value = _state.value.copy(
            liveAiTip = tip,
            liveAiTipTimestamp = System.currentTimeMillis(),
            isAiAnalyzing = false
        )
    }

    fun setAiAnalyzing(isAnalyzing: Boolean) {
        _state.value = _state.value.copy(isAiAnalyzing = isAnalyzing)
    }

    fun setExternalBiometrics(
        heartRate: Int?,
        cadence: Int?,
        steps: Int? = null,
        source: String = "Smartwatch BLE"
    ) {
        if (heartRate != null && heartRate in 35..230) {
            currentHr = heartRate
            hrSourceDesc = source
        }
        if (cadence != null && cadence in 30..260) {
            currentCadence = cadence
        }
        if (steps != null && steps > 0) {
            stepCountSinceStart = kotlin.math.max(stepCountSinceStart, steps)
            _state.value = _state.value.copy(
                stepCount = kotlin.math.max(_state.value.stepCount, steps)
            )
        }
    }
}
