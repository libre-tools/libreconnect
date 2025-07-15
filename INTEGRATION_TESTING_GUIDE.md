# LibreConnect Android-Rust Integration Testing Guide

## 🎯 Overview

This guide provides comprehensive testing procedures for validating the LibreConnect Android-Rust integration. Follow these steps to ensure all components work correctly after replacing mock data with real device communication.

## 📋 Prerequisites

### Hardware Requirements
- **Desktop/Laptop**: Linux, Windows, or macOS with Rust installed
- **Android Device**: Android 13+ with USB debugging enabled
- **Network**: Both devices on same WiFi network

### Software Requirements
- **Rust**: Latest stable version (`curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs/ | sh`)
- **Android Studio**: For APK installation and debugging
- **ADB**: Android Debug Bridge for device communication

### Initial Setup
```bash
# Clone and prepare LibreConnect
git clone <libreconnect-repo>
cd libreconnect

# Build Rust daemon
cargo build --release --bin libreconnectd

# Build Android APK
cd mobile/android
./gradlew assembleDebug
```

## 🔧 Phase 1: Basic Connectivity Testing

### Step 1: Network Preparation

1. **Verify Network Connectivity**
   ```bash
   # On desktop, find IP address
   ip addr show | grep inet
   # Note your WiFi IP (e.g., 192.168.1.100)
   
   # On Android, check WiFi connection
   # Settings → WiFi → [Your Network] → Check IP
   # Should be same subnet (e.g., 192.168.1.xxx)
   ```

2. **Test Network Reachability**
   ```bash
   # From desktop, ping Android
   ping [ANDROID_IP]
   
   # From Android terminal app
   ping [DESKTOP_IP]
   ```

### Step 2: Start Rust Daemon

1. **Launch LibreConnect Daemon**
   ```bash
   cd libreconnect
   RUST_LOG=debug cargo run --bin libreconnectd
   ```

2. **Verify Startup Messages**
   Look for these log entries:
   ```
   🚀 LibreConnect Daemon starting...
   📱 Local device: [Your Device Name]
   🔌 Available plugins: 9
   🔍 mDNS discovery started
   ✅ LibreConnect Daemon started successfully
   🌐 Listening on port 1716
   ```

3. **Test mDNS Service Registration**
   ```bash
   # On Linux/macOS, verify mDNS service
   avahi-browse -rt _libreconnect._tcp.local.
   # Should show your desktop device
   ```

### Step 3: Install and Launch Android App

1. **Install APK**
   ```bash
   cd libreconnect/mobile/android
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Enable Logging**
   ```bash
   # Open separate terminal for Android logs
   adb logcat -s LibreConnectService:D DeviceDiscovery:D NetworkManager:D
   ```

3. **Launch App**
   - Open LibreConnect on Android device
   - Grant all permissions when prompted
   - Navigate to "Discovery" screen

### Step 4: Device Discovery Test

1. **Start Discovery**
   - Tap "Scan for Devices" in Android app
   - Watch for discovery logs in Android logcat:
   ```
   D/DeviceDiscovery: Starting real mDNS device discovery
   D/DeviceDiscovery: JmDNS initialized on [IP]
   D/DeviceDiscovery: Service resolved: [Device Name]
   I/DeviceDiscovery: Discovered device: [Name] (DESKTOP) at [IP]:1716
   ```

2. **Verify Device Appears**
   - Desktop device should appear in discovered devices list
   - Check device name, type (Desktop/Laptop), and IP address
   - Status should show "Not Connected"

3. **Troubleshooting Discovery Issues**
   ```bash
   # If device not discovered, check:
   
   # 1. Firewall (disable temporarily)
   sudo ufw disable  # Ubuntu
   
   # 2. mDNS service on Android
   adb logcat -s jmdns:D
   
   # 3. Network connectivity
   ping [OTHER_DEVICE_IP]
   
   # 4. Port availability
   netstat -ln | grep 1716
   ```

## 🔗 Phase 2: Connection Testing

### Step 1: Establish Connection

1. **Connect to Device**
   - In Android app, tap discovered device
   - Tap "Connect" button
   - Watch connection logs:

   **Android logs:**
   ```
   D/NetworkManager: Connecting to [Device] at [IP]:1716
   D/NetworkManager: Successfully connected to [Device]
   D/LibreConnectService: Device connected: [Device]
   ```

   **Rust daemon logs:**
   ```
   📨 Client connected from [ANDROID_IP]
   📨 Received message from [ANDROID_IP]: DeviceInfo
   ✅ Device paired: [Android Device]
   ```

2. **Verify Connection Status**
   - Device status should change to "Connected"
   - Connection indicator should show green/connected state

### Step 2: Basic Message Exchange

1. **Ping Test**
   - In Android app, go to connected device details
   - Look for ping/connectivity test option
   - Expected logs:
   ```
   # Android
   D/NetworkManager: Sent message to [Device]
   D/NetworkManager: Received message from [Device]: Pong
   
   # Rust
   📨 Received message: Ping
   📨 Sent response: Pong
   ```

2. **Heartbeat Verification**
   - Leave connection active for 30+ seconds
   - Verify heartbeat messages in logs (every 30 seconds)

## 🔌 Phase 3: Plugin Functionality Testing

### Test 1: Clipboard Sync Plugin

1. **Android → Desktop**
   ```bash
   # Preparation
   # Open text editor on desktop (notepad, gedit, etc.)
   
   # On Android
   # 1. Go to Clipboard Sync plugin
   # 2. Type test message: "Hello from Android!"
   # 3. Tap "Send to Desktop"
   
   # On Desktop
   # 4. Paste (Ctrl+V) in text editor
   # 5. Verify "Hello from Android!" appears
   ```

2. **Desktop → Android**
   ```bash
   # On Desktop
   # 1. Copy text: "Hello from Desktop!"
   
   # On Android  
   # 2. In Clipboard Sync plugin, tap "Request Clipboard"
   # 3. Verify text appears in app
   # 4. Long-press text field, paste
   # 5. Verify "Hello from Desktop!" appears
   ```

### Test 2: Input Share Plugin

1. **Keyboard Input Test**
   ```bash
   # Preparation
   # Open text editor on desktop
   
   # On Android
   # 1. Go to Input Share plugin
   # 2. Tap "Enable Keyboard"
   # 3. Type: "Testing remote keyboard"
   # 4. Text should appear in desktop editor
   ```

2. **Special Keys Test**
   ```bash
   # Test these keys from Android:
   # - Enter (new line)
   # - Backspace (delete characters)
   # - Arrow keys (cursor movement)
   # - Ctrl+A (select all)
   # - Ctrl+C (copy)
   ```

### Test 3: Touchpad Mode Plugin

1. **Mouse Movement**
   ```bash
   # On Android
   # 1. Go to Touchpad Mode plugin
   # 2. Enable touchpad
   # 3. Move finger around touchpad area
   # 4. Verify mouse cursor moves on desktop
   ```

2. **Click Actions**
   ```bash
   # Test these actions:
   # - Single tap (left click)
   # - Two-finger tap (right click)
   # - Scroll gestures (if implemented)
   ```

### Test 4: File Transfer Plugin

1. **Send File to Desktop**
   ```bash
   # On Android
   # 1. Go to File Transfer plugin
   # 2. Tap "Select File"
   # 3. Choose small test file (image, document)
   # 4. Tap "Send"
   # 5. Monitor transfer progress
   
   # On Desktop
   # 6. Check Downloads folder for received file
   # 7. Verify file integrity (open, compare)
   ```

### Test 5: Media Control Plugin

1. **Preparation**
   ```bash
   # On Desktop
   # Open media player (VLC, Spotify, YouTube, etc.)
   # Start playing music/video
   ```

2. **Control Test**
   ```bash
   # On Android
   # 1. Go to Media Control plugin
   # 2. Test buttons: Play, Pause, Next, Previous
   # 3. Test volume: Volume Up, Volume Down
   # 4. Verify desktop media responds to commands
   ```

### Test 6: Notification Sync Plugin

1. **Send Notification**
   ```bash
   # On Android
   # 1. Go to Notification Sync plugin
   # 2. Enter title: "Test Notification"
   # 3. Enter message: "This is a test from Android"
   # 4. Tap "Send"
   
   # On Desktop
   # 5. Check for system notification
   # 6. Verify title and message content
   ```

### Test 7: Battery Status Plugin

1. **Battery Information**
   ```bash
   # On Android
   # 1. Go to Battery Status plugin
   # 2. Tap "Send Battery Info"
   
   # Check logs for battery data:
   D/NetworkManager: Battery status sent: XX%
   
   # On Desktop
   # 3. Verify battery info received in daemon logs
   ```

### Test 8: Remote Commands Plugin

1. **Safe Command Test**
   ```bash
   # On Android
   # 1. Go to Remote Commands plugin
   # 2. Select safe command like "echo Hello"
   # 3. Tap "Execute"
   
   # On Desktop
   # 4. Check daemon logs for command execution
   # 5. Verify output in logs
   ```

2. **Security Test**
   ```bash
   # Verify dangerous commands are blocked:
   # - rm, del, format commands should be rejected
   # - Only whitelisted commands should execute
   ```

### Test 9: Slide Control Plugin

1. **Presentation Test**
   ```bash
   # Preparation
   # On Desktop: Open presentation software (LibreOffice Impress, PowerPoint)
   # Start slideshow mode (F5)
   
   # On Android
   # 1. Go to Slide Control plugin
   # 2. Test buttons:
   #    - Next Slide (should advance)
   #    - Previous Slide (should go back)
   #    - End Presentation (should exit slideshow)
   ```

## 🛠️ Phase 4: Error Handling & Edge Cases

### Test 1: Network Disconnection

1. **WiFi Disconnection Test**
   ```bash
   # During active session:
   # 1. Disconnect Android from WiFi
   # 2. Verify app shows "Connection Lost"
   # 3. Reconnect to WiFi
   # 4. Verify automatic reconnection or manual reconnect option
   ```

2. **Desktop Network Change**
   ```bash
   # While connected:
   # 1. Restart desktop network interface
   # 2. Check if Android detects disconnection
   # 3. Test rediscovery and reconnection
   ```

### Test 2: Service Recovery

1. **Android App Recovery**
   ```bash
   # 1. Kill Android app completely
   # 2. Restart app
   # 3. Verify services restart properly
   # 4. Test device rediscovery and reconnection
   ```

2. **Daemon Recovery**
   ```bash
   # 1. Stop Rust daemon (Ctrl+C)
   # 2. Restart daemon
   # 3. Verify Android detects new service
   # 4. Test reconnection capability
   ```

### Test 3: Multiple Device Scenarios

1. **Multiple Desktop Devices**
   ```bash
   # If you have multiple computers:
   # 1. Start daemon on second computer
   # 2. Verify Android discovers both devices
   # 3. Test connecting to both simultaneously
   ```

## 📊 Test Results Documentation

### Create Test Report
Document your results using this template:

```markdown
# LibreConnect Integration Test Report

## Test Environment
- **Date**: [Date]
- **Android Device**: [Model, Android Version]
- **Desktop OS**: [OS and Version]
- **Network**: [WiFi Type, Speed]
- **APK Version**: [Git commit/version]
- **Daemon Version**: [Git commit/version]

## Test Results

### Phase 1: Basic Connectivity ✅/❌
- [ ] Network connectivity
- [ ] Daemon startup
- [ ] mDNS discovery
- [ ] Device connection

### Phase 2: Plugin Testing ✅/❌
- [ ] Clipboard Sync
- [ ] Input Share
- [ ] Touchpad Mode
- [ ] File Transfer
- [ ] Media Control
- [ ] Notification Sync
- [ ] Battery Status
- [ ] Remote Commands
- [ ] Slide Control

### Phase 3: Error Handling ✅/❌
- [ ] Network disconnection recovery
- [ ] Service recovery
- [ ] Multiple device handling

## Issues Found
- [List any bugs, errors, or unexpected behavior]

## Performance Notes
- Connection time: [X seconds]
- Message latency: [X milliseconds]
- File transfer speed: [X MB/s]
```

## 🚨 Common Issues & Solutions

### Issue: Device Not Discovered
**Symptoms**: Android doesn't find desktop device
**Solutions**:
```bash
# Check firewall
sudo ufw status  # Ubuntu
# Disable temporarily if blocking

# Verify mDNS service
systemctl status avahi-daemon  # Linux

# Check WiFi networks match
ip route | grep default  # Desktop
# Compare with Android WiFi settings
```

### Issue: Connection Refused
**Symptoms**: Connection fails with "Connection refused"
**Solutions**:
```bash
# Check if daemon is running
ps aux | grep libreconnectd

# Verify port is listening
netstat -ln | grep 1716

# Check firewall rules
iptables -L | grep 1716
```

### Issue: Messages Not Received
**Symptoms**: Plugin actions don't work
**Solutions**:
```bash
# Check message logs
adb logcat -s NetworkManager:D

# Verify protocol compatibility
# Compare Android ProtocolAdapter with Rust shared types

# Test with simple ping first
```

## ✅ Success Criteria

The integration is considered successful when:

1. **Discovery**: Android consistently discovers desktop devices via mDNS
2. **Connection**: Stable TCP connection establishment within 5 seconds
3. **Plugins**: All 9 plugins function correctly with <1 second latency
4. **Recovery**: Automatic reconnection after network interruptions
5. **Stability**: No crashes during 10-minute continuous usage
6. **Performance**: File transfer >1MB/s, input latency <100ms

## 🎉 Next Steps After Successful Testing

1. **Security Enhancement**: Implement device pairing with user approval
2. **Performance Optimization**: Reduce connection time and message latency
3. **UI Polish**: Add better connection status indicators and error messages
4. **Advanced Features**: Screen sharing, advanced file browser
5. **iOS Development**: Begin iOS client implementation
6. **Production Deployment**: Prepare for release builds and distribution

---

**Testing Complete**: When all phases pass, the Android-Rust integration is production-ready! 🚀