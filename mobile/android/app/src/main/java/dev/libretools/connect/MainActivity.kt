package dev.libretools.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.* // Import all Material3 components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.libretools.connect.ui.theme.LibreConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibreConnectTheme {
                LibreConnectNavHost()
            }
        }
    }
}

@Composable
fun LibreConnectNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "deviceList") {
        composable("deviceList") {
            LibreConnectTheme {
                DeviceListScreen(navController = navController)
            }
        }
        composable("deviceDetail/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            LibreConnectTheme {
                DeviceDetailScreen(deviceId = deviceId, navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(navController: NavController) {
    val devices = remember { mutableStateOf(listOf("Device A", "Device B", "Device C")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LibreConnect Devices") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Implement device discovery */ }) {
                Icon(Icons.Filled.Add, "Add new device")
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) { // Use LazyColumn for scrollable content
            items(devices.value) { deviceName ->
                ListItem(
                    headlineContent = { Text(deviceName) },
                    supportingContent = { Text("Status: Connected (Simulated)") },
                    modifier = Modifier.clickable {
                        navController.navigate("deviceDetail/$deviceName")
                    }
                )
                HorizontalDivider() // Use HorizontalDivider instead of Divider
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(deviceId: String, navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var daemonStatus by remember { mutableStateOf("Daemon Status: Not Started") }
    var clipboardContent by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var fileTransferStatus by remember { mutableStateOf("File Transfer Status: Idle") }
    var keyEventInput by remember { mutableStateOf("") }
    var mouseEventAction by remember { mutableStateOf("") }
    var mouseEventX by remember { mutableStateOf("0") }
    var mouseEventY by remember { mutableStateOf("0") }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationBody by remember { mutableStateOf("") }
    var receivedNotification by remember { mutableStateOf("No new notifications") }
    var mediaControlStatus by remember { mutableStateOf("Media Control: Idle") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Device: $deviceId") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make the column scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between elements
        ) {
            Text(text = "Details for $deviceId", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            // Daemon Status
            Text(
                text = daemonStatus,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = {
                daemonStatus = "Daemon Status: Started (simulated)"
            }) {
                Text("Start Daemon")
            }

            // Clipboard Sync
            TextField(
                value = clipboardContent,
                onValueChange = { clipboardContent = it },
                label = { Text("Clipboard Content") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = {
                    val clip = ClipData.newPlainText("LibreConnect Clipboard", clipboardContent)
                    clipboardManager.setPrimaryClip(clip)
                    println("Set local clipboard: $clipboardContent")
                    // In a real app, this would send clipboardContent to the daemon
                }) {
                    Text("Set Local Clipboard")
                }
                Button(onClick = {
                    val item = clipboardManager.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString() ?: ""
                    clipboardContent = text
                    println("Got local clipboard: $clipboardContent")
                    // In a real app, this would request clipboard from the daemon
                }) {
                    Text("Get Local Clipboard")
                }
            }

            // File Transfer
            TextField(
                value = filePath,
                onValueChange = { filePath = it },
                label = { Text("File Path") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = {
                    fileTransferStatus = "Sending file: $filePath (simulated)"
                    println("Simulating sending file: $filePath")
                    // In a real app, this would send the file to the daemon
                }) {
                    Text("Send File")
                }
                Button(onClick = {
                    fileTransferStatus = "Receiving file (simulated)"
                    println("Simulating receiving file.")
                    // In a real app, this would request a file from the daemon
                }) {
                    Text("Receive File")
                }
            }
            Text(
                text = fileTransferStatus,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Input Share
            TextField(
                value = keyEventInput,
                onValueChange = { keyEventInput = it },
                label = { Text("Key Event (e.g., 'press A')") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                println("Simulating sending key event: $keyEventInput")
                // In a real app, this would send the key event to the daemon
            }) {
                Text("Send Key Event")
            }

            TextField(
                value = mouseEventAction,
                onValueChange = { mouseEventAction = it },
                label = { Text("Mouse Action (e.g., 'move', 'press left')") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = mouseEventX,
                onValueChange = { mouseEventX = it },
                label = { Text("Mouse X") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = mouseEventY,
                onValueChange = { mouseEventY = it },
                label = { Text("Mouse Y") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                println("Simulating sending mouse event: action=$mouseEventAction, x=$mouseEventX, y=$mouseEventY")
                // In a real app, this would send the mouse event to the daemon
            }) {
                Text("Send Mouse Event")
            }

            // Notification Mirroring
            TextField(
                value = notificationTitle,
                onValueChange = { notificationTitle = it },
                label = { Text("Notification Title") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = notificationBody,
                onValueChange = { notificationBody = it },
                label = { Text("Notification Body") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                println("Simulating sending notification: Title='$notificationTitle', Body='$notificationBody'")
                // In a real app, this would send the notification to the daemon
            }) {
                Text("Send Notification")
            }
            Text(
                text = receivedNotification,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Media Control
            Text(
                text = mediaControlStatus,
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = { mediaControlStatus = "Media Control: Play (simulated)" }) { Text("Play") }
                Button(onClick = { mediaControlStatus = "Media Control: Pause (simulated)" }) { Text("Pause") }
                Button(onClick = { mediaControlStatus = "Media Control: Next (simulated)" }) { Text("Next") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = { mediaControlStatus = "Media Control: Previous (simulated)" }) { Text("Previous") }
                Button(onClick = { mediaControlStatus = "Media Control: Volume Up (simulated)" }) { Text("Vol Up") }
                Button(onClick = { mediaControlStatus = "Media Control: Volume Down (simulated)" }) { Text("Vol Down") }
            }
            Button(onClick = { mediaControlStatus = "Media Control: Toggle Mute (simulated)" }) { Text("Toggle Mute") }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Back to Device List")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDeviceListScreen() {
    LibreConnectTheme {
        DeviceListScreen(rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDeviceDetailScreen() {
    LibreConnectTheme {
        DeviceDetailScreen("TestDevice", rememberNavController())
    }
}