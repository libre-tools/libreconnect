# LibreConnect Android - Quick Start Guide

Get up and running with LibreConnect Android in minutes!

## 🚀 Quick Setup

### 1. Prerequisites
- Android device with API level 33+ (Android 13+)
- WiFi network connection
- LibreConnect daemon running on target devices

### 2. Installation
```bash
# Clone the repository
git clone https://github.com/libretools/libreconnect
cd libreconnect/mobile/android

# Build the APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. First Launch
1. **Open LibreConnect** on your Android device
2. **Grant permissions** when prompted (network, notifications, file access)
3. **Tap the + button** to discover devices
4. **Connect to discovered devices** from the list

## 📱 Basic Usage

### Discover Devices
- Tap the **floating + button** on the main screen
- Press **"Start Scanning"** to find LibreConnect devices
- Devices appear automatically in the main list

### Connect to a Device
- Tap any **discovered device** from the list
- View **available plugins** for that device
- Tap a **plugin** to open its interface

### Use Plugins

#### 📋 Clipboard Sync
- View clipboard content from connected device
- Send text from your phone to the device
- Automatic sync when enabled

#### 📁 File Transfer
- Tap **"Browse Files"** to select files
- Choose files and tap **"Send File"**
- Monitor transfer progress

#### 🖱️ Remote Input
- Use **touchpad area** for mouse control
- Tap **left/right click** buttons
- Access **virtual keyboard** for typing

#### 🎵 Media Control
- Control **play/pause/skip** on connected device
- Adjust **volume** with slider
- See **current track** information

## ⚙️ Configuration

### Network Settings
- **Default Port**: 1716
- **Discovery**: Automatic via mDNS
- **Protocol**: TCP with JSON messaging

### Permissions Setup
Enable these permissions for full functionality:
- **Network Access** - Device discovery and communication
- **File Access** - File transfer capabilities  
- **Notifications** - System notification mirroring
- **Foreground Service** - Background connectivity

## 🔧 Troubleshooting

### No Devices Found
- ✅ Check WiFi connection on both devices
- ✅ Ensure LibreConnect daemon is running on target device
- ✅ Verify same network (no guest networks)
- ✅ Check firewall settings (allow port 1716)

### Connection Issues
- 🔄 Try restarting the discovery scan
- 🔄 Toggle WiFi off/on on both devices
- 🔄 Restart the LibreConnect service

### App Crashes
- 📋 Check Android logs: `adb logcat | grep LibreConnect`
- 🔄 Clear app data and restart
- 📱 Ensure Android 13+ (API 33+)

## 🎯 Pro Tips

### Optimize Performance
- Keep devices on **same WiFi network**
- Use **5GHz WiFi** for better performance
- Close unnecessary apps for stable connections

### Security Best Practices
- Only connect to **trusted devices**
- Use **secure WiFi networks**
- Review **remote command** permissions

### Background Operation
- The app runs a **foreground service** for persistent connections
- **Notification** shows connection status
- Service automatically **reconnects** on network changes

## 📊 Usage Statistics

The app provides real-time information:
- **Connection Status** - In the top bar
- **Device Count** - Number of discovered/connected devices
- **Plugin Availability** - Per-device capabilities
- **Battery Levels** - For connected devices

## 🚨 Common Scenarios

### Home Network Setup
1. Connect phone and computer to same WiFi
2. Start LibreConnect daemon on computer
3. Open Android app and discover
4. Enjoy seamless connectivity!

### Office Environment
1. Ensure corporate firewall allows port 1716
2. Use same network segment
3. Check with IT for mDNS support
4. Consider mobile hotspot if needed

### Development/Testing
1. Use `adb` for debugging: `adb logcat | grep LibreConnect`
2. Mock discovery available for testing
3. Service connection status in logs
4. Network traffic visible in debug builds

## 📚 Next Steps

- **Explore all plugins** - Each offers unique functionality
- **Customize settings** - Adjust notifications and behavior  
- **Read full documentation** - See README.md for complete details
- **Report issues** - Help improve LibreConnect

## 🔗 Useful Commands

```bash
# View app logs
adb logcat | grep LibreConnect

# Clear app data
adb shell pm clear dev.libretools.connect

# Check network connectivity
adb shell ping [target-ip]

# Force stop app
adb shell am force-stop dev.libretools.connect
```

## 💡 Getting Help

- **Check logs** for error messages
- **Review permissions** if features don't work
- **Restart both devices** for connectivity issues
- **Update to latest version** for bug fixes

---

**Ready to connect? Tap that + button and discover your devices! 🚀**