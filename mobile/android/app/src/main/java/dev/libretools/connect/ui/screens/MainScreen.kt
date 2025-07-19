package dev.libretools.connect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.ui.components.DeviceCard
import dev.libretools.connect.ui.components.TipCard
import java.util.*
import android.util.Log
import android.net.Uri
import dev.libretools.connect.service.LibreConnectServiceConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
        navController: NavController,
        devices: List<Device>,
        connectionStatus: String = "Ready",
        serviceConnection: LibreConnectServiceConnection? = null
) {
    // Log all device IDs for debugging
    LaunchedEffect(devices) {
        Log.d("DevicesScreen", "Device count: ${devices.size}")
        Log.d("DevicesScreen", "Device IDs: ${devices.joinToString { it.id }}")
        devices.forEach { device ->
            Log.d("DevicesScreen", "Device: ${device.name} (${device.id}) - Connected: ${device.isConnected}")
        }
    }
    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Text(
                                        "Devices",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        connectionStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Lucide.Settings, "Settings")
                            }
                            IconButton(onClick = { navController.navigate("about") }) {
                                Icon(Lucide.Info, "About")
                            }
                            // Manual refresh button
                            if (serviceConnection != null) {
                                IconButton(onClick = {
                                    serviceConnection.startDeviceDiscovery()
                                }) {
                                    Icon(Lucide.RefreshCw, "Refresh Devices")
                                }
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick = { navController.navigate("discover") },
                        containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Lucide.Plus, "Discover New Devices") }
            }
    ) { innerPadding ->
        if (devices.isEmpty()) {
            EmptyDevicesState(
                    onDiscoverClick = { navController.navigate("discover") },
                    modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                            device = device,
                            onClick = {
                                Log.d("DevicesScreen", "Clicked device: ${device.id}")
                                val safeId = Uri.encode(device.id)
                                navController.navigate("device/$safeId")
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDevicesState(onDiscoverClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                Lucide.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                "No devices found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                "Start by discovering devices on your network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
                onClick = onDiscoverClick,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                        )
        ) {
            Icon(Lucide.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Discover Devices")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                "Tip: Use the + button to quickly discover new devices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
        navController: NavController,
        serviceConnection: dev.libretools.connect.service.LibreConnectServiceConnection? = null
) {
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    "Discover Devices",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Lucide.ArrowLeft, "Back to Devices")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Lucide.Settings, "Settings")
                            }
                        }
                )
            }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                                Lucide.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "Find LibreConnect Devices",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "Scan your local network for devices running LibreConnect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isScanning) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    "Scanning for devices...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                    onClick = { isScanning = false },
                                    colors =
                                            ButtonDefaults.outlinedButtonColors(
                                                    contentColor =
                                                            MaterialTheme.colorScheme
                                                                    .onPrimaryContainer
                                            )
                            ) { Text("Cancel") }
                        } else {
                            Button(
                                    onClick = {
                                        isScanning = true
                                        // Start actual device discovery
                                        serviceConnection?.startDeviceDiscovery()
                                        // Simulate scanning completion for UI
                                        Timer().schedule(
                                                        object : TimerTask() {
                                                            override fun run() {
                                                                isScanning = false
                                                            }
                                                        },
                                                        5000
                                                )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor =
                                                            MaterialTheme.colorScheme.primary
                                            )
                            ) {
                                Icon(Lucide.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Scanning")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                        "Setup Tips",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )
            }

            item {
                TipCard(
                        icon = Lucide.Wifi,
                        title = "Same Network",
                        description = "Ensure all devices are connected to the same Wi-Fi network"
                )
            }

            item {
                TipCard(
                        icon = Lucide.Shield,
                        title = "Firewall Settings",
                        description = "Check that firewall settings allow LibreConnect connections"
                )
            }

            item {
                TipCard(
                        icon = Lucide.Play,
                        title = "Service Running",
                        description = "Make sure LibreConnect daemon is running on target devices"
                )
            }

            item {
                TipCard(
                        icon = Lucide.Network,
                        title = "Port Configuration",
                        description =
                                "Default port is 1716. Ensure it's not blocked by other applications"
                )
            }
        }
    }
}
