# LibreConnect Android Client

A modern Android client for LibreConnect, enabling seamless device connectivity and control across multiple platforms.

## Overview

LibreConnect Android provides a rich mobile interface for connecting to and controlling LibreConnect-enabled devices. Built with Jetpack Compose and Material Design 3, it offers intuitive access to all LibreConnect plugins and features.

## Features

### 🔌 Plugin Support
- **Clipboard Sync** - Real-time clipboard synchronization between devices
- **File Transfer** - Send and receive files with progress tracking
- **Remote Input** - Control mouse and keyboard remotely
- **Notifications** - Mirror notifications across devices
- **Media Control** - Control media playback with volume adjustment
- **Battery Status** - Monitor battery levels and charging status
- **Remote Commands** - Execute secure whitelisted commands
- **Touchpad Mode** - Use phone as a touchpad
- **Slide Control** - Control presentations remotely

### 📱 Modern Android Features
- **Material Design 3** - Beautiful, accessible interface
- **Jetpack Compose** - Modern declarative UI
- **Background Service** - Persistent device connectivity
- **Device Discovery** - Automatic network device detection
- **Real-time Status** - Live connection and battery monitoring
- **Dark Theme Support** - System-adaptive theming

## Architecture

### Core Components

```
LibreConnect Android
├── UI Layer (Jetpack Compose)
│   ├── Screens - Device list, plugin interfaces, settings
│   ├── Components - Reusable UI elements
│   └── Theme - Material Design 3 theming
├── Service Layer
│   ├── LibreConnectService - Background connectivity service
│   ├── DeviceDiscovery - mDNS device discovery
│   └── NetworkManager - TCP connection management
├── Data Layer
│   ├── Device Models - Data classes for devices and plugins
│   └── Utilities - Helper functions and extensions
└── Network Layer
    ├── JSON Serialization - Message serialization/deserialization
    └── Protocol - LibreConnect network protocol implementation
```

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Icons**: Lucide Icons (1,520+ beautiful icons)
- **Navigation**: Navigation Compose
- **Networking**: OkHttp + Kotlinx Serialization
- **Service Discovery**: JmDNS (mDNS/Bonjour)
- **Concurrency**: Kotlin Coroutines + Flow
- **Architecture**: MVVM + Repository Pattern

## Building

### Prerequisites

- Android SDK 33+ (compileSdk 36)
- Java 21
- Kotlin 2.2.0
- Gradle 8.13+

### Build Commands

```bash
# Clean build
./gradlew clean

# Compile Kotlin sources
./gradlew compileDebugKotlin

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Generate lint report
./gradlew lintDebug
```

### Generated Artifacts

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
app/src/main/java/dev/libretools/connect/
├── MainActivity.kt                     # Entry point and navigation
├── data/
│   └── DeviceModels.kt                 # Data classes and enums
├── network/
│   ├── DeviceDiscovery.kt              # mDNS device discovery
│   └── NetworkManager.kt               # TCP connection management
├── service/
│   ├── LibreConnectService.kt          # Background connectivity service
│   └── ServiceConnection.kt            # Service binding and lifecycle
├── ui/
│   ├── components/
│   │   └── CommonComponents.kt         # Reusable UI components
│   ├── screens/
│   │   ├── MainScreen.kt              # Device list and discovery
│   │   ├── DeviceScreens.kt           # Device detail views
│   │   ├── PluginScreens.kt           # Plugin-specific interfaces
│   │   └── SettingsScreens.kt         # Settings and about pages
│   └── theme/
│       ├── Color.kt                   # Color definitions
│       ├── Theme.kt                   # Material theme setup
│       └── Type.kt                    # Typography definitions
└── utils/
    └── DeviceUtils.kt                 # Utility functions
```

## Configuration

### Network Settings

- **Default Port**: 1716
- **Discovery Protocol**: mDNS (_libreconnect._tcp.local.)
- **Connection Timeout**: 10 seconds
- **Read Timeout**: 30 seconds

### Permissions

Required permissions in `AndroidManifest.xml`:

```xml
<!-- Network access -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Service management -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- File access -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Usage

### Device Discovery

1. Open the app and tap the **+** (Discover) button
2. Ensure devices are on the same WiFi network
3. LibreConnect daemon must be running on target devices
4. Discovered devices appear in the main list

### Plugin Usage

Each plugin provides a dedicated interface:

- **Clipboard**: View and sync clipboard content
- **File Transfer**: Browse and send files with progress
- **Input Share**: Virtual keyboard and touchpad
- **Media Control**: Play/pause, volume, track navigation
- **Remote Commands**: Quick actions and custom commands

### Background Operation

The app runs a foreground service to maintain device connections:

- Service starts automatically when needed
- Persistent notification shows connection status
- Background scanning for device availability
- Automatic reconnection on network changes

## Development

### Key Classes

#### `LibreConnectService`
Background service managing device connectivity:
```kotlin
class LibreConnectService : Service() {
    fun connectToDevice(deviceId: String)
    fun sendPluginMessage(deviceId: String, pluginType: String, message: Map<String, Any>)
}
```

#### `DeviceDiscovery`
Handles mDNS device discovery:
```kotlin
class DeviceDiscovery(
    private val onDeviceFound: (Device) -> Unit,
    private val onDeviceLost: (String) -> Unit
)
```

#### `NetworkManager`
Manages TCP connections and messaging:
```kotlin
class NetworkManager(
    private val onDeviceConnected: (Device) -> Unit,
    private val onDeviceDisconnected: (Device) -> Unit
)
```

### Extending Functionality

To add new plugins:

1. Add capability to `PluginCapability` enum
2. Create plugin screen in `PluginScreens.kt`
3. Update navigation in `MainActivity.kt`
4. Implement network protocol in `NetworkManager.kt`

### Testing

The project includes:
- **Unit Tests**: Core logic testing
- **UI Tests**: Compose UI testing
- **Integration Tests**: Service and network testing

## Troubleshooting

### Common Issues

**Devices not discovered**
- Verify same WiFi network
- Check firewall settings (port 1716)
- Ensure LibreConnect daemon is running

**Connection failures**
- Network connectivity issues
- Port conflicts
- Service not started

**UI not updating**
- Service connection issues
- StateFlow not collecting properly

### Debugging

Enable debug logging:
```kotlin
Log.d("LibreConnect", "Debug message")
```

View service status:
```bash
adb shell dumpsys activity services dev.libretools.connect
```

## Contributing

1. Follow Material Design 3 guidelines
2. Use Jetpack Compose best practices
3. Maintain consistent code style
4. Add comprehensive tests
5. Update documentation

## License

LibreConnect is open-source software. See the root LICENSE file for details.

## Related Projects

- **LibreConnect Core**: Rust daemon and protocol implementation
- **LibreConnect CLI**: Command-line interface
- **LibreConnect Plugins**: Individual plugin implementations

---

**Built with ❤️ for seamless device connectivity**