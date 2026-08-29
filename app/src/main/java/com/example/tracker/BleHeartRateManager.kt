package com.example.tracker

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.model.SmartwatchBrand
import com.example.model.SmartwatchDevice
import com.example.model.WatchConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Real-time biometric payload received from a BLE Heart Rate Monitor or Wearable Device
 */
data class BleHeartRatePayload(
    val heartRateBpm: Int,
    val isSensorContactSupported: Boolean = false,
    val isSensorInContact: Boolean = false,
    val energyExpendedKcal: Int? = null,
    val rrIntervalsMs: List<Int> = emptyList(),
    val hrvRmssdMs: Double? = null,
    val cadenceSpm: Int? = null,
    val stepCount: Int? = null,
    val instantaneousSpeedMps: Double? = null,
    val batteryPercent: Int? = null,
    val deviceName: String = "BLE Wearable Device",
    val macAddress: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dedicated Bluetooth Low Energy GATT Client for Heart Rate Monitors, Smartwatches, and Wearable Fitness Trackers.
 * Extracts live Heart Rate (BPM), Step Count, Cadence (SPM), Speed, and HRV from standard Bluetooth SIG profiles.
 */
class BleHeartRateManager(private val context: Context) {

    companion object {
        private const val TAG = "BleHeartRateManager"

        // Bluetooth SIG standard Service & Characteristic UUIDs
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val HEART_RATE_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        val BODY_SENSOR_LOCATION_CHAR_UUID: UUID = UUID.fromString("00002A38-0000-1000-8000-00805F9B34FB")

        val RSC_SERVICE_UUID: UUID = UUID.fromString("00001814-0000-1000-8000-00805F9B34FB")
        val RSC_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A53-0000-1000-8000-00805F9B34FB")

        val CSC_SERVICE_UUID: UUID = UUID.fromString("00001816-0000-1000-8000-00805F9B34FB")
        val CSC_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805F9B34FB")

        val FITNESS_MACHINE_SERVICE_UUID: UUID = UUID.fromString("00001826-0000-1000-8000-00805F9B34FB")
        val TREADMILL_DATA_CHAR_UUID: UUID = UUID.fromString("00002ACD-0000-1000-8000-00805F9B34FB")
        val STEP_CLIMBER_CHAR_UUID: UUID = UUID.fromString("00002ACF-0000-1000-8000-00805F9B34FB")
        val INDOOR_BIKE_CHAR_UUID: UUID = UUID.fromString("00002AD2-0000-1000-8000-00805F9B34FB")

        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        // Wearable Step & Activity UUIDs
        val MIBAND_SERVICE_UUID: UUID = UUID.fromString("0000FEE0-0000-1000-8000-00805F9B34FB")
        val MIBAND_STEPS_CHAR_UUID: UUID = UUID.fromString("00000007-0000-3512-2118-0009AF100700")
        val MIBAND_STEPS_CHAR_UUID_2: UUID = UUID.fromString("0000FF06-0000-1000-8000-00805F9B34FB")
        val GENERIC_STEP_CHAR_UUID: UUID = UUID.fromString("00002AF0-0000-1000-8000-00805F9B34FB")
        val GENERIC_STEP_CHAR_UUID_2: UUID = UUID.fromString("00002B40-0000-1000-8000-00805F9B34FB")
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _discoveredDevices = MutableStateFlow<List<SmartwatchDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<SmartwatchDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<SmartwatchDevice?>(null)
    val connectedDevice: StateFlow<SmartwatchDevice?> = _connectedDevice.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _latestPayload = MutableStateFlow<BleHeartRatePayload?>(null)
    val latestPayload: StateFlow<BleHeartRatePayload?> = _latestPayload.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var lastConnectedMac: String? = null
    private var autoReconnectEnabled = false
    private var reconnectAttempts = 0

    // Cumulative step counters from wearable BLE RSC / Steps stream
    private var accumulatedWearableSteps = 0
    private var lastCadenceTimestamp = 0L
    private var lastRscDistanceMeters = -1.0

    // RR Intervals history for rolling HRV (RMSSD)
    private val recentRrIntervals = Collections.synchronizedList(mutableListOf<Int>())

    // Telemetry callback listeners
    private var onHeartRateListener: ((Int, String) -> Unit)? = null
    private var onCadenceListener: ((Int) -> Unit)? = null
    private var onStepsListener: ((Int) -> Unit)? = null
    private var onSpeedListener: ((Double) -> Unit)? = null
    private var onHrvCalculatedListener: ((Double) -> Unit)? = null

    fun setTelemetryListeners(
        onHeartRate: (Int, String) -> Unit,
        onCadence: (Int) -> Unit,
        onSteps: ((Int) -> Unit)? = null,
        onSpeed: ((Double) -> Unit)? = null,
        onHrv: ((Double) -> Unit)? = null
    ) {
        this.onHeartRateListener = onHeartRate
        this.onCadenceListener = onCadence
        this.onStepsListener = onSteps
        this.onSpeedListener = onSpeed
        this.onHrvCalculatedListener = onHrv
    }

    /**
     * Start BLE Scan for Smartwatches, Wearables, Heart Rate Straps, and Fitness Trackers
     */
    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = 8000L) {
        if (_isScanning.value) return
        if (!BlePermissionHelper.hasBluetoothPermissions(context)) {
            Log.w(TAG, "Cannot start BLE scan: Missing Bluetooth permissions")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Cannot start BLE scan: Bluetooth adapter is disabled or null")
            return
        }

        _isScanning.value = true

        coroutineScope.launch {
            try {
                val scanner = adapter.bluetoothLeScanner
                if (scanner == null) {
                    _isScanning.value = false
                    return@launch
                }

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { handleScanResult(it) }
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                        results?.forEach { handleScanResult(it) }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "BLE Scan failed with errorCode: $errorCode")
                        _isScanning.value = false
                    }
                }

                // Scan for all nearby BLE devices to ensure all watches, fitness bands, and chest straps are found
                try {
                    scanner.startScan(null, settings, scanCallback)
                } catch (e: Exception) {
                    scanner.startScan(scanCallback)
                }

                delay(timeoutMs)

                try {
                    scanner.stopScan(scanCallback)
                } catch (e: Exception) { }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during BLE scan: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val rawName: String? = try { device.name } catch (e: Exception) { null }
        val address = device.address ?: return
        val rssi = result.rssi

        // Accept device if named or has valid wearable/HR service UUIDs in scan record
        val scanRecord = result.scanRecord
        val serviceUuids = scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
        val isLikelyWearable = serviceUuids.contains(HEART_RATE_SERVICE_UUID) ||
                serviceUuids.contains(RSC_SERVICE_UUID) ||
                serviceUuids.contains(CSC_SERVICE_UUID) ||
                serviceUuids.contains(FITNESS_MACHINE_SERVICE_UUID) ||
                serviceUuids.contains(BATTERY_SERVICE_UUID) ||
                !rawName.isNullOrEmpty()

        if (!isLikelyWearable && rawName.isNullOrEmpty()) return

        val name = rawName ?: "Bluetooth Wearable Device"
        val brand = guessBrand(name)
        val devItem = SmartwatchDevice(
            id = "ble_${address.replace(":", "")}",
            name = name,
            brand = brand,
            macAddress = address,
            signalRssiDbm = rssi,
            batteryPercent = 90,
            isVirtual = false,
            isConnected = _connectedDevice.value?.macAddress.equals(address, ignoreCase = true)
        )

        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.macAddress.equals(address, ignoreCase = true) }
        if (index >= 0) {
            current[index] = current[index].copy(signalRssiDbm = rssi, name = name)
        } else {
            current.add(0, devItem)
        }
        _discoveredDevices.value = current
    }

    /**
     * Connect to BLE Wearable Device / Smartwatch
     */
    @SuppressLint("MissingPermission")
    fun connect(device: SmartwatchDevice, enableAutoReconnect: Boolean = true) {
        if (!BlePermissionHelper.hasBluetoothPermissions(context)) {
            Log.w(TAG, "Cannot connect to BLE device: Missing permissions")
            return
        }

        lastConnectedMac = device.macAddress
        autoReconnectEnabled = enableAutoReconnect
        reconnectAttempts = 0

        coroutineScope.launch {
            // Disconnect existing GATT if any
            disconnectCurrentGatt()

            val connectingDev = device.copy(status = WatchConnectionStatus.CONNECTING)
            updateDeviceState(connectingDev)
            _connectedDevice.value = connectingDev

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter == null || !BluetoothAdapter.checkBluetoothAddress(device.macAddress)) {
                Log.e(TAG, "Invalid Bluetooth address or adapter unavailable: ${device.macAddress}")
                markDisconnected(device)
                return@launch
            }

            val remoteDevice = adapter.getRemoteDevice(device.macAddress)
            if (remoteDevice == null) {
                markDisconnected(device)
                return@launch
            }

            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                remoteDevice.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                remoteDevice.connectGatt(context, false, gattCallback)
            }
        }
    }

    /**
     * Disconnect active BLE Wearable Device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        autoReconnectEnabled = false
        disconnectCurrentGatt()
        _connectedDevice.value?.let { markDisconnected(it) }
        _connectedDevice.value = null
        recentRrIntervals.clear()
        accumulatedWearableSteps = 0
        lastRscDistanceMeters = -1.0
    }

    @SuppressLint("MissingPermission")
    private fun disconnectCurrentGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT: ${e.message}")
        }
        activeGatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            val device = _connectedDevice.value

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnectAttempts = 0
                mainHandler.post {
                    val connectedDev = (device ?: SmartwatchDevice(
                        id = "ble_${gatt.device.address.replace(":", "")}",
                        name = try { gatt.device.name } catch (e: Exception) { "BLE Wearable" } ?: "BLE Wearable",
                        brand = guessBrand(try { gatt.device.name ?: "" } catch (e: Exception) { "" }),
                        macAddress = gatt.device.address
                    )).copy(
                        isConnected = true,
                        status = WatchConnectionStatus.CONNECTED
                    )
                    updateDeviceState(connectedDev)
                    _connectedDevice.value = connectedDev
                }
                // Discover all GATT services
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mainHandler.post {
                    device?.let { markDisconnected(it) }
                }
                // Handle Auto-reconnect if active workout session is ongoing
                if (autoReconnectEnabled && reconnectAttempts < 5) {
                    reconnectAttempts++
                    Log.d(TAG, "Attempting auto-reconnect #$reconnectAttempts in 2s...")
                    coroutineScope.launch {
                        delay(2000L * reconnectAttempts)
                        device?.let { connect(it, true) }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onServicesDiscovered failed with status: $status")
                return
            }

            coroutineScope.launch {
                val charsToEnable = mutableListOf<BluetoothGattCharacteristic>()

                gatt.services.forEach { service ->
                    service.characteristics.forEach { char ->
                        val uuid = char.uuid
                        if (uuid == HEART_RATE_MEASUREMENT_CHAR_UUID ||
                            uuid == RSC_MEASUREMENT_CHAR_UUID ||
                            uuid == CSC_MEASUREMENT_CHAR_UUID ||
                            uuid == TREADMILL_DATA_CHAR_UUID ||
                            uuid == STEP_CLIMBER_CHAR_UUID ||
                            uuid == INDOOR_BIKE_CHAR_UUID ||
                            uuid == MIBAND_STEPS_CHAR_UUID ||
                            uuid == MIBAND_STEPS_CHAR_UUID_2 ||
                            uuid == GENERIC_STEP_CHAR_UUID ||
                            uuid == GENERIC_STEP_CHAR_UUID_2
                        ) {
                            charsToEnable.add(char)
                        } else if (uuid == BATTERY_LEVEL_CHAR_UUID) {
                            gatt.readCharacteristic(char)
                        }
                    }
                }

                // Sequential notification descriptor registration to prevent GATT write conflicts
                charsToEnable.forEach { char ->
                    try {
                        gatt.setCharacteristicNotification(char, true)
                        val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                            delay(100L)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed enabling notifications on char ${char.uuid}: ${e.message}")
                    }
                }
            }
        }

        // Standard Android 13+ Characteristic Changed
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseCharacteristicData(gatt, characteristic.uuid, value)
        }

        // Legacy Characteristic Changed
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: return
            parseCharacteristicData(gatt, characteristic.uuid, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: return
                mainHandler.post {
                    val dev = _connectedDevice.value?.copy(batteryPercent = batteryLevel)
                    if (dev != null) {
                        updateDeviceState(dev)
                        _connectedDevice.value = dev
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mainHandler.post {
                    val dev = _connectedDevice.value?.copy(signalRssiDbm = rssi)
                    if (dev != null) {
                        _connectedDevice.value = dev
                    }
                }
            }
        }
    }

    /**
     * Parses standard Bluetooth SIG GATT Byte Arrays and Custom Wearable Step Payloads
     */
    private fun parseCharacteristicData(gatt: BluetoothGatt, uuid: UUID, data: ByteArray) {
        if (data.isEmpty()) return

        when (uuid) {
            HEART_RATE_MEASUREMENT_CHAR_UUID -> {
                parseHeartRateMeasurement(gatt, data)
            }
            RSC_MEASUREMENT_CHAR_UUID -> {
                parseRscMeasurement(data)
            }
            STEP_CLIMBER_CHAR_UUID, TREADMILL_DATA_CHAR_UUID, INDOOR_BIKE_CHAR_UUID -> {
                parseFitnessMachineData(data)
            }
            MIBAND_STEPS_CHAR_UUID, MIBAND_STEPS_CHAR_UUID_2, GENERIC_STEP_CHAR_UUID, GENERIC_STEP_CHAR_UUID_2 -> {
                parseDirectStepCount(data)
            }
            BATTERY_LEVEL_CHAR_UUID -> {
                val battery = data[0].toInt() and 0xFF
                mainHandler.post {
                    val dev = _connectedDevice.value?.copy(batteryPercent = battery)
                    if (dev != null) {
                        updateDeviceState(dev)
                        _connectedDevice.value = dev
                    }
                }
            }
        }
    }

    /**
     * Parses Standard Heart Rate Measurement (0x2A37)
     * Format:
     * - Flags (1 byte):
     *   - Bit 0: HR Format (0 = UINT8, 1 = UINT16)
     *   - Bit 1-2: Sensor Contact Status
     *   - Bit 3: Energy Expended Present
     *   - Bit 4: RR-Intervals Present
     */
    private fun parseHeartRateMeasurement(gatt: BluetoothGatt, data: ByteArray) {
        val flags = data[0].toInt()
        val is16Bit = (flags and 0x01) != 0
        var offset = 1

        val hrVal: Int = if (is16Bit && data.size >= offset + 2) {
            val v = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
            v
        } else if (data.size >= offset + 1) {
            val v = data[offset].toInt() and 0xFF
            offset += 1
            v
        } else return

        // Sensor Contact Status
        val contactStatusBits = (flags and 0x06) shr 1
        val contactSupported = contactStatusBits == 2 || contactStatusBits == 3
        val inContact = contactStatusBits == 3

        // Energy Expended (kJ / kcal)
        var energyExpendedKcal: Int? = null
        if ((flags and 0x08) != 0 && data.size >= offset + 2) {
            energyExpendedKcal = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        }

        // RR-Intervals (1/1024s resolution -> ms)
        val rrList = mutableListOf<Int>()
        if ((flags and 0x10) != 0) {
            while (offset + 1 < data.size) {
                val rawRr = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                val rrMs = (rawRr * 1000) / 1024
                if (rrMs in 250..2200) {
                    rrList.add(rrMs)
                    recentRrIntervals.add(rrMs)
                    if (recentRrIntervals.size > 40) recentRrIntervals.removeAt(0)
                }
                offset += 2
            }
        }

        // Calculate HRV RMSSD (Root Mean Square of Successive Differences)
        val hrv = calculateRmssd(recentRrIntervals)

        val devName = try { gatt.device.name } catch (e: Exception) { null } ?: "BLE Wearable"
        val payload = BleHeartRatePayload(
            heartRateBpm = hrVal,
            isSensorContactSupported = contactSupported,
            isSensorInContact = inContact,
            energyExpendedKcal = energyExpendedKcal,
            rrIntervalsMs = rrList,
            hrvRmssdMs = hrv,
            stepCount = accumulatedWearableSteps.takeIf { it > 0 },
            deviceName = devName,
            macAddress = gatt.device.address
        )

        mainHandler.post {
            _latestPayload.value = payload
            val dev = _connectedDevice.value?.copy(liveHeartRateBpm = hrVal)
            if (dev != null) {
                _connectedDevice.value = dev
            }
            onHeartRateListener?.invoke(hrVal, "$devName (BLE)")
            if (hrv != null) {
                onHrvCalculatedListener?.invoke(hrv)
            }
        }
    }

    /**
     * Parses Running Speed and Cadence (0x2A53) & Computes Step Counts
     * Flags:
     * - Bit 0: Instantaneous Stride Length Present
     * - Bit 1: Total Distance Present
     * - Bit 2: Walking or Running Status (0 = Walking, 1 = Running)
     */
    private fun parseRscMeasurement(data: ByteArray) {
        if (data.size < 4) return
        val flags = data[0].toInt()
        val speedRaw = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        val speedMps = speedRaw / 256.0
        val cadence = data[3].toInt() and 0xFF

        var offset = 4
        var strideLengthCm: Int? = null
        if ((flags and 0x01) != 0 && data.size >= offset + 2) {
            strideLengthCm = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        }

        var totalDistMeters: Double? = null
        if ((flags and 0x02) != 0 && data.size >= offset + 4) {
            val rawDistDecimeters = (data[offset].toLong() and 0xFF) or
                    ((data[offset + 1].toLong() and 0xFF) shl 8) or
                    ((data[offset + 2].toLong() and 0xFF) shl 16) or
                    ((data[offset + 3].toLong() and 0xFF) shl 24)
            totalDistMeters = rawDistDecimeters / 10.0
        }

        // Calculate cumulative steps from distance or cadence
        val now = System.currentTimeMillis()
        if (totalDistMeters != null && totalDistMeters > 0.0) {
            val stride = (strideLengthCm?.toDouble() ?: 78.0) / 100.0
            accumulatedWearableSteps = max(accumulatedWearableSteps, (totalDistMeters / stride).toInt())
        } else if (cadence > 0) {
            if (lastCadenceTimestamp > 0L) {
                val dtSec = (now - lastCadenceTimestamp) / 1000.0
                if (dtSec in 0.5..5.0) {
                    val stepIncrement = ((cadence / 60.0) * dtSec).toInt()
                    if (stepIncrement > 0) {
                        accumulatedWearableSteps += stepIncrement
                    }
                }
            }
            lastCadenceTimestamp = now
        }

        val steps = accumulatedWearableSteps

        mainHandler.post {
            val dev = _connectedDevice.value?.copy(
                liveCadenceSpm = cadence,
                liveStepCount = steps.takeIf { it > 0 },
                liveSpeedMps = speedMps
            )
            if (dev != null) {
                _connectedDevice.value = dev
            }
            onCadenceListener?.invoke(cadence)
            onSpeedListener?.invoke(speedMps)
            if (steps > 0) {
                onStepsListener?.invoke(steps)
            }
        }
    }

    /**
     * Parses Fitness Machine Treadmill & Step Climber Data (0x2ACD, 0x2ACF)
     */
    private fun parseFitnessMachineData(data: ByteArray) {
        if (data.size < 4) return
        var steps = 0
        if (data.size >= 6) {
            steps = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        }
        if (steps > 0) {
            accumulatedWearableSteps = max(accumulatedWearableSteps, steps)
            mainHandler.post {
                val dev = _connectedDevice.value?.copy(liveStepCount = accumulatedWearableSteps)
                if (dev != null) _connectedDevice.value = dev
                onStepsListener?.invoke(accumulatedWearableSteps)
            }
        }
    }

    /**
     * Parses 4-byte / 2-byte standard Direct Wearable Step Count characteristics
     */
    private fun parseDirectStepCount(data: ByteArray) {
        if (data.isEmpty()) return
        val steps = if (data.size >= 4) {
            (data[0].toInt() and 0xFF) or
                    ((data[1].toInt() and 0xFF) shl 8) or
                    ((data[2].toInt() and 0xFF) shl 16) or
                    ((data[3].toInt() and 0xFF) shl 24)
        } else if (data.size >= 2) {
            (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        } else {
            data[0].toInt() and 0xFF
        }

        if (steps in 1..200000) {
            accumulatedWearableSteps = steps
            mainHandler.post {
                val dev = _connectedDevice.value?.copy(liveStepCount = steps)
                if (dev != null) {
                    _connectedDevice.value = dev
                }
                onStepsListener?.invoke(steps)
            }
        }
    }

    /**
     * Compute RMSSD (Root Mean Square of Successive Differences) in ms
     */
    private fun calculateRmssd(intervals: List<Int>): Double? {
        if (intervals.size < 3) return null
        var sumSquares = 0.0
        var count = 0
        for (i in 0 until intervals.size - 1) {
            val diff = (intervals[i + 1] - intervals[i]).toDouble()
            sumSquares += diff * diff
            count++
        }
        return if (count > 0) sqrt(sumSquares / count) else null
    }

    private fun updateDeviceState(device: SmartwatchDevice) {
        val list = _discoveredDevices.value.map {
            if (it.macAddress.equals(device.macAddress, ignoreCase = true)) device
            else if (device.isConnected) it.copy(isConnected = false, status = WatchConnectionStatus.DISCONNECTED)
            else it
        }
        _discoveredDevices.value = list
    }

    private fun markDisconnected(device: SmartwatchDevice) {
        val disconnected = device.copy(
            isConnected = false,
            status = WatchConnectionStatus.DISCONNECTED,
            liveHeartRateBpm = null,
            liveCadenceSpm = null,
            liveStepCount = null
        )
        updateDeviceState(disconnected)
    }

    private fun guessBrand(name: String): SmartwatchBrand {
        val lower = name.lowercase()
        return when {
            lower.contains("garmin") || lower.contains("forerunner") || lower.contains("fenix") || lower.contains("venu") -> SmartwatchBrand.GARMIN
            lower.contains("galaxy") || lower.contains("pixel") || lower.contains("wear") || lower.contains("samsung") -> SmartwatchBrand.WEAR_OS
            lower.contains("apple") || lower.contains("watch") -> SmartwatchBrand.APPLE_WATCH
            lower.contains("polar") || lower.contains("wahoo") || lower.contains("whoop") || lower.contains("h10") || lower.contains("strap") || lower.contains("scosche") -> SmartwatchBrand.POLAR_WAHOO
            lower.contains("coros") || lower.contains("suunto") -> SmartwatchBrand.COROS_SUUNTO
            lower.contains("fitbit") || lower.contains("mi") || lower.contains("band") || lower.contains("amazfit") || lower.contains("huawei") || lower.contains("honor") -> SmartwatchBrand.FITBIT_XIAOMI
            else -> SmartwatchBrand.GENERIC_BLE
        }
    }
}
