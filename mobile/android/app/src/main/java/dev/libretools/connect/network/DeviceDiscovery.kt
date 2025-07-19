package dev.libretools.connect.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.DeviceType
import dev.libretools.connect.data.PluginCapability
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlinx.coroutines.*

class DeviceDiscovery(
        private val context: Context,
        private val onDeviceFound: (Device) -> Unit,
        private val onDeviceLost: (String) -> Unit
) {
    companion object {
        private const val TAG = "DeviceDiscovery"
        private const val SERVICE_TYPE = "_libreconnect._tcp.local."
        private const val DISCOVERY_TIMEOUT = 5000L // 5 seconds timeout
    }

    private var isDiscoveryActive = false
    private var discoveryJob: Job? = null
    private val discoveredDevices = mutableMapOf<String, Device>()
    private var jmdns: JmDNS? = null

    suspend fun startDiscovery() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "startDiscovery called. isDiscoveryActive=$isDiscoveryActive")
            if (isDiscoveryActive) {
                Log.w(TAG, "Discovery already active, restarting...")
                stop() // Stop previous discovery before restarting
            }
            isDiscoveryActive = true
            discoveredDevices.clear()
            val inetAddress = getWifiInetAddress()
            Log.i(TAG, "Starting real mDNS device discovery on IP: $inetAddress")
            startRealDiscovery()
        }
    }

    private fun startRealDiscovery() {
        Log.i(TAG, "startRealDiscovery called")
        discoveryJob =
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Get WiFi interface
                        val wifiManager =
                                context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val multicastLock = wifiManager.createMulticastLock("LibreConnectDiscovery")
                        multicastLock.acquire()

                        try {
                            // Log all network interfaces
                            try {
                                val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
                                for (iface in interfaces) {
                                    val addrs = java.util.Collections.list(iface.inetAddresses)
                                    for (addr in addrs) {
                                        Log.d(TAG, "Interface: ${iface.displayName}, Address: ${addr.hostAddress}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error listing network interfaces", e)
                            }

                            // Initialize JmDNS
                            val inetAddress = getWifiInetAddress()
                            Log.d(TAG, "WiFi InetAddress: $inetAddress")
                            if (inetAddress == null) {
                                Log.e(TAG, "No WiFi connection available")
                                return@launch
                            }

                            jmdns = JmDNS.create(inetAddress, "LibreConnectAndroid")
                            Log.i(TAG, "JmDNS initialized on ${inetAddress.hostAddress}")

                            // Add service listener
                            val serviceListener =
                                    object : ServiceListener {
                                        override fun serviceAdded(event: ServiceEvent) {
                                            Log.i(TAG, "Service added: ${event.name} (${event.type})")
                                            // Request service info
                                            jmdns?.requestServiceInfo(
                                                    event.type,
                                                    event.name,
                                                    DISCOVERY_TIMEOUT
                                            )
                                        }

                                        override fun serviceRemoved(event: ServiceEvent) {
                                            Log.i(TAG, "Service removed: ${event.name} (${event.type})")
                                            val deviceId = event.name
                                            discoveredDevices.remove(deviceId)
                                        }

                                        override fun serviceResolved(event: ServiceEvent) {
                                            Log.i(TAG, "Service resolved: ${event.name} (${event.type})")
                                            val serviceInfo = event.info

                                            if (serviceInfo != null) {
                                                try {
                                                    val deviceId = event.name
                                                    val deviceName =
                                                            serviceInfo.getPropertyString("name")
                                                                    ?: event.name
                                                    val deviceTypeStr =
                                                            serviceInfo.getPropertyString(
                                                                    "device_type"
                                                            )
                                                                    ?: "desktop"
                                                    val pluginCount =
                                                            serviceInfo
                                                                    .getPropertyString("plugins")
                                                                    ?.toIntOrNull()
                                                                    ?: 0

                                                    val deviceType =
                                                            when (deviceTypeStr.lowercase()) {
                                                                "mobile" -> DeviceType.PHONE
                                                                "laptop" -> DeviceType.LAPTOP
                                                                else -> DeviceType.DESKTOP
                                                            }

                                                    // Create device with all capabilities (will be
                                                    // refined during pairing)
                                                    val capabilities =
                                                            PluginCapability.entries.take(
                                                                    pluginCount.coerceAtMost(
                                                                            PluginCapability.entries
                                                                                    .size
                                                                    )
                                                            )

                                                    val device =
                                                            Device(
                                                                    id = deviceId,
                                                                    name = deviceName,
                                                                    type = deviceType,
                                                                    isConnected = false,
                                                                    ipAddress =
                                                                            serviceInfo
                                                                                    .inet4Addresses
                                                                                    ?.firstOrNull()
                                                                                    ?.hostAddress,
                                                                    port = serviceInfo.port,
                                                                    capabilities = capabilities
                                                            )

                                                    discoveredDevices[deviceId] = device
                                                    onDeviceFound(device)

                                                    Log.i(
                                                            TAG,
                                                            "Discovered device: $deviceName ($deviceType) at ${device.ipAddress}:${device.port}"
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e(
                                                            TAG,
                                                            "Error processing discovered device",
                                                            e
                                                    )
                                                }
                                            } else {
                                                Log.w(TAG, "serviceInfo is null for resolved event: ${event.name}")
                                            }
                                        }
                                    }

                            // Start browsing for LibreConnect services
                            jmdns?.addServiceListener(SERVICE_TYPE, serviceListener)
                            Log.i(TAG, "Started browsing for LibreConnect services")

                            // Keep discovery running while active
                            while (isDiscoveryActive) {
                                kotlinx.coroutines.delay(1000)
                            }
                        } finally {
                            multicastLock.release()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during mDNS discovery", e)
                    }
                }
    }

    private fun getWifiInetAddress(): InetAddress? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            if (ipAddress != 0) {
                val ip =
                        String.format(
                                "%d.%d.%d.%d",
                                ipAddress and 0xff,
                                ipAddress shr 8 and 0xff,
                                ipAddress shr 16 and 0xff,
                                ipAddress shr 24 and 0xff
                        )
                InetAddress.getByName(ip)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi IP address", e)
            null
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "stop called. isDiscoveryActive=$isDiscoveryActive")
            Log.d(TAG, "Stopping DeviceDiscovery")
            isDiscoveryActive = false
            discoveryJob?.cancelAndJoin()
            discoveryJob = null
            try {
                jmdns?.close()
                jmdns = null
            } catch (e: Exception) {
                Log.e(TAG, "Error closing JmDNS", e)
            }
            discoveredDevices.clear()
        }
    }

    fun getDiscoveredDevices(): List<Device> {
        return discoveredDevices.values.toList()
    }

    fun isDeviceDiscovered(deviceId: String): Boolean {
        return discoveredDevices.containsKey(deviceId)
    }
}
