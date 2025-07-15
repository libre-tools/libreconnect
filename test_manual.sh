#!/bin/bash

# LibreConnect Manual Integration Testing Script
# Simple validation of Android-Rust communication

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DAEMON_PORT=1716
APK_PATH="mobile/android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="dev.libretools.connect"

echo -e "${BLUE}🚀 LibreConnect Manual Integration Test${NC}"
echo -e "${BLUE}======================================${NC}"

# Function to print status
print_status() {
    local status=$1
    local message=$2
    case $status in
        "pass")
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        "fail")
            echo -e "${RED}❌ $message${NC}"
            ;;
        "warn")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ;;
        "info")
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
    esac
}

# Function to check if command exists
check_command() {
    if command -v $1 >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to check if port is in use
check_port() {
    if netstat -ln 2>/dev/null | grep ":$1 " >/dev/null; then
        return 0
    else
        return 1
    fi
}

# Function to get local IP
get_local_ip() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        ip route get 1.1.1.1 | grep -oP 'src \K\S+' 2>/dev/null || hostname -I | awk '{print $1}'
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        route get default | grep interface | awk '{print $2}' | xargs ipconfig getifaddr
    else
        echo "127.0.0.1"
    fi
}

echo -e "\n${BLUE}📋 Step 1: Prerequisites Check${NC}"
echo "=================================="

# Check required commands
COMMANDS=("cargo" "adb" "netstat")
for cmd in "${COMMANDS[@]}"; do
    if check_command $cmd; then
        print_status "pass" "$cmd is available"
    else
        print_status "fail" "$cmd is not installed"
        exit 1
    fi
done

# Check ADB connection
if adb devices | grep -q "device$"; then
    print_status "pass" "Android device connected via ADB"
else
    print_status "fail" "No Android device connected. Connect device and enable USB debugging"
    exit 1
fi

# Check if APK exists
if [[ -f "$APK_PATH" ]]; then
    print_status "pass" "Android APK found at $APK_PATH"
else
    print_status "fail" "Android APK not found. Run: cd mobile/android && ./gradlew assembleDebug"
    exit 1
fi

# Get network information
LOCAL_IP=$(get_local_ip)
print_status "info" "Local IP address: $LOCAL_IP"

echo -e "\n${BLUE}📋 Step 2: Build and Install${NC}"
echo "=============================="

# Build Rust daemon
print_status "info" "Building Rust daemon..."
if cargo build --release --bin libreconnectd; then
    print_status "pass" "Rust daemon built successfully"
else
    print_status "fail" "Failed to build Rust daemon"
    exit 1
fi

# Install Android APK
print_status "info" "Installing Android APK..."
if adb install -r "$APK_PATH" >/dev/null 2>&1; then
    print_status "pass" "Android APK installed successfully"
else
    print_status "fail" "Failed to install Android APK"
    exit 1
fi

echo -e "\n${BLUE}📋 Step 3: Start Services${NC}"
echo "=========================="

# Check if daemon is already running
if check_port $DAEMON_PORT; then
    print_status "warn" "Port $DAEMON_PORT is already in use. Daemon may already be running."
else
    print_status "info" "Starting LibreConnect daemon..."
    print_status "info" "Command: cargo run --release --bin libreconnectd"
    print_status "info" "Starting daemon in background..."

    # Start daemon in background
    nohup cargo run --release --bin libreconnectd > daemon.log 2>&1 &
    DAEMON_PID=$!

    # Wait for daemon to start
    sleep 3

    if check_port $DAEMON_PORT; then
        print_status "pass" "Daemon started successfully (PID: $DAEMON_PID)"
        print_status "info" "Daemon logs: tail -f daemon.log"
    else
        print_status "fail" "Daemon failed to start"
        print_status "info" "Check daemon.log for errors"
        exit 1
    fi
fi

echo -e "\n${BLUE}📋 Step 4: Manual Testing Instructions${NC}"
echo "======================================="

print_status "info" "LibreConnect daemon is running on $LOCAL_IP:$DAEMON_PORT"
print_status "info" "Android app is installed and ready"

echo -e "\n${YELLOW}📱 Manual Testing Steps:${NC}"
echo "1. Launch LibreConnect app on your Android device"
echo "2. Grant all permissions when prompted"
echo "3. Navigate to 'Discovery' screen"
echo "4. Tap 'Scan for Devices'"
echo "5. Look for your desktop device in the list"
echo "6. Tap the discovered device"
echo "7. Tap 'Connect'"
echo "8. Test individual plugins:"
echo "   - Clipboard Sync: Copy text between devices"
echo "   - Input Share: Use phone as keyboard"
echo "   - Touchpad: Use phone as mouse"
echo "   - Media Control: Control desktop media"
echo "   - File Transfer: Send files"
echo "   - Notifications: Send notifications"

echo -e "\n${YELLOW}📊 Monitoring Commands:${NC}"
echo "- Monitor daemon logs: tail -f daemon.log"
echo "- Monitor Android logs: adb logcat -s LibreConnectService:D DeviceDiscovery:D NetworkManager:D"
echo "- Check network connections: netstat -an | grep $DAEMON_PORT"

echo -e "\n${YELLOW}🔍 Expected Results:${NC}"
echo "✅ Device Discovery: Desktop device appears in Android app"
echo "✅ Connection: Status changes to 'Connected'"
echo "✅ Clipboard: Text copied on one device appears on the other"
echo "✅ Input: Typing on phone controls desktop"
echo "✅ Touchpad: Phone movements control desktop mouse"

echo -e "\n${YELLOW}🛠️  Troubleshooting:${NC}"
echo "- If device not discovered: Check firewall and WiFi network"
echo "- If connection fails: Check daemon logs and Android logs"
echo "- If plugins don't work: Verify connection status first"

echo -e "\n${BLUE}📋 Step 5: Cleanup (when done testing)${NC}"
echo "======================================="
echo "To stop the daemon and cleanup:"
echo "1. Press Ctrl+C if daemon is running in foreground"
echo "2. Or kill background daemon: kill $DAEMON_PID"
echo "3. Remove logs: rm daemon.log"

# Create cleanup script
cat > cleanup_test.sh << 'EOF'
#!/bin/bash
echo "🧹 Cleaning up LibreConnect test environment..."
pkill -f libreconnectd
rm -f daemon.log nohup.out
echo "✅ Cleanup complete"
EOF
chmod +x cleanup_test.sh

print_status "info" "Cleanup script created: ./cleanup_test.sh"

echo -e "\n${GREEN}🎉 Setup Complete!${NC}"
echo -e "${GREEN}LibreConnect is ready for manual testing.${NC}"
echo -e "${GREEN}Follow the manual testing steps above.${NC}"

# If daemon was started by this script, show PID for cleanup
if [[ -n "$DAEMON_PID" ]]; then
    echo -e "\n${YELLOW}💡 Remember:${NC}"
    echo "- Daemon PID: $DAEMON_PID"
    echo "- To stop daemon: kill $DAEMON_PID"
    echo "- Or use cleanup script: ./cleanup_test.sh"
fi

echo -e "\n${BLUE}Happy Testing! 🚀${NC}"
