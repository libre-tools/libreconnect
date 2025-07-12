package dev.libretools.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            DeviceListScreen(navController = navController)
        }
        composable("deviceDetail/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            DeviceDetailScreen(deviceId = deviceId, navController = navController)
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
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(devices.value) { deviceName ->
                ListItem(
                    headlineContent = { Text(deviceName) },
                    supportingContent = { Text("Status: Connected (Simulated)") },
                    modifier = Modifier.clickable {
                        navController.navigate("deviceDetail/$deviceName")
                    }
                )
                Divider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(deviceId: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Device: $deviceId") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Details for $deviceId", style = MaterialTheme.typography.headlineMedium)
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