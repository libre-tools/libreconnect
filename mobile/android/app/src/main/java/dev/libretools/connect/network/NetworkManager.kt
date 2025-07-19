package dev.libretools.connect.network

import android.content.Context
import android.util.Log
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.PairedDeviceStorage
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

class NetworkManager(
        private val context: Context,
        private val onDeviceConnected: (Device) -> Unit,
        private val onDeviceDisconnected: (Device) -> Unit,
        private val onConnectionStatusChanged: (String) -> Unit
) {
    companion object {
        private const val TAG = "NetworkManager"
        private const val DEFAULT_PORT = 1716
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 120000 // 2 minutes (much longer)
        private const val CONNECTION_CHECK_INTERVAL = 120000L // 2 minutes (much less frequent)
        private const val VERBOSE_LOGGING = true // Enable verbose logging for debugging
    }

    private val protocolAdapter = ProtocolAdapter()
    private val pairedDeviceStorage = PairedDeviceStorage(context)

    // Active connections to devices
    private val activeConnections = ConcurrentHashMap<String, DeviceConnection>()
    private var isManagerActive = false
    private var heartbeatJob: Job? = null

    private data class DeviceConnection(
            val device: Device,
            val socket: Socket,
            val reader: BufferedReader,
            val writer: BufferedWriter,
            val job: Job
    )

    suspend fun start() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting NetworkManager")
            isManagerActive = true
            // DISABLED: startHeartbeat() - removing broken heartbeat system
            onConnectionStatusChanged("NetworkManager Started")
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Stopping NetworkManager")
            isManagerActive = false

            heartbeatJob?.cancel()

            // Close all active connections
            activeConnections.values.forEach { connection ->
                try {
                    connection.job.cancel()
                    connection.socket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing connection to ${connection.device.name}", e)
                }
            }
            activeConnections.clear()

            onConnectionStatusChanged("NetworkManager Stopped")
        }
    }

    suspend fun connectToDevice(device: Device) {
        withContext(Dispatchers.IO) {
            if (activeConnections.containsKey(device.id)) {
                Log.d(TAG, "Already connected to ${device.name}")
                return@withContext
            }

            try {
                Log.d(TAG, "Connecting to ${device.name} at ${device.id}:$DEFAULT_PORT")
                onConnectionStatusChanged("Connecting to ${device.name}...")

                // Use real IP address and port from discovered device
                val ipAddress = device.ipAddress ?: throw IllegalArgumentException("Device has no IP address")
                if (VERBOSE_LOGGING) Log.v(TAG, "Device IP Address: $ipAddress")
                val port = device.port ?: DEFAULT_PORT
                if (VERBOSE_LOGGING) Log.v(TAG, "Device Port: $port")

                Log.d(TAG, "Attempting to connect to ${device.name} at $ipAddress:$port")

                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
                Log.d(TAG, "Socket connected to ${device.name}")
                socket.soTimeout = READ_TIMEOUT

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                Log.d(TAG, "Input/Output streams opened for ${device.name}")

                // Send initial pairing request instead of device info
                val deviceId = "android-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                val deviceName = "Android Device (${android.os.Build.MODEL})"
                val pairingMessage = protocolAdapter.createPairingRequestMessage(deviceId, deviceName)
                if (VERBOSE_LOGGING) Log.v(TAG, "Sending pairing request: $pairingMessage")
                Log.d(TAG, "Sending pairing request to ${device.name}: $pairingMessage")

                writer.write(pairingMessage)
                writer.newLine()
                try {
                    writer.flush()
                    if (VERBOSE_LOGGING) Log.v(TAG, "Handshake message flushed")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush handshake message", e)
                    return@withContext
                }

                // Start message listening job
                val job =
                        CoroutineScope(Dispatchers.IO).launch { listenForMessages(device, reader) }

                val connection = DeviceConnection(device, socket, reader, writer, job)
                activeConnections[device.id] = connection

                onDeviceConnected(device)
                onConnectionStatusChanged("Connected to ${device.name}")

                Log.d(TAG, "Successfully connected to ${device.name}")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout to ${device.name}", e)
                onConnectionStatusChanged("Connection timeout to ${device.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to ${device.name}", e)
                onConnectionStatusChanged("Failed to connect to ${device.name}: ${e.message}")
            }
        }
    }

    suspend fun pairWithDevice(device: Device, pairingKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (activeConnections.containsKey(device.id)) {
                Log.d(TAG, "Already connected to ${device.name}")
                return@withContext true
            }

            try {
                Log.d(TAG, "Pairing with ${device.name} using key: $pairingKey")
                onConnectionStatusChanged("Pairing with ${device.name}...")

                // Use real IP address and port from discovered device
                val ipAddress = device.ipAddress ?: throw IllegalArgumentException("Device has no IP address")
                val port = device.port ?: DEFAULT_PORT

                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
                socket.soTimeout = READ_TIMEOUT

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                // Send pairing request with key
                val deviceId = "android-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                val deviceName = "Android Device (${android.os.Build.MODEL})"
                val pairingMessage = protocolAdapter.createPairingRequestWithKeyMessage(deviceId, deviceName, pairingKey)
                
                writer.write(pairingMessage)
                writer.newLine()
                writer.flush()

                // Wait for pairing response (with timeout)
                val startTime = System.currentTimeMillis()
                var pairingResult: Boolean? = null
                
                while (pairingResult == null && System.currentTimeMillis() - startTime < 10000) {
                    try {
                        socket.soTimeout = 1000 // 1 second timeout for each read attempt
                        val response = reader.readLine()
                        if (response != null) {
                            val parsedMessage = protocolAdapter.parseIncomingMessage(response)
                            when (parsedMessage) {
                                is ProtocolAdapter.ParsedMessage.PairingAccepted -> {
                                    pairingResult = true
                                    Log.d(TAG, "Pairing accepted by ${device.name}")
                                }
                                is ProtocolAdapter.ParsedMessage.PairingRejected -> {
                                    pairingResult = false
                                    Log.d(TAG, "Pairing rejected by ${device.name}")
                                }
                                else -> {
                                    // Ignore other message types during pairing
                                }
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Continue waiting
                        continue
                    }
                }

                if (pairingResult == true) {
                    // Reset socket timeout to normal read timeout for ongoing communication
                    socket.soTimeout = READ_TIMEOUT
                    
                    // Start message listening job
                    val job = CoroutineScope(Dispatchers.IO).launch { listenForMessages(device, reader) }
                    val connection = DeviceConnection(device, socket, reader, writer, job)
                    activeConnections[device.id] = connection

                    // Save paired device to persistent storage
                    pairedDeviceStorage.savePairedDevice(device)

                    onDeviceConnected(device)
                    onConnectionStatusChanged("Successfully paired with ${device.name}")
                    return@withContext true
                } else {
                    socket.close()
                    onConnectionStatusChanged("Pairing failed with ${device.name}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pair with ${device.name}", e)
                onConnectionStatusChanged("Failed to pair with ${device.name}: ${e.message}")
                return@withContext false
            }
        }
    }

    suspend fun disconnectFromDevice(device: Device) {
        withContext(Dispatchers.IO) {
            val connection = activeConnections.remove(device.id)
            if (connection != null) {
                try {
                    Log.d(TAG, "Disconnecting from ${device.name}")

                    // Send ping to test connection before closing
                    val pingMessage = protocolAdapter.createPingMessage()
                    connection.writer.write(pingMessage)
                    connection.writer.newLine()
                    try {
                        connection.writer.flush()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to flush ping message", e)
                    }

                    connection.job.cancel()
                    connection.socket.close()

                    onDeviceDisconnected(device)
                    onConnectionStatusChanged("Disconnected from ${device.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting from ${device.name}", e)
                }
            }
        }
    }

    suspend fun sendClipboardSync(device: Device, content: String) {
        sendLibreConnectMessage(device, protocolAdapter.createClipboardSyncMessage(content))
    }

    suspend fun sendKeyEvent(device: Device, action: String, keyCode: String) {
        sendLibreConnectMessage(device, protocolAdapter.createKeyEventMessage(action, keyCode))
    }

    suspend fun sendMouseEvent(
            device: Device,
            action: String,
            x: Int,
            y: Int,
            button: String? = null,
            scrollDelta: Int? = null
    ) {
        sendLibreConnectMessage(
                device,
                protocolAdapter.createMouseEventMessage(action, x, y, button, scrollDelta)
        )
    }

    suspend fun sendTouchpadEvent(
            device: Device,
            x: Int,
            y: Int,
            dx: Int,
            dy: Int,
            scrollDeltaX: Int = 0,
            scrollDeltaY: Int = 0,
            isLeftClick: Boolean = false,
            isRightClick: Boolean = false
    ) {
        sendLibreConnectMessage(
                device,
                protocolAdapter.createTouchpadEventMessage(
                        x,
                        y,
                        dx,
                        dy,
                        scrollDeltaX,
                        scrollDeltaY,
                        isLeftClick,
                        isRightClick
                )
        )
    }

    suspend fun sendMediaControl(device: Device, action: String) {
        sendLibreConnectMessage(device, protocolAdapter.createMediaControlMessage(action))
    }

    suspend fun sendRemoteCommand(
            device: Device,
            command: String,
            args: List<String> = emptyList()
    ) {
        sendLibreConnectMessage(device, protocolAdapter.createRemoteCommandMessage(command, args))
    }

    suspend fun sendSlideControl(device: Device, action: String) {
        sendLibreConnectMessage(device, protocolAdapter.createSlideControlMessage(action))
    }

    suspend fun requestClipboard(device: Device) {
        sendLibreConnectMessage(device, protocolAdapter.createClipboardRequestMessage())
    }

    private suspend fun sendLibreConnectMessage(device: Device, message: String) {
        withContext(Dispatchers.IO) {
            val connection = activeConnections[device.id]
            if (connection != null) {
                try {
                    connection.writer.write(message)
                    connection.writer.newLine()
                    connection.writer.flush()
                    Log.d(TAG, "Sent message to ${device.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message to ${device.name}", e)
                }
            } else {
                Log.w(TAG, "No active connection to ${device.name}")
            }
        }
    }

    private suspend fun listenForMessages(device: Device, reader: BufferedReader) {
        try {
            Log.d(TAG, "Started listening for messages from ${device.name}")
            while (isManagerActive) {
                val line = reader.readLine() ?: break

                Log.d(TAG, "Received raw message from ${device.name}: $line")
                try {
                    val parsedMessage = protocolAdapter.parseIncomingMessage(line)
                    if (parsedMessage != null) {
                        if (VERBOSE_LOGGING) Log.v(TAG, "Parsed message: $parsedMessage")
                        handleIncomingMessage(device, parsedMessage)
                    } else {
                        Log.w(TAG, "Failed to parse message from ${device.name}: $line")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message from ${device.name}: $line", e)
                } finally {
                    if (VERBOSE_LOGGING) Log.v(TAG, "Finished processing line: $line")
                }
            }
            Log.d(TAG, "Stopped listening for messages from ${device.name} - isManagerActive: $isManagerActive")
        } catch (e: IOException) {
            Log.e(TAG, "Connection lost to ${device.name}", e)
            handleConnectionLost(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for messages from ${device.name}", e)
            handleConnectionLost(device)
        }
    }

    private fun handleIncomingMessage(device: Device, message: ProtocolAdapter.ParsedMessage) {
        Log.d(TAG, "Received message from ${device.name}: ${message::class.simpleName}")

        when (message) {
            is ProtocolAdapter.ParsedMessage.Ping -> {
                // Respond with pong
                val pongMessage = protocolAdapter.createPongMessage()
                CoroutineScope(Dispatchers.IO).launch {
                    sendLibreConnectMessage(device, pongMessage)
                }
            }
            is ProtocolAdapter.ParsedMessage.Pong -> {
                Log.d(TAG, "Received pong from ${device.name}")
            }
            is ProtocolAdapter.ParsedMessage.ClipboardSync -> {
                Log.d(TAG, "Clipboard sync from ${device.name}: ${message.content}")
                // TODO: Update system clipboard
            }
            is ProtocolAdapter.ParsedMessage.RequestClipboard -> {
                Log.d(TAG, "Clipboard request from ${device.name}")
                // TODO: Send current clipboard content
            }
            is ProtocolAdapter.ParsedMessage.Notification -> {
                Log.d(TAG, "Notification from ${device.name}: ${message.title}")
                // TODO: Show system notification
            }
            is ProtocolAdapter.ParsedMessage.BatteryStatus -> {
                Log.d(TAG, "Battery status from ${device.name}: ${message.charge}%")
            }
            is ProtocolAdapter.ParsedMessage.PairingAccepted -> {
                Log.i(TAG, "Pairing accepted by ${device.name}")
            }
            is ProtocolAdapter.ParsedMessage.PairingRejected -> {
                Log.w(TAG, "Pairing rejected by ${device.name}")
            }
            else -> {
                Log.d(TAG, "Unhandled message type: ${message::class.simpleName}")
            }
        }
    }

    private fun handleConnectionLost(device: Device) {
        Log.w(TAG, "Connection lost to ${device.name}")
        activeConnections.remove(device.id)
        onDeviceDisconnected(device)
        onConnectionStatusChanged("Connection lost to ${device.name}")
    }

    private fun startHeartbeat() {
        // Heartbeat disabled to prevent connection drops
        Log.d(TAG, "Heartbeat system disabled for stable connections")
    }

    private suspend fun sendHeartbeat() {
        // Heartbeat disabled to prevent connection drops  
        Log.d(TAG, "Heartbeat system disabled for stable connections")
    }

    fun getConnectedDevices(): List<Device> {
        return activeConnections.values.map { it.device }
    }

    fun isConnectedToDevice(deviceId: String): Boolean {
        return activeConnections.containsKey(deviceId)
    }

    /**
     * Get all paired devices from storage
     */
    fun getPairedDevices(): List<Device> {
        return pairedDeviceStorage.loadPairedDevices()
    }

    /**
     * Check if a device is paired
     */
    fun isDevicePaired(deviceId: String): Boolean {
        return pairedDeviceStorage.isDevicePaired(deviceId)
    }

    /**
     * Remove a paired device
     */
    fun unpairDevice(deviceId: String) {
        pairedDeviceStorage.removePairedDevice(deviceId)
        // Also disconnect if currently connected
        val device = activeConnections[deviceId]?.device
        if (device != null) {
            CoroutineScope(Dispatchers.IO).launch {
                disconnectFromDevice(device)
            }
        }
    }

    /**
     * Clear all paired devices
     */
    fun clearAllPairedDevices() {
        pairedDeviceStorage.clearAllPairedDevices()
    }

    /**
     * Attempt to reconnect to all paired devices that are currently available
     */
    suspend fun reconnectToPairedDevices(availableDevices: List<Device>) {
        withContext(Dispatchers.IO) {
            val pairedDevices = pairedDeviceStorage.loadPairedDevices()
            Log.d(TAG, "Attempting to reconnect to ${pairedDevices.size} paired device(s)")

            for (pairedDevice in pairedDevices) {
                // Check if this paired device is in the available devices list
                val availableDevice = availableDevices.find { it.id == pairedDevice.id }
                if (availableDevice != null && !isConnectedToDevice(pairedDevice.id)) {
                    Log.d(TAG, "Attempting auto-reconnection to paired device: ${pairedDevice.name}")
                    try {
                        // Use the updated device info (IP address may have changed)
                        connectToDevice(availableDevice)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to auto-reconnect to ${pairedDevice.name}", e)
                    }
                } else if (availableDevice == null) {
                    Log.d(TAG, "Paired device ${pairedDevice.name} is not currently available")
                }
            }
        }
    }
}
