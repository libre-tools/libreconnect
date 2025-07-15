package dev.libretools.connect.network

import android.util.Log
import dev.libretools.connect.data.Device
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Active connections to devices
    private val activeConnections = ConcurrentHashMap<String, DeviceConnection>()
    private var isManagerActive = false
    private var heartbeatJob: Job? = null

    @Serializable
    data class NetworkMessage(
            val type: String,
            val payload: String,
            val timestamp: Long = System.currentTimeMillis()
    )

    @Serializable
    data class PluginMessage(
            val plugin: String,
            val action: String,
            val data: Map<String, String> = emptyMap()
    )

    @Serializable
    data class DeviceInfo(
            val id: String,
            val name: String,
            val type: String,
            val capabilities: List<String>
    )

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

                // For demo purposes, we'll simulate a connection
                // In real implementation, you'd parse IP from device.id or use mDNS resolution
                val socket = Socket()
                socket.connect(
                        java.net.InetSocketAddress("127.0.0.1", DEFAULT_PORT),
                        CONNECTION_TIMEOUT
                )
                socket.soTimeout = READ_TIMEOUT

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                // Send initial handshake
                val handshake =
                        NetworkMessage(
                                type = "handshake",
                                payload =
                                        json.encodeToString(
                                                DeviceInfo(
                                                        id = "android-${android.os.Build.MODEL}",
                                                        name = "Android Device",
                                                        type = "mobile",
                                                        capabilities =
                                                                listOf(
                                                                        "clipboard",
                                                                        "notifications",
                                                                        "file-transfer"
                                                                )
                                                )
                                        )
                        )

                sendMessage(writer, handshake)

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

                // For demo purposes, simulate successful connection
                simulateConnection(device)
            }
        }
    }

    // Temporary simulation for demo
    private fun simulateConnection(device: Device) {
        Log.d(TAG, "Simulating connection to ${device.name} for demo")
        onDeviceConnected(device)
        onConnectionStatusChanged("Simulated connection to ${device.name}")
    }

    suspend fun disconnectFromDevice(device: Device) {
        withContext(Dispatchers.IO) {
            val connection = activeConnections.remove(device.id)
            if (connection != null) {
                try {
                    Log.d(TAG, "Disconnecting from ${device.name}")

                    // Send disconnect message
                    val disconnectMessage = NetworkMessage(type = "disconnect", payload = "Goodbye")
                    sendMessage(connection.writer, disconnectMessage)

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

    suspend fun sendPluginMessage(device: Device, pluginType: String, message: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            val connection = activeConnections[device.id]
            if (connection != null) {
                try {
                    val pluginMessage =
                            PluginMessage(
                                    plugin = pluginType,
                                    action = "execute",
                                    data = message.mapValues { it.value.toString() }
                            )

                    val networkMessage =
                            NetworkMessage(
                                    type = "plugin",
                                    payload = json.encodeToString(pluginMessage)
                            )

                    sendMessage(connection.writer, networkMessage)
                    Log.d(TAG, "Sent plugin message to ${device.name}: $pluginType")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send plugin message to ${device.name}", e)
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
                    val message = json.decodeFromString<NetworkMessage>(line)
                    handleIncomingMessage(device, message)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message from ${device.name}: $line", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Connection lost to ${device.name}", e)
            handleConnectionLost(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for messages from ${device.name}", e)
        }
    }

    private fun handleIncomingMessage(device: Device, message: NetworkMessage) {
        Log.d(TAG, "Received message from ${device.name}: ${message.type}")

        when (message.type) {
            "pong" -> {
                Log.d(TAG, "Received pong from ${device.name}")
            }
            "plugin" -> {
                try {
                    val pluginMessage = json.decodeFromString<PluginMessage>(message.payload)
                    handlePluginMessage(device, pluginMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse plugin message", e)
                }
            }
            "notification" -> {
                handleNotificationMessage(device, message.payload)
            }
            else -> {
                Log.d(TAG, "Unknown message type: ${message.type}")
            }
        }
    }

    private fun handlePluginMessage(device: Device, pluginMessage: PluginMessage) {
        Log.d(
                TAG,
                "Plugin message from ${device.name}: ${pluginMessage.plugin} - ${pluginMessage.action}"
        )

        // Handle different plugin responses
        when (pluginMessage.plugin) {
            "clipboard" -> {
                Log.d(TAG, "Clipboard data received: ${pluginMessage.data}")
            }
            "battery" -> {
                Log.d(TAG, "Battery status received: ${pluginMessage.data}")
            }
            "media" -> {
                Log.d(TAG, "Media control response: ${pluginMessage.data}")
            }
        }
    }

    private fun handleNotificationMessage(device: Device, payload: String) {
        Log.d(TAG, "Notification from ${device.name}: $payload")
        // TODO: Show system notification
    }

    private fun handleConnectionLost(device: Device) {
        Log.w(TAG, "Connection lost to ${device.name}")
        activeConnections.remove(device.id)
        onDeviceDisconnected(device)
        onConnectionStatusChanged("Connection lost to ${device.name}")
    }

    private fun sendMessage(writer: BufferedWriter, message: NetworkMessage) {
        try {
            val jsonString = json.encodeToString(message)
            writer.write(jsonString)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            throw e
        }
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
                val pingMessage = NetworkMessage(type = "ping", payload = "heartbeat")
                sendMessage(connection.writer, pingMessage)
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
