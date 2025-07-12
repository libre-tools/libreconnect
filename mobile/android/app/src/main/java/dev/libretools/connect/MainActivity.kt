package dev.libretools.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.libretools.connect.ui.theme.LibreConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibreConnectTheme {
                LibreConnectApp()
            }
        }
    }
}

@Composable
fun LibreConnectApp(modifier: Modifier = Modifier) {
    var daemonStatus by remember { mutableStateOf("Daemon Status: Not Started") }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = daemonStatus,
                modifier = Modifier.padding(bottom = 16.dp) // Add some padding
            )
            Button(onClick = {
                // In a real app, this would call the Rust daemon start function
                daemonStatus = "Daemon Status: Started (simulated)"
            }) {
                Text("Start Daemon")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibreConnectAppPreview() {
    LibreConnectTheme {
        LibreConnectApp()
    }
}
