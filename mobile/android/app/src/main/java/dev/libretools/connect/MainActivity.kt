package dev.libretools.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.composables.icons.lucide.*
import dev.libretools.connect.data.Device
import dev.libretools.connect.data.DeviceType
import dev.libretools.connect.data.PluginCapability
import dev.libretools.connect.ui.screens.AboutScreen
import dev.libretools.connect.ui.screens.DeviceDetailScreen
import dev.libretools.connect.ui.screens.DevicesScreen
import dev.libretools.connect.ui.screens.DiscoverScreen
import dev.libretools.connect.ui.screens.PluginScreen
import dev.libretools.connect.ui.screens.SettingsScreen
import dev.libretools.connect.ui.theme.LibreConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LibreConnectTheme { LibreConnectApp() } }
    }
}

@Composable
fun LibreConnectApp() {
    val navController = rememberNavController()
    val devices = remember {
        mutableStateOf(
                listOf(
                        Device(
                                "desktop-1",
                                "My Desktop PC",
                                DeviceType.DESKTOP,
                                isConnected = true,
                                batteryLevel = 85,
                                isCharging = false,
                                capabilities = PluginCapability.values().toList()
                        ),
                        Device(
                                "laptop-1",
                                "Work Laptop",
                                DeviceType.LAPTOP,
                                isConnected = false,
                                batteryLevel = 42,
                                isCharging = true,
                                lastSeen = System.currentTimeMillis() - 300000,
                                capabilities =
                                        listOf(
                                                PluginCapability.CLIPBOARD,
                                                PluginCapability.FILE_TRANSFER,
                                                PluginCapability.NOTIFICATIONS,
                                                PluginCapability.MEDIA_CONTROL
                                        )
                        )
                )
        )
    }

    NavHost(navController = navController, startDestination = "devices") {
        composable("devices") {
            DevicesScreen(navController = navController, devices = devices.value)
        }

        composable("discover") { DiscoverScreen(navController = navController) }

        composable(
                "device/{deviceId}",
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val device = devices.value.find { it.id == deviceId }
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
            val device = devices.value.find { it.id == deviceId }
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
    LibreConnectTheme { LibreConnectApp() }
}
