package dev.libretools.connect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.ui.components.DeviceInfoCard
import dev.libretools.connect.ui.components.PluginCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(device: Device, navController: NavController) {
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
            item { DeviceInfoCard(device) }

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
