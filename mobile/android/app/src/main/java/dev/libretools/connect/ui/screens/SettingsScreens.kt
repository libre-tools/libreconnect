package dev.libretools.connect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import dev.libretools.connect.ui.components.SettingsToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var deviceName by remember { mutableStateOf("My Android Device") }
    var autoConnect by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Lucide.ArrowLeft, "Back")
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
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                "Device Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                                value = deviceName,
                                onValueChange = { deviceName = it },
                                label = { Text("Device Name") },
                                modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                "Connection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsToggle(
                                title = "Auto Connect",
                                description = "Automatically connect to known devices",
                                checked = autoConnect,
                                onCheckedChange = { autoConnect = it }
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                "Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsToggle(
                                title = "Enable Notifications",
                                description = "Show notifications from connected devices",
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsToggle(
                                title = "Dark Mode",
                                description = "Use dark theme",
                                checked = darkMode,
                                onCheckedChange = { darkMode = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("About") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Lucide.ArrowLeft, "Back")
                            }
                        }
                )
            }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            item {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        shape =
                                                                androidx.compose.foundation.shape
                                                                        .CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    Lucide.Smartphone,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                                "LibreConnect",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                                "Version 1.0.0",
                                style = MaterialTheme.typography.titleMedium,
                                color =
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.8f
                                        )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "Open-source device connectivity",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // About Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    Lucide.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    "About LibreConnect",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "LibreConnect is an open-source device connectivity solution that allows you to seamlessly connect and control multiple devices across different platforms. Share files, sync clipboards, control media, and much more.",
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp
                        )
                    }
                }
            }

            // Features Grid
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    Lucide.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    "Features",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Two-column feature grid
                        val features =
                                listOf(
                                        "📋" to "Clipboard sync",
                                        "📁" to "File transfer",
                                        "🖱️" to "Remote input",
                                        "🔔" to "Notifications",
                                        "🎵" to "Media control",
                                        "🔋" to "Battery monitor",
                                        "💻" to "Remote commands",
                                        "📱" to "Touchpad",
                                        "🎯" to "Presentations"
                                )

                        features.chunked(3).forEach { rowFeatures ->
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowFeatures.forEach { (emoji, text) ->
                                    Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                                emoji,
                                                style = MaterialTheme.typography.headlineMedium,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                                text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2
                                        )
                                    }
                                }
                            }
                            if (rowFeatures != features.chunked(3).last()) {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }

            // Tech Stack & Open Source
            item {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tech Stack
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Icon(
                                    Lucide.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                    "Built With",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    "• Kotlin\n• Jetpack Compose\n• Material Design 3\n• Lucide Icons",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                            )
                        }
                    }

                    // Open Source
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Icon(
                                    Lucide.Heart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                    "Open Source",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    "Contribute, report issues, or view source code on GitHub.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Footer
            item {
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                ) {
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                "Made with ❤️ for seamless connectivity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "LibreConnect © 2024",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                        )
                        )
                    }
                }
            }
        }
    }
}
