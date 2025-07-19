package dev.libretools.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.PluginCapability
import dev.libretools.connect.data.formatLastSeen

@Composable
fun DeviceCard(device: Device, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (device.isConnected)
                                            MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                            if (device.isConnected)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        device.type.icon,
                        contentDescription = null,
                        tint =
                                if (device.isConnected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            if (device.isConnected) "Connected" else "Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                    if (device.isConnected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                    )
                    if (!device.isConnected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                "• Last seen ${formatLastSeen(device.lastSeen)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (device.isConnected && device.capabilities.isNotEmpty()) {
                    Text(
                            "${device.capabilities.size} plugins available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (device.batteryLevel != null) {
                BatteryIndicator(device.batteryLevel, device.isCharging)
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                    Lucide.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun BatteryIndicator(level: Int, isCharging: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
                Lucide.Battery,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint =
                        when {
                            level > 20 -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
                        }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
                "$level%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TipCard(icon: ImageVector, title: String, description: String) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PluginCard(plugin: PluginCapability, isEnabled: Boolean, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = isEnabled) { onClick() },
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isEnabled) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    plugin.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint =
                            if (isEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        plugin.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                                if (isEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline
                )
                Text(
                        plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                                if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (isEnabled) {
                Icon(
                        Lucide.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DeviceInfoCard(device: Device, onReconnectClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (device.isConnected)
                                            MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                    )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        device.type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint =
                                if (device.isConnected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                            device.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                    if (device.isConnected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                            if (device.isConnected) "Connected" else "Offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                    if (device.isConnected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (device.batteryLevel != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        BatteryIndicator(device.batteryLevel, device.isCharging)
                    }
                }
            }

            if (!device.isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                        onClick = onReconnectClick,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair Device")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                        onClick = onReconnectClick,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Unplug, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(
        title: String,
        description: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
