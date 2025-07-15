package dev.libretools.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.PluginCapability
import dev.libretools.connect.service.LibreConnectServiceConnection
import dev.libretools.connect.ui.screens.AboutScreen
import dev.libretools.connect.ui.screens.DeviceDetailScreen
import dev.libretools.connect.ui.screens.DevicesScreen
import dev.libretools.connect.ui.screens.DiscoverScreen
import dev.libretools.connect.ui.screens.PluginScreen
import dev.libretools.connect.ui.screens.SettingsScreen
import dev.libretools.connect.ui.theme.LibreConnectTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var serviceConnection: LibreConnectServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize service connection
        serviceConnection =
                LibreConnectServiceConnection(
                        context = this,
                        onServiceConnected = { service ->
                            // Service is connected, start it
                            lifecycleScope.launch { serviceConnection.startService() }
                        }
                )

        // Add service connection to lifecycle
        lifecycle.addObserver(serviceConnection)

        setContent { LibreConnectTheme { LibreConnectApp(serviceConnection = serviceConnection) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(serviceConnection)
    }
}

@Composable
fun LibreConnectApp(serviceConnection: LibreConnectServiceConnection) {
    val navController = rememberNavController()

    // Collect devices from service
    val discoveredDevices by
            serviceConnection.discoveredDevices?.collectAsState()
                    ?: remember { mutableStateOf(emptyList()) }
    val connectedDevices by
            serviceConnection.connectedDevices?.collectAsState()
                    ?: remember { mutableStateOf(emptyList()) }
    val connectionStatus by
            serviceConnection.connectionStatus?.collectAsState()
                    ?: remember { mutableStateOf("Initializing...") }

    // Combine discovered and connected devices, prioritizing connected ones
    val allDevices =
            remember(discoveredDevices, connectedDevices) {
                val deviceMap = mutableMapOf<String, Device>()

                // Add discovered devices first
                discoveredDevices.forEach { device -> deviceMap[device.id] = device }

                // Update with connected devices (they override discovered ones)
                connectedDevices.forEach { device ->
                    deviceMap[device.id] = device.copy(isConnected = true)
                }

                deviceMap.values.toList()
            }

    NavHost(navController = navController, startDestination = "devices") {
        composable("devices") {
            DevicesScreen(
                    navController = navController,
                    devices = allDevices,
                    connectionStatus = connectionStatus
            )
        }

        composable("discover") {
            DiscoverScreen(navController = navController, serviceConnection = serviceConnection)
        }

        composable(
                "device/{deviceId}",
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val device = allDevices.find { it.id == deviceId }
            if (device != null) {
                DeviceDetailScreen(device = device, navController = navController)
            }
        }

        composable(
                "plugin/{deviceId}/{pluginName}",
                arguments =
                        listOf(
                                navArgument("deviceId") { type = NavType.StringType },
                                navArgument("pluginName") { type = NavType.StringType }
                        )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val pluginName = backStackEntry.arguments?.getString("pluginName") ?: return@composable
            val device = allDevices.find { it.id == deviceId }
            val plugin = PluginCapability.values().find { it.name == pluginName }

            if (device != null && plugin != null) {
                PluginScreen(device = device, plugin = plugin, navController = navController)
            }
        }

        composable("settings") { SettingsScreen(navController = navController) }
        composable("about") { AboutScreen(navController = navController) }
    }
}

@Preview(showBackground = true)
@Composable
fun LibreConnectPreview() {
    // Preview with mock service connection
    val mockServiceConnection = LibreConnectServiceConnection(LocalContext.current)
    LibreConnectTheme { LibreConnectApp(serviceConnection = mockServiceConnection) }
}
