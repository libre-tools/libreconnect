# LibreConnect Android UI Structure

This document describes the refactored UI structure for the LibreConnect Android app. The code has been organized into separate files for better maintainability and readability.

## File Structure

```
app/src/main/java/dev/libretools/connect/
├── MainActivity.kt                     # Main activity and navigation setup
├── data/
│   └── DeviceModels.kt                 # Data classes and enums
├── ui/
│   ├── components/
│   │   └── CommonComponents.kt         # Reusable UI components
│   ├── screens/
│   │   ├── MainScreen.kt              # Main screen with tab navigation
│   │   ├── DeviceScreens.kt           # Device detail screens
│   │   ├── PluginScreens.kt           # All plugin-specific screens
│   │   └── SettingsScreens.kt         # Settings and about screens
│   └── theme/
│       ├── Color.kt                   # App color scheme
│       ├── Theme.kt                   # Material theme configuration
│       └── Type.kt                    # Typography definitions
```

## File Descriptions

### MainActivity.kt
- Contains the main activity class
- Sets up navigation with NavHost
- Manages sample device data
- Entry point for the application

### data/DeviceModels.kt
- `Device` data class representing connected devices
- `DeviceType` enum for different device categories
- `PluginCapability` enum for available plugin features
- `formatLastSeen()` utility function for timestamp formatting

### ui/components/CommonComponents.kt
- `DeviceCard` - Displays device information in list
- `BatteryIndicator` - Shows battery level and charging status
- `TipCard` - Information cards with icon and description
- `PluginCard` - Plugin capability display with enable/disable state
- `QuickActionButton` - Consistent button style for actions
- `DeviceInfoCard` - Detailed device information display
- `SettingsToggle` - Toggle switch with title and description
- `FeatureItem` - Feature list item with emoji icon

### ui/screens/MainScreen.kt
- `MainScreen` - Main application screen with top bar and tabs
- `DevicesTab` - Tab showing connected devices list
- `DiscoverTab` - Tab for discovering new devices with tips

### ui/screens/DeviceScreens.kt
- `DeviceDetailScreen` - Detailed view of a specific device
- Shows device info and available plugins

### ui/screens/PluginScreens.kt
- `PluginScreen` - Router for different plugin screens
- Individual plugin screens:
  - `ClipboardPluginScreen` - Clipboard synchronization
  - `FileTransferPluginScreen` - File sharing functionality
  - `InputSharePluginScreen` - Remote input control
  - `NotificationsPluginScreen` - Notification mirroring
  - `MediaControlPluginScreen` - Media playback control
  - `BatteryStatusPluginScreen` - Battery monitoring
  - `RemoteCommandsPluginScreen` - Execute remote commands
  - `TouchpadPluginScreen` - Touchpad functionality
  - `PresentationPluginScreen` - Presentation control

### ui/screens/SettingsScreens.kt
- `SettingsScreen` - App settings and preferences
- `AboutScreen` - Modern about screen with hero section, feature grid, and tech stack

## Design Principles

### Separation of Concerns
- Data models are separated from UI components
- Screen logic is isolated in dedicated files
- Common components are reusable across screens

### Composition
- Heavy use of Jetpack Compose for declarative UI
- Component-based architecture for reusability
- Consistent styling through shared components

### Navigation
- Single navigation graph in MainActivity
- Type-safe navigation with arguments
- Clean app bar navigation without bottom tabs
- Floating Action Button for primary actions

### Material Design 3 with Lucide Icons
- Modern Material Design 3 components
- Lucide icons library for consistent and beautiful iconography
- Dynamic color theming support
- Consistent typography and spacing

## Key Features

### Device Management
- Device discovery and connection
- Battery monitoring
- Connection status tracking
- Device type categorization

### Plugin System
- Modular plugin architecture
- Enable/disable individual plugins
- Plugin-specific UI screens
- Capability-based feature detection

### User Experience
- Clean single-screen navigation
- Floating Action Button for device discovery
- Context-aware actions
- Consistent visual design
- Responsive layout with full device width utilization
- Modern card-based layouts with proper spacing

## Building and Running

The app uses:
- Android SDK 36 (compile target)
- Minimum SDK 33
- Kotlin with Jetpack Compose
- Material Design 3
- Lucide Icons for Compose (com.composables:icons-lucide)
- Navigation Compose

To build:
```bash
cd mobile/android
./gradlew compileDebugKotlin
./gradlew assembleDebug
```

## Icon Library

The app uses **Lucide Icons** for a consistent and modern iconography:
- 1,520+ beautiful icons available
- Consistent stroke width and style
- Perfect for modern app interfaces
- Easy integration with Jetpack Compose

### Icon Usage
```kotlin
import com.composables.icons.lucide.*

Icon(Lucide.Settings, contentDescription = "Settings")
Icon(Lucide.Search, contentDescription = "Search")
Icon(Lucide.Smartphone, contentDescription = "Device")
```

## Navigation Changes

### Removed Components
- **Bottom Navigation Bar**: Simplified navigation by removing bottom tabs
- **Search Icon in App Bar**: Removed redundant search action from devices screen

### Enhanced Components
- **Floating Action Button**: Primary method for discovering new devices
- **Back Navigation**: Clear back actions in discovery screen
- **Empty State**: Enhanced empty devices state with helpful tips

## About Screen Design

### Modern Layout Features
- **Hero Section**: Circular app icon with gradient background and version info
- **Full-Width Cards**: Utilizes device width with proper margins (20dp horizontal)
- **Feature Grid**: 3-column grid layout showcasing all app capabilities
- **Tech Stack Section**: Side-by-side cards showing technologies used
- **Visual Hierarchy**: Proper spacing (24dp between sections) and typography scale
- **Responsive Design**: Adapts to different screen sizes while maintaining readability

### Design Elements
- **Color Usage**: Primary container for hero, surface variants for footer
- **Icon Integration**: Lucide icons for section headers with consistent sizing
- **Typography**: Proper scale from headline to body text with appropriate line heights
- **Spacing**: Consistent 24dp vertical spacing, 20dp card padding
- **Content Organization**: Logical grouping of information in digestible sections

## Future Enhancements

- Add actual device communication logic
- Implement real plugin functionality
- Add unit and UI tests
- Improve accessibility support
- Add animations and transitions
- Implement proper error handling
- Consider adding swipe gestures for navigation
- Add dark/light theme toggle
- Implement dynamic theming based on device wallpaper