package com.example.tracker

import android.annotation.SuppressLint
import android.content.Context
import com.example.model.*
import kotlinx.coroutines.flow.StateFlow

class SmartwatchSyncManager(private val context: Context) {

    val bleManager = BleHeartRateManager(context)

    val discoveredDevices: StateFlow<List<SmartwatchDevice>> = bleManager.discoveredDevices
    val connectedWatch: StateFlow<SmartwatchDevice?> = bleManager.connectedDevice
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val latestBlePayload: StateFlow<BleHeartRatePayload?> = bleManager.latestPayload

    fun setTelemetryListeners(
        onHeartRate: (Int, String) -> Unit,
        onCadence: (Int) -> Unit,
        onSteps: ((Int) -> Unit)? = null,
        onSpeed: ((Double) -> Unit)? = null,
        onHrv: ((Double) -> Unit)? = null
    ) {
        bleManager.setTelemetryListeners(
            onHeartRate = onHeartRate,
            onCadence = onCadence,
            onSteps = onSteps,
            onSpeed = onSpeed,
            onHrv = onHrv
        )
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        bleManager.startScan()
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        bleManager.stopScan()
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: SmartwatchDevice, autoReconnect: Boolean = true) {
        bleManager.connect(device, enableAutoReconnect = autoReconnect)
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(device: SmartwatchDevice) {
        bleManager.disconnect()
    }
}
