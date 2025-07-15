package dev.libretools.connect.network

import android.util.Log
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.DeviceType
import dev.libretools.connect.data.PluginCapability
import kotlinx.coroutines.*

class DeviceDiscovery(
        private val onDeviceFound: (Device) -> Unit,
        private val onDeviceLost: (String) -> Unit
) {
    companion object {
        private const val TAG = "DeviceDiscovery"
        private const val MOCK_DISCOVERY_DELAY = 2000L // 2 seconds for demo
    }

    private var isDiscoveryActive = false
    private var discoveryJob: Job? = null
    private val discoveredDevices = mutableMapOf<String, Device>()

    suspend fun start() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting DeviceDiscovery")
            isDiscoveryActive = true
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Stopping DeviceDiscovery")
            isDiscoveryActive = false
            discoveryJob?.cancel()
            discoveredDevices.clear()
        }
    }

    suspend fun startDiscovery() {
        withContext(Dispatchers.IO) {
            if (!isDiscoveryActive) {
                Log.w(TAG, "Discovery service not active")
                return@withContext
            }

            Log.d(TAG, "Starting device discovery scan")
            startMockDiscovery()
        }
    }

    private fun startMockDiscovery() {
        Log.d(TAG, "Starting mock discovery for demonstration")

        discoveryJob =
                CoroutineScope(Dispatchers.IO).launch {
                    delay(MOCK_DISCOVERY_DELAY)

                    if (isDiscoveryActive) {
                        val mockDevices = createMockDevices()
                        mockDevices.forEach { device ->
                            discoveredDevices[device.id] = device
                            onDeviceFound(device)
                            delay(500) // Stagger discoveries
                        }
                    }
                }
    }

    private fun createMockDevices(): List<Device> {
        return listOf(
                Device(
                        id = "mock-desktop-192.168.1.100",
                        name = "Ubuntu Desktop",
                        type = DeviceType.DESKTOP,
                        isConnected = false,
                        capabilities =
                                listOf(
                                        PluginCapability.CLIPBOARD,
                                        PluginCapability.FILE_TRANSFER,
                                        PluginCapability.INPUT_SHARE,
                                        PluginCapability.NOTIFICATIONS,
                                        PluginCapability.MEDIA_CONTROL,
                                        PluginCapability.BATTERY_STATUS,
                                        PluginCapability.REMOTE_COMMANDS,
                                        PluginCapability.SLIDE_CONTROL
                                )
                ),
                Device(
                        id = "mock-laptop-192.168.1.105",
                        name = "MacBook Pro",
                        type = DeviceType.LAPTOP,
                        isConnected = false,
                        batteryLevel = 78,
                        isCharging = false,
                        capabilities =
                                listOf(
                                        PluginCapability.CLIPBOARD,
                                        PluginCapability.FILE_TRANSFER,
                                        PluginCapability.NOTIFICATIONS,
                                        PluginCapability.MEDIA_CONTROL,
                                        PluginCapability.BATTERY_STATUS,
                                        PluginCapability.TOUCHPAD
                                )
                ),
                Device(
                        id = "mock-desktop-192.168.1.110",
                        name = "Windows Gaming PC",
                        type = DeviceType.DESKTOP,
                        isConnected = false,
                        capabilities =
                                listOf(
                                        PluginCapability.CLIPBOARD,
                                        PluginCapability.FILE_TRANSFER,
                                        PluginCapability.INPUT_SHARE,
                                        PluginCapability.MEDIA_CONTROL,
                                        PluginCapability.REMOTE_COMMANDS,
                                        PluginCapability.SLIDE_CONTROL
                                )
                )
        )
    }

    fun getDiscoveredDevices(): List<Device> {
        return discoveredDevices.values.toList()
    }

    fun isDeviceDiscovered(deviceId: String): Boolean {
        return discoveredDevices.containsKey(deviceId)
    }
}
