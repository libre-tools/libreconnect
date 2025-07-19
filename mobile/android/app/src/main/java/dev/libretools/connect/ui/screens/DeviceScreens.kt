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
import dev.libretools.connect.ui.components.DeviceInfoCard
import dev.libretools.connect.ui.components.PluginCard
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(device: Device?, navController: NavController, serviceConnection: dev.libretools.connect.service.LibreConnectServiceConnection) {
    Log.d("DeviceDetailScreen", "deviceId: ${device?.id}, device found: ${device != null}")
    if (device == null) {
        // Show error UI if device is missing
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Device not found or offline.", color = MaterialTheme.colorScheme.error)
        }
        return
    }
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(device.name) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Lucide.ArrowLeft, "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* TODO: Device settings */}) {
                                Icon(Lucide.EllipsisVertical, "More options")
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
                DeviceInfoCard(device) { 
                    if (!device.isConnected) {
                        // Navigate to pairing screen instead of directly connecting
                        navController.navigate("pairing/${device.id}")
                    } else {
                        serviceConnection.disconnectFromDevice(device.id)
                    }
                } 
            }

            item {
                Text(
                        "Available Plugins",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )
            }

            items(device.capabilities) { plugin ->
                PluginCard(
                        plugin = plugin,
                        isEnabled = device.isConnected,
                        onClick = {
                            if (device.isConnected) {
                                navController.navigate("plugin/${device.id}/${plugin.name}")
                            }
                        }
                )
            }
        }
    }
}
