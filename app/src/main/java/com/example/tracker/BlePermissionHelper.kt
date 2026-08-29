package com.example.tracker

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

object BlePermissionHelper {

    /**
     * Returns the array of permissions needed for BLE scanning, connecting,
     * high-precision GPS tracking, body sensors, and notifications.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Bluetooth permissions for Android 12 (API 31) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android 11 and lower
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Body sensors for hardware heart rate and activity recognition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // Notification permission for Android 13 (API 33) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * Checks if all required permissions are currently granted.
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Bluetooth Scan and Connect permissions are granted.
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Fine Location permission is granted.
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if Bluetooth hardware adapter is turned ON.
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }

    /**
     * Checks if GPS / Location service is turned ON.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager != null && (
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )
    }

    /**
     * Launches the system App Details settings page if permissions are permanently denied.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Launches Bluetooth Settings screen.
     */
    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * State representing current permission evaluation
 */
data class TrackingPermissionState(
    val hasAllPermissions: Boolean,
    val hasBluetoothPermission: Boolean,
    val hasLocationPermission: Boolean,
    val isBluetoothEnabled: Boolean,
    val isLocationEnabled: Boolean
)

/**
 * Composable permission helper dialog and handler
 */
@Composable
fun BleTrackingPermissionDialog(
    onDismiss: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(
            TrackingPermissionState(
                hasAllPermissions = BlePermissionHelper.hasAllRequiredPermissions(context),
                hasBluetoothPermission = BlePermissionHelper.hasBluetoothPermissions(context),
                hasLocationPermission = BlePermissionHelper.hasLocationPermissions(context),
                isBluetoothEnabled = BlePermissionHelper.isBluetoothEnabled(context),
                isLocationEnabled = BlePermissionHelper.isLocationServiceEnabled(context)
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        permissionState = TrackingPermissionState(
            hasAllPermissions = BlePermissionHelper.hasAllRequiredPermissions(context),
            hasBluetoothPermission = BlePermissionHelper.hasBluetoothPermissions(context),
            hasLocationPermission = BlePermissionHelper.hasLocationPermissions(context),
            isBluetoothEnabled = BlePermissionHelper.isBluetoothEnabled(context),
            isLocationEnabled = BlePermissionHelper.isLocationServiceEnabled(context)
        )
        if (allGranted || BlePermissionHelper.hasLocationPermissions(context)) {
            onPermissionsGranted()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonGreen)), RoundedCornerShape(20.dp))
                .testTag("ble_permission_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BLUETOOTH & GPS PERMISSIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pranam connects directly to Bluetooth Low Energy (BLE) heart rate chest straps and smartwatches to provide real-time biometric telemetry alongside GPS tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission item rows
                PermissionRequirementRow(
                    icon = Icons.Default.Bluetooth,
                    title = "BLE Heart Rate & Smartwatch",
                    description = "Required to discover and stream GATT biometrics from Polar, Garmin, Wahoo, Apple & Wear OS devices",
                    isGranted = permissionState.hasBluetoothPermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRequirementRow(
                    icon = Icons.Default.LocationOn,
                    title = "High-Accuracy GPS Route",
                    description = "Required to pinpoint live start/finish addresses, speed, pace, and route coordinates",
                    isGranted = permissionState.hasLocationPermission
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionRequirementRow(
                    icon = Icons.Default.Favorite,
                    title = "Body & Health Sensors",
                    description = "Required to read step cadence and physiological heart rate metrics",
                    isGranted = permissionState.hasAllPermissions
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { BlePermissionHelper.openAppSettings(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("app_settings_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            permissionLauncher.launch(BlePermissionHelper.getRequiredPermissions())
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("grant_permissions_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = androidx.compose.ui.graphics.Color.Black
                        )
                    ) {
                        Text(
                            text = "Grant Access",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequirementRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, if (isGranted) NeonGreen.copy(alpha = 0.3f) else CyberBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isGranted) NeonGreen.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) NeonGreen else NeonAmber,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 12.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) NeonGreen else NeonAmber,
            modifier = Modifier.size(16.dp)
        )
    }
}
