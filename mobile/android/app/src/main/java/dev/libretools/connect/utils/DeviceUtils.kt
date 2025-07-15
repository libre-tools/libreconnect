package dev.libretools.connect.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.DeviceType
import dev.libretools.connect.data.PluginCapability
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

/** Utility functions for device management and network operations */
object DeviceUtils {

    /** Get the current device's local IP address */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
        return null
    }

    /** Check if the device is connected to WiFi */
    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /** Get the WiFi network name (SSID) */
    fun getWifiNetworkName(context: Context): String? {
        if (!isConnectedToWifi(context)) return null

        val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            @Suppress("DEPRECATION") val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid?.replace("\"", "")
        } catch (e: Exception) {
            null
        }
    }

    /** Generate a unique device ID based on device characteristics */
    fun generateDeviceId(deviceName: String, ipAddress: String?): String {
        val sanitizedName = deviceName.replace(Regex("[^a-zA-Z0-9-_]"), "")
        return if (ipAddress != null) {
            "$sanitizedName-$ipAddress"
        } else {
            "$sanitizedName-${System.currentTimeMillis()}"
        }
    }

    /** Get the current Android device info */
    fun getCurrentDeviceInfo(): Device {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val ipAddress = getLocalIpAddress()
        val deviceId = generateDeviceId(deviceName, ipAddress)

        return Device(
                id = deviceId,
                name = deviceName,
                type = getDeviceType(),
                isConnected = true,
                capabilities = getAllSupportedCapabilities()
        )
    }

    /** Determine device type based on form factor */
    private fun getDeviceType(): DeviceType {
        return when {
            isTablet() -> DeviceType.TABLET
            else -> DeviceType.PHONE
        }
    }

    /** Check if the current device is a tablet */
    private fun isTablet(): Boolean {
        // Simple heuristic based on screen size configuration
        // You might want to use more sophisticated detection
        return false // For now, assume phone
    }

    /** Get all capabilities supported by the Android client */
    private fun getAllSupportedCapabilities(): List<PluginCapability> {
        return listOf(
                PluginCapability.CLIPBOARD,
                PluginCapability.FILE_TRANSFER,
                PluginCapability.INPUT_SHARE,
                PluginCapability.NOTIFICATIONS,
                PluginCapability.MEDIA_CONTROL,
                PluginCapability.BATTERY_STATUS,
                PluginCapability.REMOTE_COMMANDS,
                PluginCapability.TOUCHPAD,
                PluginCapability.SLIDE_CONTROL
        )
    }

    /** Parse device type from string */
    fun parseDeviceType(typeString: String): DeviceType {
        return when (typeString.lowercase()) {
            "desktop", "pc" -> DeviceType.DESKTOP
            "laptop", "notebook" -> DeviceType.LAPTOP
            "phone", "mobile" -> DeviceType.PHONE
            "tablet" -> DeviceType.TABLET
            else -> DeviceType.DESKTOP
        }
    }

    /** Parse capabilities from comma-separated string */
    fun parseCapabilities(capabilitiesString: String): List<PluginCapability> {
        if (capabilitiesString.isBlank()) {
            return getAllSupportedCapabilities()
        }

        return capabilitiesString.split(",").mapNotNull { capability ->
            when (capability.trim().lowercase()) {
                "clipboard" -> PluginCapability.CLIPBOARD
                "file-transfer", "file_transfer" -> PluginCapability.FILE_TRANSFER
                "input-share", "input_share" -> PluginCapability.INPUT_SHARE
                "notifications" -> PluginCapability.NOTIFICATIONS
                "media-control", "media_control" -> PluginCapability.MEDIA_CONTROL
                "battery-status", "battery_status" -> PluginCapability.BATTERY_STATUS
                "remote-commands", "remote_commands" -> PluginCapability.REMOTE_COMMANDS
                "touchpad" -> PluginCapability.TOUCHPAD
                "slide-control", "slide_control" -> PluginCapability.SLIDE_CONTROL
                else -> null
            }
        }
    }

    /** Check if a device is reachable on the network */
    suspend fun pingDevice(ipAddress: String, timeoutMs: Int = 3000): Boolean {
        return try {
            val address = InetAddress.getByName(ipAddress)
            address.isReachable(timeoutMs)
        } catch (e: Exception) {
            false
        }
    }

    /** Format device capabilities for display */
    fun formatCapabilities(capabilities: List<PluginCapability>): String {
        return when (capabilities.size) {
            0 -> "No capabilities"
            1 -> capabilities.first().displayName
            in 2..3 -> capabilities.joinToString(", ") { it.displayName }
            else ->
                    "${capabilities.take(3).joinToString(", ") { it.displayName }} +${capabilities.size - 3} more"
        }
    }

    /** Get battery level if available */
    fun getBatteryLevel(context: Context): Int? {
        return try {
            val batteryManager =
                    context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val level =
                    batteryManager.getIntProperty(
                            android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
                    )
            if (level >= 0) level else null
        } catch (e: Exception) {
            null
        }
    }

    /** Check if device is charging */
    fun isDeviceCharging(context: Context): Boolean {
        return try {
            val batteryManager =
                    context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val status =
                    batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
        } catch (e: Exception) {
            false
        }
    }

    /** Validate device connection parameters */
    fun validateConnectionParams(deviceId: String, port: Int): Boolean {
        return deviceId.isNotBlank() && port in 1024..65535
    }

    /** Get network subnet for discovery */
    fun getNetworkSubnet(): String? {
        val localIp = getLocalIpAddress() ?: return null
        val parts = localIp.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } else null
    }
}
