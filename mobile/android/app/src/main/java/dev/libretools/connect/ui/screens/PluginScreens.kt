package dev.libretools.connect.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.PluginCapability
import dev.libretools.connect.ui.components.QuickActionButton
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(device: Device, plugin: PluginCapability, navController: NavController) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(plugin.displayName) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Lucide.ArrowLeft, "Back")
                            }
                        }
                )
            }
    ) { innerPadding ->
        when (plugin) {
            PluginCapability.CLIPBOARD ->
                    ClipboardPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.FILE_TRANSFER ->
                    FileTransferPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.INPUT_SHARE ->
                    InputSharePluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.NOTIFICATIONS ->
                    NotificationsPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.MEDIA_CONTROL ->
                    MediaControlPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.BATTERY_STATUS ->
                    BatteryStatusPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.REMOTE_COMMANDS ->
                    RemoteCommandsPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.TOUCHPAD ->
                    TouchpadPluginScreen(device, Modifier.padding(innerPadding))
            PluginCapability.SLIDE_CONTROL ->
                    SlideControlPluginScreen(device, Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun ClipboardPluginScreen(device: Device, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var clipboardContent by remember { mutableStateOf("") }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Clipboard Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Sync clipboard content between devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
                value = clipboardContent,
                onValueChange = { clipboardContent = it },
                label = { Text("Clipboard Content") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
        )

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                    onClick = {
                        val item = clipboardManager.primaryClip?.getItemAt(0)
                        clipboardContent = item?.text?.toString() ?: ""
                    },
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Lucide.Clipboard, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Local")
            }

            Button(
                    onClick = {
                        val clip = ClipData.newPlainText("LibreConnect", clipboardContent)
                        clipboardManager.setPrimaryClip(clip)
                    },
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Lucide.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to ${device.name}")
            }
        }
    }
}

@Composable
fun FileTransferPluginScreen(device: Device, modifier: Modifier = Modifier) {
    var selectedFile by remember { mutableStateOf("") }
    var transferProgress by remember { mutableStateOf(0f) }
    var isTransferring by remember { mutableStateOf(false) }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "File Transfer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Send and receive files between devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
                value = selectedFile,
                onValueChange = { selectedFile = it },
                label = { Text("File Path") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { /* TODO: File picker */}) {
                        Icon(Lucide.FolderOpen, "Browse")
                    }
                }
        )

        if (isTransferring) {
            Column {
                Text("Transferring file...")
                LinearProgressIndicator(
                        progress = { transferProgress },
                        modifier = Modifier.fillMaxWidth()
                )
                Text("${(transferProgress * 100).toInt()}%")
            }
        }

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                    onClick = { /* TODO: Browse files */},
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Lucide.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Files")
            }

            Button(
                    onClick = {
                        if (selectedFile.isNotEmpty()) {
                            isTransferring = true
                            // Simulate transfer
                            Timer().schedule(
                                            object : TimerTask() {
                                                override fun run() {
                                                    transferProgress = 1f
                                                    isTransferring = false
                                                }
                                            },
                                            2000
                                    )
                        }
                    },
                    enabled = selectedFile.isNotEmpty() && !isTransferring,
                    modifier = Modifier.weight(1f)
            ) {
                Icon(Lucide.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send File")
            }
        }
    }
}

@Composable
fun InputSharePluginScreen(device: Device, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Remote Input",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Control mouse and keyboard on ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { /* TODO: Open keyboard */}, modifier = Modifier.weight(1f)) {
                Icon(Lucide.Keyboard, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keyboard")
            }

            Button(onClick = { /* TODO: Open touchpad */}, modifier = Modifier.weight(1f)) {
                Icon(Lucide.MousePointer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Touchpad")
            }
        }

        // Quick action buttons
        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickActionButton(
                        icon = Lucide.Volume2,
                        text = "Volume Up",
                        onClick = { /* TODO: Volume up */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Volume1,
                        text = "Volume Down",
                        onClick = { /* TODO: Volume down */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Play,
                        text = "Play/Pause",
                        onClick = { /* TODO: Play/Pause */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.House,
                        text = "Home",
                        onClick = { /* TODO: Home key */}
                )
            }
        }
    }
}

@Composable
fun NotificationsPluginScreen(device: Device, modifier: Modifier = Modifier) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Mirror notifications from ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                "Enable Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                "Receive notifications from this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }
        }

        if (notificationsEnabled) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                            "Recent Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "No recent notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MediaControlPluginScreen(device: Device, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("No media playing") }
    var currentArtist by remember { mutableStateOf("") }
    var volume by remember { mutableStateOf(0.5f) }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Media Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Control media playback on ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Now Playing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentTitle, style = MaterialTheme.typography.bodyMedium)
                if (currentArtist.isNotEmpty()) {
                    Text(
                            currentArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { /* TODO: Previous track */}, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.SkipBack, contentDescription = "Previous")
            }

            IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        /* TODO: Play/Pause */
                    },
                    modifier = Modifier.size(64.dp)
            ) {
                Icon(
                        if (isPlaying) Lucide.Pause else Lucide.Play,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { /* TODO: Next track */}, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.SkipForward, contentDescription = "Next")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Volume",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Volume1, contentDescription = null)
                    Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            modifier = Modifier.weight(1f)
                    )
                    Icon(Lucide.Volume2, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun BatteryStatusPluginScreen(device: Device, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Battery Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Monitor battery status of ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                                "Battery Level",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                        )
                        Text(
                                "${device.batteryLevel ?: 0}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                            Lucide.Battery,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint =
                                    when {
                                        (device.batteryLevel ?: 0) > 20 ->
                                                MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                        progress = { (device.batteryLevel ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        if (device.isCharging) "Charging" else "Not charging",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RemoteCommandsPluginScreen(device: Device, modifier: Modifier = Modifier) {
    var commandText by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Remote Commands",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Execute commands on ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                label = { Text("Command") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                            onClick = {
                                if (commandText.isNotEmpty()) {
                                    commandHistory = commandHistory + commandText
                                    commandText = ""
                                }
                            }
                    ) { Icon(Lucide.Send, "Execute") }
                }
        )

        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickActionButton(
                        icon = Lucide.Power,
                        text = "Shutdown",
                        onClick = { /* TODO: Shutdown */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.RotateCcw,
                        text = "Restart",
                        onClick = { /* TODO: Restart */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Lock,
                        text = "Lock Screen",
                        onClick = { /* TODO: Lock */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Moon,
                        text = "Sleep",
                        onClick = { /* TODO: Sleep */}
                )
            }
        }

        if (commandHistory.isNotEmpty()) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                            "Command History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(commandHistory.reversed()) { command ->
                            Text(
                                    "> $command",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TouchpadPluginScreen(device: Device, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Touchpad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Use your phone as a touchpad for ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(16.dp)
                                    .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                    )
                                    .clickable { /* TODO: Touchpad functionality */},
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        "Touchpad Area",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { /* TODO: Left click */}, modifier = Modifier.weight(1f)) {
                Text("Left Click")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { /* TODO: Right click */}, modifier = Modifier.weight(1f)) {
                Text("Right Click")
            }
        }
    }
}

@Composable
fun SlideControlPluginScreen(device: Device, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Slide Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        "Control slide presentations on ${device.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickActionButton(
                        icon = Lucide.ChevronLeft,
                        text = "Previous Slide",
                        onClick = { /* TODO: Previous slide */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.ChevronRight,
                        text = "Next Slide",
                        onClick = { /* TODO: Next slide */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Play,
                        text = "Start Slideshow",
                        onClick = { /* TODO: Start slideshow */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.Square,
                        text = "End Slideshow",
                        onClick = { /* TODO: End slideshow */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.ChevronsLeft,
                        text = "First Slide",
                        onClick = { /* TODO: First slide */}
                )
            }
            item {
                QuickActionButton(
                        icon = Lucide.ChevronsRight,
                        text = "Last Slide",
                        onClick = { /* TODO: Last slide */}
                )
            }
        }
    }
}
