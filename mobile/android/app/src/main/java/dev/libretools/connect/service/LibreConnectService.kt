package dev.libretools.connect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.libretools.connect.MainActivity
import dev.libretools.connect.R
import dev.libretools.connect.data.Device
import dev.libretools.connect.network.DeviceDiscovery
import dev.libretools.connect.network.NetworkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibreConnectService : Service() {

    companion object {
        private const val TAG = "LibreConnectService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "libreconnect_channel"
        private const val CHANNEL_NAME = "LibreConnect Background Service"

        // Service actions
        const val ACTION_START_SERVICE = "dev.libretools.connect.START_SERVICE"
        const val ACTION_STOP_SERVICE = "dev.libretools.connect.STOP_SERVICE"
        const val ACTION_DISCOVER_DEVICES = "dev.libretools.connect.DISCOVER_DEVICES"
    }

    // Binder for UI to service communication
    inner class LocalBinder : Binder() {
        fun getService(): LibreConnectService = this@LibreConnectService
    }

    private val binder = LocalBinder()
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Core components
    private lateinit var networkManager: NetworkManager
    private lateinit var deviceDiscovery: DeviceDiscovery

    // Service state
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<Device>>(emptyList())
    val connectedDevices: StateFlow<List<Device>> = _connectedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LibreConnectService created")

        // Initialize core components
        networkManager =
                NetworkManager(
                        onDeviceConnected = { device -> handleDeviceConnected(device) },
                        onDeviceDisconnected = { device -> handleDeviceDisconnected(device) },
                        onConnectionStatusChanged = { status -> _connectionStatus.value = status }
                )

        deviceDiscovery =
                DeviceDiscovery(
                        context = this,
                        onDeviceFound = { device -> handleDeviceDiscovered(device) },
                        onDeviceLost = { deviceId -> handleDeviceLost(deviceId) }
                )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                startLibreConnectService()
            }
            ACTION_STOP_SERVICE -> {
                stopLibreConnectService()
                stopSelf()
            }
            ACTION_DISCOVER_DEVICES -> {
                startDeviceDiscovery()
            }
        }

        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LibreConnectService destroyed")

        stopLibreConnectService()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    CHANNEL_NAME,
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description =
                                        "Background service for LibreConnect device connectivity"
                                setShowBadge(false)
                            }

            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LibreConnect")
                .setContentText(
                        "Running in background - ${_connectedDevices.value.size} devices connected"
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build()
    }

    private fun updateNotification() {
        val notification = createServiceNotification()
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startLibreConnectService() {
        if (_isServiceRunning.value) {
            Log.d(TAG, "Service already running")
            return
        }

        serviceScope.launch {
            try {
                Log.d(TAG, "Starting LibreConnect service components")

                // Start network manager
                networkManager.start()

                // Device discovery will be started when startDeviceDiscovery() is called

                _isServiceRunning.value = true
                _connectionStatus.value = "Service Running"

                Log.d(TAG, "LibreConnect service started successfully")

                // Start real device discovery
                startDeviceDiscovery()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LibreConnect service", e)
                _connectionStatus.value = "Failed to start: ${e.message}"
            }
        }
    }

    private fun stopLibreConnectService() {
        if (!_isServiceRunning.value) return

        serviceScope.launch {
            try {
                Log.d(TAG, "Stopping LibreConnect service")

                // Stop device discovery
                deviceDiscovery.stop()

                // Stop network manager
                networkManager.stop()

                _isServiceRunning.value = false
                _connectionStatus.value = "Service Stopped"
                _connectedDevices.value = emptyList()
                _discoveredDevices.value = emptyList()

                Log.d(TAG, "LibreConnect service stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping LibreConnect service", e)
            }
        }
    }

    private fun startDeviceDiscovery() {
        serviceScope.launch {
            Log.d(TAG, "Starting device discovery")
            deviceDiscovery.startDiscovery()
        }
    }

    private fun handleDeviceDiscovered(device: Device) {
        Log.d(TAG, "Device discovered: ${device.name}")

        val currentDevices = _discoveredDevices.value.toMutableList()
        if (!currentDevices.any { it.id == device.id }) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }

    private fun handleDeviceLost(deviceId: String) {
        Log.d(TAG, "Device lost: $deviceId")

        val currentDevices = _discoveredDevices.value.toMutableList()
        currentDevices.removeAll { it.id == deviceId }
        _discoveredDevices.value = currentDevices
    }

    private fun handleDeviceConnected(device: Device) {
        Log.d(TAG, "Device connected: ${device.name}")

        val currentDevices = _connectedDevices.value.toMutableList()
        if (!currentDevices.any { it.id == device.id }) {
            currentDevices.add(device.copy(isConnected = true))
            _connectedDevices.value = currentDevices
            updateNotification()
        }
    }

    private fun handleDeviceDisconnected(device: Device) {
        Log.d(TAG, "Device disconnected: ${device.name}")

        val currentDevices = _connectedDevices.value.toMutableList()
        currentDevices.removeAll { it.id == device.id }
        _connectedDevices.value = currentDevices
        updateNotification()
    }

    // Device discovery is now handled by real mDNS discovery
    // Mock devices removed - devices will be discovered automatically

    // Public API for UI
    fun connectToDevice(deviceId: String) {
        serviceScope.launch {
            val device = _discoveredDevices.value.find { it.id == deviceId }
            if (device != null) {
                Log.d(TAG, "Attempting to connect to device: ${device.name}")
                networkManager.connectToDevice(device)
            }
        }
    }

    fun disconnectFromDevice(deviceId: String) {
        serviceScope.launch {
            val device = _connectedDevices.value.find { it.id == deviceId }
            if (device != null) {
                Log.d(TAG, "Disconnecting from device: ${device.name}")
                networkManager.disconnectFromDevice(device)
            }
        }
    }

    fun sendPluginMessage(deviceId: String, pluginType: String, message: Map<String, Any>) {
        serviceScope.launch {
            val device = _connectedDevices.value.find { it.id == deviceId }
            if (device != null) {
                Log.d(TAG, "Sending plugin message to ${device.name}: $pluginType")

                // Route to appropriate NetworkManager method based on plugin type
                when (pluginType) {
                    "clipboard" -> {
                        val content = message["content"]?.toString() ?: ""
                        networkManager.sendClipboardSync(device, content)
                    }
                    "input" -> {
                        val action = message["action"]?.toString() ?: "press"
                        val keyCode = message["keyCode"]?.toString() ?: ""
                        networkManager.sendKeyEvent(device, action, keyCode)
                    }
                    "mouse" -> {
                        val action = message["action"]?.toString() ?: "move"
                        val x = message["x"]?.toString()?.toIntOrNull() ?: 0
                        val y = message["y"]?.toString()?.toIntOrNull() ?: 0
                        val button = message["button"]?.toString()
                        val scrollDelta = message["scrollDelta"]?.toString()?.toIntOrNull()
                        networkManager.sendMouseEvent(device, action, x, y, button, scrollDelta)
                    }
                    "touchpad" -> {
                        val x = message["x"]?.toString()?.toIntOrNull() ?: 0
                        val y = message["y"]?.toString()?.toIntOrNull() ?: 0
                        val dx = message["dx"]?.toString()?.toIntOrNull() ?: 0
                        val dy = message["dy"]?.toString()?.toIntOrNull() ?: 0
                        val isLeftClick = message["isLeftClick"]?.toString()?.toBoolean() ?: false
                        val isRightClick = message["isRightClick"]?.toString()?.toBoolean() ?: false
                        networkManager.sendTouchpadEvent(
                                device,
                                x,
                                y,
                                dx,
                                dy,
                                0,
                                0,
                                isLeftClick,
                                isRightClick
                        )
                    }
                    "media" -> {
                        val action = message["action"]?.toString() ?: "play"
                        networkManager.sendMediaControl(device, action)
                    }
                    "remote" -> {
                        val command = message["command"]?.toString() ?: ""
                        val args =
                                (message["args"] as? List<*>)?.mapNotNull { it?.toString() }
                                        ?: emptyList()
                        networkManager.sendRemoteCommand(device, command, args)
                    }
                    "slide" -> {
                        val action = message["action"]?.toString() ?: "next"
                        networkManager.sendSlideControl(device, action)
                    }
                    else -> {
                        Log.w(TAG, "Unknown plugin type: $pluginType")
                    }
                }
            } else {
                Log.w(TAG, "Device not found: $deviceId")
            }
        }
    }
}
