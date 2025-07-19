package dev.libretools.connect.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persistence of paired devices using SharedPreferences
 */
class PairedDeviceStorage(context: Context) {
    companion object {
        private const val TAG = "PairedDeviceStorage"
        private const val PREFS_NAME = "libreconnect_paired_devices"
        private const val KEY_PAIRED_DEVICES = "paired_devices"
    }

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Save a paired device to persistent storage
     */
    fun savePairedDevice(device: Device) {
        val pairedDevices = loadPairedDevices().toMutableList()
        
        // Remove existing device with same ID if any
        pairedDevices.removeAll { it.id == device.id }
        
        // Add the new/updated device
        pairedDevices.add(device)
        
        savePairedDevices(pairedDevices)
        Log.d(TAG, "Saved paired device: ${device.name} (Total: ${pairedDevices.size})")
    }

    /**
     * Load all paired devices from persistent storage
     */
    fun loadPairedDevices(): List<Device> {
        return try {
            val jsonString = sharedPreferences.getString(KEY_PAIRED_DEVICES, null)
            if (jsonString != null) {
                val devices: List<Device> = json.decodeFromString(jsonString)
                Log.d(TAG, "Loaded ${devices.size} paired device(s) from storage")
                devices
            } else {
                Log.d(TAG, "No paired devices found in storage")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load paired devices", e)
            emptyList()
        }
    }

    /**
     * Remove a paired device from persistent storage
     */
    fun removePairedDevice(deviceId: String) {
        val pairedDevices = loadPairedDevices().toMutableList()
        val removed = pairedDevices.removeAll { it.id == deviceId }
        
        if (removed) {
            savePairedDevices(pairedDevices)
            Log.d(TAG, "Removed paired device: $deviceId (Remaining: ${pairedDevices.size})")
        } else {
            Log.w(TAG, "Device not found for removal: $deviceId")
        }
    }

    /**
     * Check if a device is paired
     */
    fun isDevicePaired(deviceId: String): Boolean {
        return loadPairedDevices().any { it.id == deviceId }
    }

    /**
     * Clear all paired devices
     */
    fun clearAllPairedDevices() {
        sharedPreferences.edit().remove(KEY_PAIRED_DEVICES).apply()
        Log.d(TAG, "Cleared all paired devices")
    }

    /**
     * Get count of paired devices
     */
    fun getPairedDeviceCount(): Int {
        return loadPairedDevices().size
    }

    private fun savePairedDevices(devices: List<Device>) {
        try {
            val jsonString = json.encodeToString(devices)
            sharedPreferences.edit().putString(KEY_PAIRED_DEVICES, jsonString).apply()
            Log.d(TAG, "Saved ${devices.size} paired device(s) to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save paired devices", e)
        }
    }
}