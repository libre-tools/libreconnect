package dev.libretools.connect.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*

// Data Classes
data class Device(
        val id: String,
        val name: String,
        val type: DeviceType,
        val isConnected: Boolean = false,
        val batteryLevel: Int? = null,
        val isCharging: Boolean = false,
        val lastSeen: Long = System.currentTimeMillis(),
        val capabilities: List<PluginCapability> = emptyList(),
        val ipAddress: String? = null,
        val port: Int? = null
)

enum class DeviceType(val displayName: String, val icon: ImageVector) {
    DESKTOP("Desktop", Lucide.Monitor),
    LAPTOP("Laptop", Lucide.Laptop),
    PHONE("Phone", Lucide.Smartphone),
    TABLET("Tablet", Lucide.Tablet)
}

enum class PluginCapability(
        val displayName: String,
        val icon: ImageVector,
        val description: String
) {
    CLIPBOARD("Clipboard", Lucide.Clipboard, "Sync clipboard content"),
    FILE_TRANSFER("File Transfer", Lucide.FolderOpen, "Send and receive files"),
    INPUT_SHARE("Remote Input", Lucide.MousePointer, "Control mouse and keyboard"),
    NOTIFICATIONS("Notifications", Lucide.Bell, "Mirror notifications"),
    MEDIA_CONTROL("Media Control", Lucide.Play, "Control media playback"),
    BATTERY_STATUS("Battery", Lucide.Battery, "Monitor battery status"),
    REMOTE_COMMANDS("Remote Commands", Lucide.Terminal, "Execute commands remotely"),
    TOUCHPAD("Touchpad", Lucide.Hand, "Use as touchpad"),
    SLIDE_CONTROL("Slide Control", Lucide.Presentation, "Control slide presentations")
}

// Helper function for formatting timestamps
fun formatLastSeen(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
