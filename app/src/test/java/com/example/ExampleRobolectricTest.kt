package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.tracker.TrendsCalculator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pranam", appName)
    }

    @Test
    fun `test HeartRateZone calculation`() {
        val maxHr = 190
        val zone1 = HeartRateZone.fromHeartRate(100, maxHr)
        val zone2 = HeartRateZone.fromHeartRate(120, maxHr)
        val zone3 = HeartRateZone.fromHeartRate(140, maxHr)
        val zone4 = HeartRateZone.fromHeartRate(160, maxHr)
        val zone5 = HeartRateZone.fromHeartRate(180, maxHr)

        assertEquals(HeartRateZone.ZONE_1, zone1)
        assertEquals(HeartRateZone.ZONE_2, zone2)
        assertEquals(HeartRateZone.ZONE_3, zone3)
        assertEquals(HeartRateZone.ZONE_4, zone4)
        assertEquals(HeartRateZone.ZONE_5, zone5)
    }

    @Test
    fun `test HRV Recovery status from RMSSD`() {
        assertEquals(HrvRecoveryStatus.OPTIMAL_PRIMED, HrvRecoveryStatus.fromRmssd(75))
        assertEquals(HrvRecoveryStatus.BALANCED_READY, HrvRecoveryStatus.fromRmssd(60))
        assertEquals(HrvRecoveryStatus.MODERATE_FATIGUE, HrvRecoveryStatus.fromRmssd(50))
        assertEquals(HrvRecoveryStatus.STRAINED_RECOVERY, HrvRecoveryStatus.fromRmssd(35))
    }

    @Test
    fun `test TrendsCalculator generates non-empty trends`() {
        val userProfile = UserProfileEntity(targetWeeklyKm = 40.0)
        val sampleSessions = listOf(
            WorkoutSessionEntity(
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() - 86400000L + 1800000L,
                workoutType = WorkoutType.RUNNING.name,
                durationSeconds = 1800L,
                distanceMeters = 5000.0,
                caloriesBurned = 350,
                avgHeartRate = 152,
                maxHeartRate = 175,
                avgPaceSecPerKm = 360
            )
        )

        val trends = TrendsCalculator.generateTrends(sampleSessions, TrendsTimeframe.WEEK_7_DAYS, userProfile)
        assertEquals(7, trends.size)

        val summary = TrendsCalculator.computeSummary(trends, userProfile)
        assertTrue(summary.totalDistanceKm >= 5.0)
        assertTrue(summary.totalCalories >= 350)
        assertEquals(40.0, summary.targetWeeklyKm, 0.01)
    }

    @Test
    fun `test AppThemeMode switching`() {
        val darkTheme = AppThemeMode.CYBER_BLACK
        val lightTheme = AppThemeMode.HIGH_TECH_WHITE
        assertNotEquals(darkTheme, lightTheme)
    }

    @Test
    fun `test MapTileType entries`() {
        val street = com.example.ui.components.MapTileType.STREETS
        val dark = com.example.ui.components.MapTileType.DARK
        val satellite = com.example.ui.components.MapTileType.SATELLITE
        assertEquals("Google Streets", street.title)
        assertEquals("Cyber Dark", dark.title)
        assertEquals("Satellite Hybrid", satellite.title)
    }

    @Test
    fun `test BlePermissionHelper required permissions list`() {
        val permissions = com.example.tracker.BlePermissionHelper.getRequiredPermissions()
        assertTrue(permissions.contains(android.Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(permissions.isNotEmpty())
    }

    @Test
    fun `test BleHeartRatePayload model`() {
        val payload = com.example.tracker.BleHeartRatePayload(
            heartRateBpm = 145,
            isSensorContactSupported = true,
            isSensorInContact = true,
            rrIntervalsMs = listOf(412, 415, 410),
            hrvRmssdMs = 42.5,
            batteryPercent = 90,
            deviceName = "Polar H10"
        )
        assertEquals(145, payload.heartRateBpm)
        assertTrue(payload.isSensorInContact)
        assertEquals(90, payload.batteryPercent)
        assertEquals("Polar H10", payload.deviceName)
        assertEquals(42.5, payload.hrvRmssdMs!!, 0.01)
    }

    @Test
    fun `test WorkoutSessionEntity location and route points preservation`() {
        val session = WorkoutSessionEntity(
            startTime = 1700000000000L,
            endTime = 1700001800000L,
            workoutType = WorkoutType.RUNNING.name,
            durationSeconds = 1800L,
            distanceMeters = 5200.0,
            caloriesBurned = 410,
            avgHeartRate = 155,
            maxHeartRate = 178,
            avgPaceSecPerKm = 346,
            startLocationName = "Golden Gate Park, San Francisco",
            endLocationName = "Ocean Beach, San Francisco",
            startLat = 37.7694,
            startLng = -122.4862,
            endLat = 37.7594,
            endLng = -122.5107,
            routePointsJson = """[{"lat":37.7694,"lng":-122.4862,"spd":3.2,"hr":140},{"lat":37.7594,"lng":-122.5107,"spd":3.4,"hr":155}]"""
        )

        assertEquals("Golden Gate Park, San Francisco", session.startLocationName)
        assertEquals("Ocean Beach, San Francisco", session.endLocationName)
        assertEquals(37.7694, session.startLat, 0.0001)
        assertEquals(-122.5107, session.endLng, 0.0001)
        assertTrue(session.routePointsJson.contains("37.7694"))
    }
}

