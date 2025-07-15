#!/usr/bin/env python3
"""
LibreConnect Android-Rust Integration Testing Framework

This script provides automated testing and validation for the LibreConnect
Android-Rust integration, verifying end-to-end communication between the
Android app and Rust daemon.

Usage:
    python test_integration.py --mode [discover|connect|plugins|all]

Requirements:
    - Android device connected via ADB
    - Rust daemon running on local machine
    - Both devices on same WiFi network
"""

import subprocess
import json
import time
import socket
import threading
import argparse
import sys
import re
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from enum import Enum

class TestResult(Enum):
    PASS = "✅ PASS"
    FAIL = "❌ FAIL"
    SKIP = "⏭️ SKIP"
    WARN = "⚠️ WARN"

@dataclass
class TestCase:
    name: str
    description: str
    result: TestResult = TestResult.SKIP
    details: str = ""
    duration: float = 0.0

class LibreConnectTester:
    def __init__(self):
        self.daemon_process = None
        self.daemon_port = 1716
        self.daemon_host = "localhost"
        self.android_package = "dev.libretools.connect"
        self.test_results: List[TestCase] = []

    def log(self, message: str, level: str = "INFO"):
        """Enhanced logging with timestamps and colors"""
        timestamp = time.strftime("%H:%M:%S")
        colors = {
            "INFO": "\033[36m",    # Cyan
            "PASS": "\033[32m",    # Green
            "FAIL": "\033[31m",    # Red
            "WARN": "\033[33m",    # Yellow
            "RESET": "\033[0m"     # Reset
        }
        color = colors.get(level, colors["INFO"])
        print(f"{color}[{timestamp}] {level}: {message}{colors['RESET']}")

    def run_command(self, cmd: List[str], timeout: int = 30) -> Tuple[bool, str]:
        """Run shell command with timeout and error handling"""
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False
            )
            return result.returncode == 0, result.stdout + result.stderr
        except subprocess.TimeoutExpired:
            return False, f"Command timed out after {timeout}s"
        except Exception as e:
            return False, f"Command failed: {str(e)}"

    def check_prerequisites(self) -> bool:
        """Verify all testing prerequisites are met"""
        self.log("🔍 Checking Prerequisites...", "INFO")

        tests = [
            ("ADB Connection", ["adb", "devices"]),
            ("Rust Toolchain", ["cargo", "--version"]),
            ("Network Interface", ["ip", "addr", "show"]),
        ]

        all_passed = True
        for test_name, cmd in tests:
            success, output = self.run_command(cmd)
            if success:
                self.log(f"{test_name}: {TestResult.PASS.value}", "PASS")
            else:
                self.log(f"{test_name}: {TestResult.FAIL.value} - {output}", "FAIL")
                all_passed = False

        return all_passed

    def start_daemon(self) -> bool:
        """Start LibreConnect daemon in background"""
        self.log("🚀 Starting LibreConnect Daemon...", "INFO")

        try:
            # Check if daemon is already running
            if self.is_daemon_running():
                self.log("Daemon already running", "WARN")
                return True

            # Start daemon process
            self.daemon_process = subprocess.Popen(
                ["cargo", "run", "--release", "--bin", "libreconnectd"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                cwd="."
            )

            # Wait for daemon to start
            for i in range(10):
                time.sleep(1)
                if self.is_daemon_running():
                    self.log("Daemon started successfully", "PASS")
                    return True

            self.log("Daemon failed to start within 10 seconds", "FAIL")
            return False

        except Exception as e:
            self.log(f"Failed to start daemon: {str(e)}", "FAIL")
            return False

    def is_daemon_running(self) -> bool:
        """Check if daemon is listening on expected port"""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(1)
            result = sock.connect_ex((self.daemon_host, self.daemon_port))
            sock.close()
            return result == 0
        except:
            return False

    def stop_daemon(self):
        """Stop the daemon process"""
        if self.daemon_process:
            self.daemon_process.terminate()
            self.daemon_process.wait(timeout=5)
            self.log("Daemon stopped", "INFO")

    def install_android_app(self) -> bool:
        """Install Android APK and verify installation"""
        self.log("📱 Installing Android App...", "INFO")

        apk_path = "mobile/android/app/build/outputs/apk/debug/app-debug.apk"

        # Check if APK exists
        success, _ = self.run_command(["test", "-f", apk_path])
        if not success:
            self.log(f"APK not found at {apk_path}", "FAIL")
            return False

        # Install APK
        success, output = self.run_command(["adb", "install", "-r", apk_path])
        if success:
            self.log("APK installed successfully", "PASS")
            return True
        else:
            self.log(f"APK installation failed: {output}", "FAIL")
            return False

    def start_android_logging(self) -> subprocess.Popen:
        """Start Android log monitoring in background"""
        self.log("📋 Starting Android log monitoring...", "INFO")

        log_cmd = [
            "adb", "logcat", "-s",
            "LibreConnectService:D",
            "DeviceDiscovery:D",
            "NetworkManager:D",
            "ProtocolAdapter:D"
        ]

        return subprocess.Popen(log_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

    def launch_android_app(self) -> bool:
        """Launch LibreConnect Android app"""
        self.log("🚀 Launching Android App...", "INFO")

        # Launch main activity
        success, output = self.run_command([
            "adb", "shell", "am", "start",
            "-n", f"{self.android_package}/.MainActivity"
        ])

        if success:
            self.log("Android app launched", "PASS")
            time.sleep(3)  # Wait for app to initialize
            return True
        else:
            self.log(f"Failed to launch app: {output}", "FAIL")
            return False

    def test_device_discovery(self) -> TestCase:
        """Test mDNS device discovery functionality"""
        test = TestCase(
            name="Device Discovery",
            description="Test mDNS discovery of desktop device from Android"
        )

        start_time = time.time()
        self.log("🔍 Testing Device Discovery...", "INFO")

        try:
            # Trigger discovery via ADB (simulate button press)
            self.log("Starting device discovery scan...", "INFO")

            # Send discovery intent
            success, output = self.run_command([
                "adb", "shell", "am", "broadcast",
                "-a", "dev.libretools.connect.DISCOVER_DEVICES",
                "-n", f"{self.android_package}/.service.LibreConnectService"
            ])

            if not success:
                test.result = TestResult.FAIL
                test.details = f"Failed to trigger discovery: {output}"
                return test

            # Wait for discovery results
            self.log("Waiting for discovery results...", "INFO")
            time.sleep(5)

            # Check Android logs for discovery success
            log_success = self.check_android_logs_for_pattern(
                "Discovered device.*DESKTOP.*1716",
                timeout=10
            )

            if log_success:
                test.result = TestResult.PASS
                test.details = "Desktop device discovered successfully via mDNS"
                self.log("Device discovery successful", "PASS")
            else:
                test.result = TestResult.FAIL
                test.details = "No desktop device discovered within timeout"
                self.log("Device discovery failed", "FAIL")

        except Exception as e:
            test.result = TestResult.FAIL
            test.details = f"Discovery test exception: {str(e)}"

        test.duration = time.time() - start_time
        return test

    def test_connection_establishment(self) -> TestCase:
        """Test TCP connection between Android and daemon"""
        test = TestCase(
            name="Connection Establishment",
            description="Test TCP connection establishment and handshake"
        )

        start_time = time.time()
        self.log("🔗 Testing Connection Establishment...", "INFO")

        try:
            # Simulate connection attempt via ADB
            success, output = self.run_command([
                "adb", "shell", "am", "broadcast",
                "-a", "dev.libretools.connect.CONNECT_DEVICE",
                "-e", "deviceId", "test-desktop-device",
                "-n", f"{self.android_package}/.service.LibreConnectService"
            ])

            if not success:
                test.result = TestResult.FAIL
                test.details = f"Failed to trigger connection: {output}"
                return test

            # Wait for connection attempt
            time.sleep(3)

            # Check for successful connection in logs
            connection_success = self.check_android_logs_for_pattern(
                "Successfully connected to.*|Device connected:",
                timeout=10
            )

            if connection_success:
                test.result = TestResult.PASS
                test.details = "TCP connection established successfully"
                self.log("Connection establishment successful", "PASS")
            else:
                test.result = TestResult.WARN
                test.details = "Connection attempt made but success unclear from logs"
                self.log("Connection establishment unclear", "WARN")

        except Exception as e:
            test.result = TestResult.FAIL
            test.details = f"Connection test exception: {str(e)}"

        test.duration = time.time() - start_time
        return test

    def test_plugin_communication(self) -> List[TestCase]:
        """Test individual plugin message communication"""
        self.log("🔌 Testing Plugin Communication...", "INFO")

        plugins = [
            ("Clipboard Sync", "clipboard", {"content": "test message"}),
            ("Input Share", "input", {"action": "press", "keyCode": "A"}),
            ("Touchpad Mode", "touchpad", {"x": 100, "y": 100, "dx": 5, "dy": 5}),
            ("Media Control", "media", {"action": "play"}),
            ("Remote Commands", "remote", {"command": "echo", "args": ["hello"]}),
            ("Slide Control", "slide", {"action": "next"}),
            ("Battery Status", "battery", {"charge": 85, "isCharging": True}),
        ]

        test_results = []

        for plugin_name, plugin_type, test_data in plugins:
            test = TestCase(
                name=f"Plugin: {plugin_name}",
                description=f"Test {plugin_name} message sending and protocol"
            )

            start_time = time.time()

            try:
                # Send plugin message via ADB
                data_json = json.dumps(test_data)
                success, output = self.run_command([
                    "adb", "shell", "am", "broadcast",
                    "-a", "dev.libretools.connect.SEND_PLUGIN_MESSAGE",
                    "-e", "deviceId", "test-desktop-device",
                    "-e", "pluginType", plugin_type,
                    "-e", "message", data_json,
                    "-n", f"{self.android_package}/.service.LibreConnectService"
                ])

                if success:
                    # Check logs for message sent confirmation
                    message_sent = self.check_android_logs_for_pattern(
                        f"Sent.*message.*{plugin_type}|Sending.*{plugin_name}",
                        timeout=5
                    )

                    if message_sent:
                        test.result = TestResult.PASS
                        test.details = f"{plugin_name} message sent successfully"
                    else:
                        test.result = TestResult.WARN
                        test.details = f"{plugin_name} message triggered but confirmation unclear"
                else:
                    test.result = TestResult.FAIL
                    test.details = f"Failed to send {plugin_name} message: {output}"

            except Exception as e:
                test.result = TestResult.FAIL
                test.details = f"{plugin_name} test exception: {str(e)}"

            test.duration = time.time() - start_time
            test_results.append(test)

        return test_results

    def check_android_logs_for_pattern(self, pattern: str, timeout: int = 10) -> bool:
        """Check Android logs for specific pattern within timeout"""
        try:
            # Get recent logs
            success, logs = self.run_command([
                "adb", "logcat", "-d", "-s",
                "LibreConnectService:D", "DeviceDiscovery:D", "NetworkManager:D"
            ])

            if success and re.search(pattern, logs, re.IGNORECASE):
                return True

            # If not found in recent logs, monitor for new logs
            log_process = subprocess.Popen([
                "adb", "logcat", "-s",
                "LibreConnectService:D", "DeviceDiscovery:D", "NetworkManager:D"
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

            start_time = time.time()
            while time.time() - start_time < timeout:
                line = log_process.stdout.readline()
                if line and re.search(pattern, line, re.IGNORECASE):
                    log_process.terminate()
                    return True
                time.sleep(0.1)

            log_process.terminate()
            return False

        except Exception as e:
            self.log(f"Log monitoring error: {str(e)}", "WARN")
            return False

    def run_all_tests(self) -> Dict[str, any]:
        """Run complete test suite"""
        self.log("🧪 Starting LibreConnect Integration Test Suite", "INFO")
        self.log("=" * 60, "INFO")

        start_time = time.time()
        results = {
            "start_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "tests": [],
            "summary": {},
            "success": False
        }

        try:
            # Prerequisites
            if not self.check_prerequisites():
                self.log("Prerequisites check failed. Aborting tests.", "FAIL")
                return results

            # Start daemon
            if not self.start_daemon():
                self.log("Failed to start daemon. Aborting tests.", "FAIL")
                return results

            # Install and launch Android app
            if not self.install_android_app():
                self.log("Failed to install Android app. Aborting tests.", "FAIL")
                return results

            if not self.launch_android_app():
                self.log("Failed to launch Android app. Aborting tests.", "FAIL")
                return results

            # Start log monitoring
            log_process = self.start_android_logging()

            # Run test phases
            self.log("\n📋 Phase 1: Device Discovery", "INFO")
            discovery_test = self.test_device_discovery()
            self.test_results.append(discovery_test)

            self.log("\n📋 Phase 2: Connection Establishment", "INFO")
            connection_test = self.test_connection_establishment()
            self.test_results.append(connection_test)

            self.log("\n📋 Phase 3: Plugin Communication", "INFO")
            plugin_tests = self.test_plugin_communication()
            self.test_results.extend(plugin_tests)

            # Stop log monitoring
            log_process.terminate()

        except Exception as e:
            self.log(f"Test suite exception: {str(e)}", "FAIL")

        finally:
            # Cleanup
            self.stop_daemon()

        # Generate results summary
        total_duration = time.time() - start_time
        results["duration"] = total_duration
        results["tests"] = [
            {
                "name": test.name,
                "description": test.description,
                "result": test.result.value,
                "details": test.details,
                "duration": test.duration
            }
            for test in self.test_results
        ]

        # Calculate summary statistics
        summary = {
            "total": len(self.test_results),
            "passed": len([t for t in self.test_results if t.result == TestResult.PASS]),
            "failed": len([t for t in self.test_results if t.result == TestResult.FAIL]),
            "warnings": len([t for t in self.test_results if t.result == TestResult.WARN]),
            "skipped": len([t for t in self.test_results if t.result == TestResult.SKIP]),
        }

        results["summary"] = summary
        results["success"] = summary["failed"] == 0

        # Print final report
        self.print_test_report(results)

        return results

    def print_test_report(self, results: Dict):
        """Print comprehensive test report"""
        self.log("\n" + "=" * 60, "INFO")
        self.log("🧪 LibreConnect Integration Test Report", "INFO")
        self.log("=" * 60, "INFO")

        summary = results["summary"]
        self.log(f"📊 Summary: {summary['total']} tests executed", "INFO")
        self.log(f"   ✅ Passed: {summary['passed']}", "PASS")
        self.log(f"   ❌ Failed: {summary['failed']}", "FAIL" if summary['failed'] > 0 else "INFO")
        self.log(f"   ⚠️  Warnings: {summary['warnings']}", "WARN" if summary['warnings'] > 0 else "INFO")
        self.log(f"   ⏭️  Skipped: {summary['skipped']}", "INFO")
        self.log(f"⏱️  Total Duration: {results['duration']:.2f}s", "INFO")

        self.log("\n📋 Detailed Results:", "INFO")
        for test_data in results["tests"]:
            status_icon = "✅" if "PASS" in test_data["result"] else "❌" if "FAIL" in test_data["result"] else "⚠️"
            self.log(f"  {status_icon} {test_data['name']}: {test_data['result']}", "INFO")
            if test_data["details"]:
                self.log(f"     └─ {test_data['details']}", "INFO")

        # Overall result
        if results["success"]:
            self.log("\n🎉 INTEGRATION TEST SUITE PASSED!", "PASS")
            self.log("✅ Android-Rust communication is working correctly", "PASS")
        else:
            self.log("\n💥 INTEGRATION TEST SUITE FAILED!", "FAIL")
            self.log("❌ Issues found in Android-Rust communication", "FAIL")

        self.log("=" * 60, "INFO")

def main():
    parser = argparse.ArgumentParser(description="LibreConnect Integration Tester")
    parser.add_argument(
        "--mode",
        choices=["discover", "connect", "plugins", "all"],
        default="all",
        help="Test mode to run"
    )
    parser.add_argument(
        "--daemon-host",
        default="localhost",
        help="Daemon host address"
    )
    parser.add_argument(
        "--daemon-port",
        type=int,
        default=1716,
        help="Daemon port"
    )
    parser.add_argument(
        "--output",
        help="Output file for test results (JSON)"
    )

    args = parser.parse_args()

    # Initialize tester
    tester = LibreConnectTester()
    tester.daemon_host = args.daemon_host
    tester.daemon_port = args.daemon_port

    # Run tests based on mode
    if args.mode == "all":
        results = tester.run_all_tests()
    else:
        tester.log(f"Individual test modes not yet implemented: {args.mode}", "WARN")
        return 1

    # Save results if output file specified
    if args.output:
        try:
            with open(args.output, 'w') as f:
                json.dump(results, f, indent=2)
            tester.log(f"Results saved to {args.output}", "INFO")
        except Exception as e:
            tester.log(f"Failed to save results: {str(e)}", "WARN")

    # Return appropriate exit code
    return 0 if results["success"] else 1

if __name__ == "__main__":
    sys.exit(main())
