package com.alap.mesh.mesh

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class HardwarePermissionStatus(
    val bluetoothGranted: Boolean,
    val wifiHotspotGranted: Boolean,
    val nearbyLocationGranted: Boolean,
    val storageSafReady: Boolean,
    val bluetoothAdapterEnabled: Boolean = false,
    val locationServiceEnabled: Boolean = true
)

object PermissionHelper {

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return list.distinct().toTypedArray()
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isGranted(context, Manifest.permission.BLUETOOTH_SCAN) &&
                isGranted(context, Manifest.permission.BLUETOOTH_ADVERTISE) &&
                isGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasWifiNearbyPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasLocationOrNearbyPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.NEARBY_WIFI_DEVICES) ||
                isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bm?.adapter?.isEnabled == true
    }

    fun isLocationServiceEnabledIfRequired(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun getHardwarePermissionStatus(context: Context): HardwarePermissionStatus {
        return HardwarePermissionStatus(
            bluetoothGranted = hasBluetoothPermissions(context),
            wifiHotspotGranted = hasWifiNearbyPermissions(context),
            nearbyLocationGranted = hasLocationOrNearbyPermission(context),
            storageSafReady = true,
            bluetoothAdapterEnabled = isBluetoothEnabled(context),
            locationServiceEnabled = isLocationServiceEnabledIfRequired(context)
        )
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
