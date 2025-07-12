package dev.libretools.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.TextField
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
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var daemonStatus by remember { mutableStateOf("Daemon Status: Not Started") }
    var clipboardContent by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var fileTransferStatus by remember { mutableStateOf("File Transfer Status: Idle") }

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
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = {
                daemonStatus = "Daemon Status: Started (simulated)"
            }) {
                Text("Start Daemon")
            }

            TextField(
                value = clipboardContent,
                onValueChange = { clipboardContent = it },
                label = { Text("Clipboard Content") },
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(onClick = {
                val clip = ClipData.newPlainText("LibreConnect Clipboard", clipboardContent)
                clipboardManager.setPrimaryClip(clip)
                println("Set local clipboard: $clipboardContent")
                // In a real app, this would send clipboardContent to the daemon
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Set Local Clipboard")
            }

            Button(onClick = {
                val item = clipboardManager.primaryClip?.getItemAt(0)
                val text = item?.text?.toString() ?: ""
                clipboardContent = text
                println("Got local clipboard: $clipboardContent")
                // In a real app, this would request clipboard from the daemon
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Get Local Clipboard")
            }

            TextField(
                value = filePath,
                onValueChange = { filePath = it },
                label = { Text("File Path") },
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(onClick = {
                fileTransferStatus = "Sending file: $filePath (simulated)"
                println("Simulating sending file: $filePath")
                // In a real app, this would send the file to the daemon
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Send File")
            }

            Button(onClick = {
                fileTransferStatus = "Receiving file (simulated)"
                println("Simulating receiving file.")
                // In a real app, this would request a file from the daemon
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Receive File")
            }

            Text(
                text = fileTransferStatus,
                modifier = Modifier.padding(top = 16.dp)
            )
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