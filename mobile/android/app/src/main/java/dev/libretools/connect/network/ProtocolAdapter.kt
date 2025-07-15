package dev.libretools.connect.network

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Protocol adapter for converting between Android app messages and LibreConnect daemon messages.
 * Handles serialization/deserialization and message type mapping for the JSON-based protocol.
 */
class ProtocolAdapter {
    companion object {
        private const val TAG = "ProtocolAdapter"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ===== OUTGOING MESSAGES (Android → Rust) =====

    /** Create a ping message for connectivity testing */
    fun createPingMessage(): String {
        val message = RustMessage.Ping
        return json.encodeToString(message)
    }

    /** Create a pong response message */
    fun createPongMessage(): String {
        val message = RustMessage.Pong
        return json.encodeToString(message)
    }

    /** Create device info message for handshake */
    fun createDeviceInfoMessage(deviceId: String, deviceName: String): String {
        val deviceInfo =
                RustDeviceInfo(
                        id = deviceId,
                        name = deviceName,
                        device_type = "Mobile",
                        capabilities =
                                listOf(
                                        "ClipboardSync",
                                        "FileTransfer",
                                        "InputShare",
                                        "NotificationSync",
                                        "BatteryStatus",
                                        "MediaControl",
                                        "RemoteCommands",
                                        "TouchpadMode",
                                        "SlideControl"
                                )
                )
        val message = RustMessage.DeviceInfo(deviceInfo)
        return json.encodeToString(message)
    }

    /** Create pairing request message */
    fun createPairingRequestMessage(deviceId: String, deviceName: String): String {
        val deviceInfo =
                RustDeviceInfo(
                        id = deviceId,
                        name = deviceName,
                        device_type = "Mobile",
                        capabilities = listOf()
                )
        val message = RustMessage.RequestPairing(deviceInfo)
        return json.encodeToString(message)
    }

    /** Create clipboard sync message */
    fun createClipboardSyncMessage(content: String): String {
        val message = RustMessage.ClipboardSync(content)
        return json.encodeToString(message)
    }

    /** Create clipboard request message */
    fun createClipboardRequestMessage(): String {
        val message = RustMessage.RequestClipboard
        return json.encodeToString(message)
    }

    /** Create file transfer request message */
    fun createFileTransferRequestMessage(fileName: String, fileSize: Long): String {
        val message = RustMessage.FileTransferRequest(file_name = fileName, file_size = fileSize)
        return json.encodeToString(message)
    }

    /** Create file transfer chunk message */
    fun createFileTransferChunkMessage(fileName: String, chunk: ByteArray, offset: Long): String {
        val message =
                RustMessage.FileTransferChunk(
                        file_name = fileName,
                        chunk =
                                chunk.map {
                                    it.toInt()
                                }, // Convert to List<Int> for JSON serialization
                        offset = offset
                )
        return json.encodeToString(message)
    }

    /** Create key event message */
    fun createKeyEventMessage(action: String, keyCode: String): String {
        val keyEvent =
                RustKeyEvent(
                        action =
                                when (action.lowercase()) {
                                    "press", "down" -> "Press"
                                    "release", "up" -> "Release"
                                    else -> "Press"
                                },
                        code = keyCode
                )
        val message = RustMessage.KeyEvent(keyEvent)
        return json.encodeToString(message)
    }

    /** Create mouse event message */
    fun createMouseEventMessage(
            action: String,
            x: Int,
            y: Int,
            button: String? = null,
            scrollDelta: Int? = null
    ): String {
        val mouseEvent =
                RustMouseEvent(
                        action = action,
                        x = x,
                        y = y,
                        button = button,
                        scroll_delta = scrollDelta
                )
        val message = RustMessage.MouseEvent(mouseEvent)
        return json.encodeToString(message)
    }

    /** Create touchpad event message */
    fun createTouchpadEventMessage(
            x: Int,
            y: Int,
            dx: Int,
            dy: Int,
            scrollDeltaX: Int = 0,
            scrollDeltaY: Int = 0,
            isLeftClick: Boolean = false,
            isRightClick: Boolean = false
    ): String {
        val touchpadEvent =
                RustTouchpadEvent(
                        x = x,
                        y = y,
                        dx = dx,
                        dy = dy,
                        scroll_delta_x = scrollDeltaX,
                        scroll_delta_y = scrollDeltaY,
                        is_left_click = isLeftClick,
                        is_right_click = isRightClick
                )
        val message = RustMessage.TouchpadEvent(touchpadEvent)
        return json.encodeToString(message)
    }

    /** Create notification message */
    fun createNotificationMessage(title: String, body: String, appName: String? = null): String {
        val message = RustMessage.Notification(title = title, body = body, app_name = appName)
        return json.encodeToString(message)
    }

    /** Create media control message */
    fun createMediaControlMessage(action: String): String {
        val message = RustMessage.MediaControl(action = action)
        return json.encodeToString(message)
    }

    /** Create battery status message */
    fun createBatteryStatusMessage(charge: Int, isCharging: Boolean): String {
        val batteryStatus = RustBatteryStatus(charge = charge, is_charging = isCharging)
        val message = RustMessage.BatteryStatus(batteryStatus)
        return json.encodeToString(message)
    }

    /** Create remote command message */
    fun createRemoteCommandMessage(command: String, args: List<String> = emptyList()): String {
        val message = RustMessage.RemoteCommand(command = command, args = args)
        return json.encodeToString(message)
    }

    /** Create slide control message */
    fun createSlideControlMessage(action: String): String {
        val message = RustMessage.SlideControl(action)
        return json.encodeToString(message)
    }

    // ===== INCOMING MESSAGES (Rust → Android) =====

    /** Parse incoming message from Rust daemon */
    fun parseIncomingMessage(jsonMessage: String): ParsedMessage? {
        return try {
            val rustMessage = json.decodeFromString<RustMessage>(jsonMessage)
            convertRustMessage(rustMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming message: $jsonMessage", e)
            null
        }
    }

    private fun convertRustMessage(rustMessage: RustMessage): ParsedMessage {
        return when (rustMessage) {
            is RustMessage.Ping -> ParsedMessage.Ping
            is RustMessage.Pong -> ParsedMessage.Pong
            is RustMessage.DeviceInfo ->
                    ParsedMessage.DeviceInfo(
                            id = rustMessage.deviceInfo.id,
                            name = rustMessage.deviceInfo.name,
                            deviceType = rustMessage.deviceInfo.device_type,
                            capabilities = rustMessage.deviceInfo.capabilities
                    )
            is RustMessage.PairingAccepted -> ParsedMessage.PairingAccepted(rustMessage.deviceId)
            is RustMessage.PairingRejected -> ParsedMessage.PairingRejected(rustMessage.deviceId)
            is RustMessage.ClipboardSync -> ParsedMessage.ClipboardSync(rustMessage.content)
            is RustMessage.RequestClipboard -> ParsedMessage.RequestClipboard
            is RustMessage.FileTransferRequest ->
                    ParsedMessage.FileTransferRequest(
                            fileName = rustMessage.file_name,
                            fileSize = rustMessage.file_size
                    )
            is RustMessage.Notification ->
                    ParsedMessage.Notification(
                            title = rustMessage.title,
                            body = rustMessage.body,
                            appName = rustMessage.app_name
                    )
            is RustMessage.BatteryStatus ->
                    ParsedMessage.BatteryStatus(
                            charge = rustMessage.batteryStatus.charge,
                            isCharging = rustMessage.batteryStatus.is_charging
                    )
            else -> ParsedMessage.Unknown(rustMessage.toString())
        }
    }

    // ===== RUST MESSAGE DATA CLASSES =====

    @Serializable
    sealed class RustMessage {
        @Serializable object Ping : RustMessage()
        @Serializable object Pong : RustMessage()
        @Serializable data class DeviceInfo(val deviceInfo: RustDeviceInfo) : RustMessage()
        @Serializable data class RequestPairing(val deviceInfo: RustDeviceInfo) : RustMessage()
        @Serializable data class PairingAccepted(val deviceId: String) : RustMessage()
        @Serializable data class PairingRejected(val deviceId: String) : RustMessage()
        @Serializable data class ClipboardSync(val content: String) : RustMessage()
        @Serializable object RequestClipboard : RustMessage()
        @Serializable
        data class FileTransferRequest(val file_name: String, val file_size: Long) : RustMessage()
        @Serializable
        data class FileTransferChunk(
                val file_name: String,
                val chunk: List<Int>,
                val offset: Long
        ) : RustMessage()
        @Serializable data class FileTransferEnd(val file_name: String) : RustMessage()
        @Serializable
        data class FileTransferError(val file_name: String, val error: String) : RustMessage()
        @Serializable data class KeyEvent(val keyEvent: RustKeyEvent) : RustMessage()
        @Serializable data class MouseEvent(val mouseEvent: RustMouseEvent) : RustMessage()
        @Serializable
        data class TouchpadEvent(val touchpadEvent: RustTouchpadEvent) : RustMessage()
        @Serializable
        data class Notification(val title: String, val body: String, val app_name: String?) :
                RustMessage()
        @Serializable data class MediaControl(val action: String) : RustMessage()
        @Serializable
        data class BatteryStatus(val batteryStatus: RustBatteryStatus) : RustMessage()
        @Serializable
        data class RemoteCommand(val command: String, val args: List<String>) : RustMessage()
        @Serializable data class SlideControl(val action: String) : RustMessage()
    }

    @Serializable
    data class RustDeviceInfo(
            val id: String,
            val name: String,
            val device_type: String,
            val capabilities: List<String>
    )

    @Serializable data class RustKeyEvent(val action: String, val code: String)

    @Serializable
    data class RustMouseEvent(
            val action: String,
            val x: Int,
            val y: Int,
            val button: String?,
            val scroll_delta: Int?
    )

    @Serializable
    data class RustTouchpadEvent(
            val x: Int,
            val y: Int,
            val dx: Int,
            val dy: Int,
            val scroll_delta_x: Int,
            val scroll_delta_y: Int,
            val is_left_click: Boolean,
            val is_right_click: Boolean
    )

    @Serializable data class RustBatteryStatus(val charge: Int, val is_charging: Boolean)

    // ===== PARSED MESSAGE TYPES (for Android consumption) =====

    sealed class ParsedMessage {
        object Ping : ParsedMessage()
        object Pong : ParsedMessage()
        data class DeviceInfo(
                val id: String,
                val name: String,
                val deviceType: String,
                val capabilities: List<String>
        ) : ParsedMessage()
        data class PairingAccepted(val deviceId: String) : ParsedMessage()
        data class PairingRejected(val deviceId: String) : ParsedMessage()
        data class ClipboardSync(val content: String) : ParsedMessage()
        object RequestClipboard : ParsedMessage()
        data class FileTransferRequest(val fileName: String, val fileSize: Long) : ParsedMessage()
        data class Notification(val title: String, val body: String, val appName: String?) :
                ParsedMessage()
        data class BatteryStatus(val charge: Int, val isCharging: Boolean) : ParsedMessage()
        data class Unknown(val rawMessage: String) : ParsedMessage()
    }
}
