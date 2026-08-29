package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

enum class SmartwatchBrand(
    val brandName: String,
    val defaultIcon: ImageVector,
    val accentColor: Color,
    val description: String
) {
    WEAR_OS("WearOS / Galaxy Watch / Pixel", Icons.Default.Watch, NeonCyan, "Standard WearOS companion & direct Bluetooth GATT bridge"),
    GARMIN("Garmin Connect IQ", Icons.Default.Watch, NeonGreen, "Garmin Forerunner, Fenix, Venu, Epix standard BLE telemetry"),
    APPLE_WATCH("Apple Watch Bridge", Icons.Default.Watch, Color(0xFFFFFFFF), "Apple Watch Series & Ultra wireless workout sync bridge"),
    POLAR_WAHOO("Polar / Wahoo / Scosche / Whoop", Icons.Default.Favorite, NeonCoral, "Precision optical armbands & chest strap ECG monitors"),
    COROS_SUUNTO("Coros / Suunto", Icons.Default.Speed, NeonAmber, "Endurance sports watches with cadence & barometer metrics"),
    FITBIT_XIAOMI("Fitbit / Xiaomi / Amazfit / Huawei", Icons.AutoMirrored.Filled.DirectionsWalk, NeonGreen, "Smart bands & activity trackers with continuous step & BPM telemetry"),
    GENERIC_BLE("Universal BLE Sensor & Watch", Icons.Default.Bluetooth, SpeedColor, "Standard Bluetooth SIG Heart Rate & RSC profile (0x180D/0x1814)")
}

enum class WatchConnectionStatus(val label: String, val color: Color) {
    DISCONNECTED("Disconnected", TextMuted),
    SCANNING("Scanning...", NeonCyan),
    CONNECTING("Pairing...", NeonAmber),
    CONNECTED("Connected • Live Telemetry", NeonGreen),
    SYNCING("Syncing Data...", NeonCyan),
    FAILED("Connection Failed", NeonCoral)
}

data class SmartwatchDevice(
    val id: String,
    val name: String,
    val brand: SmartwatchBrand,
    val macAddress: String,
    val isConnected: Boolean = false,
    val status: WatchConnectionStatus = WatchConnectionStatus.DISCONNECTED,
    val batteryPercent: Int = 88,
    val signalRssiDbm: Int = -55,
    val liveHeartRateBpm: Int? = null,
    val liveCadenceSpm: Int? = null,
    val liveStepCount: Int? = null,
    val liveSpeedMps: Double? = null,
    val firmwareVersion: String = "v3.4.12",
    val isVirtual: Boolean = false
)

data class WatchSyncResult(
    val success: Boolean,
    val syncedSteps: Int,
    val syncedCalories: Int,
    val avgHeartRate: Int,
    val lastSyncTimestamp: Long
)
