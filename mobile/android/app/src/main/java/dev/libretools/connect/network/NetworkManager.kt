package dev.libretools.connect.network

import android.util.Log
import dev.libretools.connect.data.Device
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

class NetworkManager(
        private val onDeviceConnected: (Device) -> Unit,
        private val onDeviceDisconnected: (Device) -> Unit,
        private val onConnectionStatusChanged: (String) -> Unit
) {
    companion object {
        private const val TAG = "NetworkManager"
        private const val DEFAULT_PORT = 1716
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 30000 // 30 seconds
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
    }

    private val protocolAdapter = ProtocolAdapter()

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
            startHeartbeat()
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
                val ipAddress =
                        device.ipAddress
                                ?: throw IllegalArgumentException("Device has no IP address")
                val port = device.port ?: DEFAULT_PORT

                Log.d(TAG, "Connecting to ${device.name} at $ipAddress:$port")

                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
                socket.soTimeout = READ_TIMEOUT

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                // Send initial device info handshake
                val deviceId = "android-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                val deviceName = "Android Device (${android.os.Build.MODEL})"
                val handshakeMessage = protocolAdapter.createDeviceInfoMessage(deviceId, deviceName)

                writer.write(handshakeMessage)
                writer.newLine()
                writer.flush()

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
                    connection.writer.flush()

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
            while (isManagerActive) {
                val line = reader.readLine() ?: break

                try {
                    val parsedMessage = protocolAdapter.parseIncomingMessage(line)
                    if (parsedMessage != null) {
                        handleIncomingMessage(device, parsedMessage)
                    } else {
                        Log.w(TAG, "Failed to parse message from ${device.name}: $line")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message from ${device.name}: $line", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Connection lost to ${device.name}", e)
            handleConnectionLost(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for messages from ${device.name}", e)
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
        heartbeatJob =
                CoroutineScope(Dispatchers.IO).launch {
                    while (isManagerActive) {
                        delay(HEARTBEAT_INTERVAL)
                        sendHeartbeat()
                    }
                }
    }

    private suspend fun sendHeartbeat() {
        val connections = activeConnections.values.toList()
        connections.forEach { connection ->
            try {
                val pingMessage = protocolAdapter.createPingMessage()
                connection.writer.write(pingMessage)
                connection.writer.newLine()
                connection.writer.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send heartbeat to ${connection.device.name}", e)
                handleConnectionLost(connection.device)
            }
        }
    }

    fun getConnectedDevices(): List<Device> {
        return activeConnections.values.map { it.device }
    }

    fun isConnectedToDevice(deviceId: String): Boolean {
        return activeConnections.containsKey(deviceId)
    }
}
