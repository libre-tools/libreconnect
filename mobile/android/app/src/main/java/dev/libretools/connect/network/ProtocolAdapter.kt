package dev.libretools.connect.network

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
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
        // Default enum (external) tagging is used, which matches Rust serde default
    }

    // ===== OUTGOING MESSAGES (Android → Rust) =====

    /** Create a ping message for connectivity testing */
    fun createPingMessage(): String = json.encodeToString(RustMessage.serializer(), RustMessage.Ping)

    /** Create a pong response message */
    fun createPongMessage(): String = json.encodeToString(RustMessage.serializer(), RustMessage.Pong)

    /** Create device info message for handshake */
    fun createDeviceInfoMessage(deviceId: String, deviceName: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.DeviceInfo(deviceId, deviceName, "Mobile", listOf(
            "ClipboardSync", "FileTransfer", "InputShare", "NotificationSync", "BatteryStatus", "MediaControl", "RemoteCommands", "TouchpadMode", "SlideControl"
        )))

    /** Create pairing request message */
    fun createPairingRequestMessage(deviceId: String, deviceName: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.RequestPairing(deviceId, deviceName, "Mobile", emptyList()))
    /** Create pairing request message with pairing key */
    fun createPairingRequestWithKeyMessage(deviceId: String, deviceName: String, pairingKey: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.RequestPairingWithKey(deviceId, deviceName, "Mobile", emptyList(), pairingKey))

    /** Create clipboard sync message */
    fun createClipboardSyncMessage(content: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.ClipboardSync(content))

    /** Create clipboard request message */
    fun createClipboardRequestMessage(): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.RequestClipboard)

    /** Create file transfer request message */
    fun createFileTransferRequestMessage(fileName: String, fileSize: Long): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.FileTransferRequest(fileName, fileSize))

    /** Create file transfer chunk message */
    fun createFileTransferChunkMessage(fileName: String, chunk: ByteArray, offset: Long): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.FileTransferChunk(fileName, chunk.map { it.toInt() }, offset))

    /** Create key event message */
    fun createKeyEventMessage(action: String, keyCode: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.KeyEvent(action, keyCode))

    /** Create mouse event message */
    fun createMouseEventMessage(
            action: String,
            x: Int,
            y: Int,
            button: String? = null,
            scrollDelta: Int? = null
    ): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.MouseEvent(action, x, y, button, scrollDelta))

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
    ): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.TouchpadEvent(x, y, dx, dy, scrollDeltaX, scrollDeltaY, isLeftClick, isRightClick))

    /** Create notification message */
    fun createNotificationMessage(title: String, body: String, appName: String? = null): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.Notification(title, body, appName))

    /** Create media control message */
    fun createMediaControlMessage(action: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.MediaControl(action))

    /** Create battery status message */
    fun createBatteryStatusMessage(charge: Int, isCharging: Boolean): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.BatteryStatus(charge, isCharging))

    /** Create remote command message */
    fun createRemoteCommandMessage(command: String, args: List<String> = emptyList()): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.RemoteCommand(command, args))

    /** Create slide control message */
    fun createSlideControlMessage(action: String): String =
        json.encodeToString(RustMessage.serializer(), RustMessage.SlideControl(action))

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
                            id = rustMessage.id,
                            name = rustMessage.name,
                            deviceType = rustMessage.device_type,
                            capabilities = rustMessage.capabilities
                    )
            is RustMessage.RequestPairing -> ParsedMessage.RequestPairing(rustMessage.id, rustMessage.name, rustMessage.device_type, rustMessage.capabilities)
            is RustMessage.PairingAccepted -> ParsedMessage.PairingAccepted(rustMessage.deviceId)
            is RustMessage.PairingRejected -> ParsedMessage.PairingRejected(rustMessage.deviceId)
            is RustMessage.ClipboardSync -> ParsedMessage.ClipboardSync(rustMessage.value)
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
                            charge = rustMessage.charge,
                            isCharging = rustMessage.is_charging
                    )
            else -> ParsedMessage.Unknown(rustMessage.toString())
        }
    }

    // ===== RUST MESSAGE DATA CLASSES =====

    @Serializable
    sealed class RustMessage {
        @Serializable
        @SerialName("Ping")
        object Ping : RustMessage()

        @Serializable
        @SerialName("Pong")
        object Pong : RustMessage()

        @Serializable
        @SerialName("DeviceInfo")
        data class DeviceInfo(val id: String, val name: String, val device_type: String, val capabilities: List<String>) : RustMessage()

        @Serializable
        @SerialName("RequestPairing")
        data class RequestPairing(val id: String, val name: String, val device_type: String, val capabilities: List<String>) : RustMessage()
        @Serializable
        @SerialName("RequestPairingWithKey")
        data class RequestPairingWithKey(val id: String, val name: String, val device_type: String, val capabilities: List<String>, val pairing_key: String) : RustMessage()

        @Serializable
        @SerialName("PairingAccepted")
        data class PairingAccepted(val deviceId: String) : RustMessage()

        @Serializable
        @SerialName("PairingRejected")
        data class PairingRejected(val deviceId: String) : RustMessage()

        @Serializable
        @SerialName("ClipboardSync")
        data class ClipboardSync(val value: String) : RustMessage()

        @Serializable
        @SerialName("RequestClipboard")
        object RequestClipboard : RustMessage()

        @Serializable
        @SerialName("FileTransferRequest")
        data class FileTransferRequest(val file_name: String, val file_size: Long) : RustMessage()

        @Serializable
        @SerialName("FileTransferChunk")
        data class FileTransferChunk(val file_name: String, val chunk: List<Int>, val offset: Long) : RustMessage()

        @Serializable
        @SerialName("KeyEvent")
        data class KeyEvent(val action: String, val code: String) : RustMessage()

        @Serializable
        @SerialName("MouseEvent")
        data class MouseEvent(val action: String, val x: Int, val y: Int, val button: String? = null, val scroll_delta: Int? = null) : RustMessage()

        @Serializable
        @SerialName("TouchpadEvent")
        data class TouchpadEvent(val x: Int, val y: Int, val dx: Int, val dy: Int, val scroll_delta_x: Int = 0, val scroll_delta_y: Int = 0, val is_left_click: Boolean = false, val is_right_click: Boolean = false) : RustMessage()

        @Serializable
        @SerialName("Notification")
        data class Notification(val title: String, val body: String, val app_name: String? = null) : RustMessage()

        @Serializable
        @SerialName("MediaControl")
        data class MediaControl(val action: String) : RustMessage()

        @Serializable
        @SerialName("BatteryStatus")
        data class BatteryStatus(val charge: Int, val is_charging: Boolean) : RustMessage()

        @Serializable
        @SerialName("RemoteCommand")
        data class RemoteCommand(val command: String, val args: List<String> = emptyList()) : RustMessage()

        @Serializable
        @SerialName("SlideControl")
        data class SlideControl(val action: String) : RustMessage()
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
        @Serializable
        data class DeviceInfo(
                val id: String,
                val name: String,
                val deviceType: String,
                val capabilities: List<String>
        ) : ParsedMessage()
        data class PairingAccepted(val deviceId: String) : ParsedMessage()
        data class PairingRejected(val deviceId: String) : ParsedMessage()
        data class RequestPairing(val id: String, val name: String, val deviceType: String, val capabilities: List<String>) : ParsedMessage()
        data class ClipboardSync(val content: String) : ParsedMessage()
        object RequestClipboard : ParsedMessage()
        data class FileTransferRequest(val fileName: String, val fileSize: Long) : ParsedMessage()
        data class Notification(val title: String, val body: String, val appName: String?) :
                ParsedMessage()
        data class BatteryStatus(val charge: Int, val isCharging: Boolean) : ParsedMessage()
        data class Unknown(val rawMessage: String) : ParsedMessage()
    }
}
