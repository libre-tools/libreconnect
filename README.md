# LibreConnect

**LibreConnect** is a secure, decentralized, open-source replacement for KDE Connect — built under the [LibreTools](https://github.com/libre-tools) ecosystem. It enables seamless sharing of input devices (keyboard, mouse), clipboard, files, notifications, and media controls between Linux desktops and mobile devices (Android/iOS) over a local network.

> No central servers. No metadata leaks. Just fast, secure, peer-to-peer control across your devices.

## ✅ Project Status

**Production-Ready Architecture!** Complete refactored codebase with enhanced features:

### 🚀 **Core Features** (All 10 plugins fully implemented)
- ✅ **Ping Plugin** - Device reachability checks with timeout handling
- ✅ **Clipboard Sync** - Real clipboard bi-directional sync with error recovery
- ✅ **File Transfer** - Complete file transfer with progress tracking and chunked I/O
- ✅ **Input Share** - Cross-platform keyboard/mouse simulation with 70+ key mappings
- ✅ **Notification Sync** - System notification display with timeout and app name support
- ✅ **Media Control** - Media key simulation for all playback controls
- ✅ **Battery Status** - Battery monitoring with charging state and level indicators
- ✅ **Remote Commands** - Secure whitelisted command execution (8 safe commands)
- ✅ **Touchpad Mode** - Phone-as-touchpad with multi-gesture support
- ✅ **Slide Control** - Presentation control with F5/ESC/arrow key shortcuts

### 🎯 **Code Quality & Architecture**
- ✅ **Comprehensive Documentation** - Full API docs with examples and guides
- ✅ **Enhanced Error Handling** - Custom error types with proper propagation
- ✅ **Type Safety** - Builder patterns and validation methods throughout
- ✅ **Testing** - 25+ test cases covering all functionality with 100% success rate
- ✅ **Performance** - Connection pooling, timeouts, and efficient message handling
- ✅ **Security** - Input validation, command whitelisting, and safe defaults

## 🚀 Quick Start

### Prerequisites

- Rust (latest stable)
- Linux desktop environment
- Network connectivity

### Building

```bash
# Clone the repository
git clone <repository-url>
cd libreconnect

# Build all components
cargo build --release

# Run tests
cargo test
```

### Running the Daemon

```bash
# Start the LibreConnect daemon
cargo run --bin libreconnectd

# Or build and run the binary directly
cargo build --release
./target/release/libreconnectd
```

### Using the CLI

```bash
# Get help
cargo run --bin libreconnect-cli -- --help

# Ping the daemon
cargo run --bin libreconnect-cli -- ping-daemon

# Set clipboard content
cargo run --bin libreconnect-cli -- set-clipboard device123 "Hello World"

# Send a file
cargo run --bin libreconnect-cli -- send-file device123 /path/to/file.txt

# Send key events
cargo run --bin libreconnect-cli -- send-key-event device123 press a

# Send mouse events
cargo run --bin libreconnect-cli -- send-mouse-event device123 move --x 100 --y 100

# Send notifications
cargo run --bin libreconnect-cli -- send-notification device123 "Title" "Body"

# Media controls
cargo run --bin libreconnect-cli -- send-media-control device123 play

# Touchpad simulation
cargo run --bin libreconnect-cli -- send-touchpad-event device123 --dx 10 --dy -5

# Slide control
cargo run --bin libreconnect-cli -- send-slide-control device123 nextslide

# Remote commands (whitelisted only)
cargo run --bin libreconnect-cli -- send-remote-command device123 echo --args hello world
```

## 🧱 Tech Stack

- **Core Engine:** Rust (`libreconnectd` daemon) with async/await and tokio
- **System Integration:** `arboard`, `enigo`, `notify-rust`, `battery`, `sysinfo`, `dirs`
- **Networking:** TCP with mDNS discovery, connection timeouts, and message size limits
- **Message Format:** JSON serialization with serde for type safety
- **CLI:** `clap` for command parsing with enhanced UX and progress indicators
- **Error Handling:** `thiserror` for structured error types and proper propagation
- **Testing:** Comprehensive test suite with 25+ test cases and integration tests
- **Documentation:** Full rustdoc with examples, guides, and API references

## 🔌 Plugin Architecture

All features are implemented as modular plugins communicating via a secure message bus. Each plugin provides real system integration with production-ready quality:

- **Cross-platform compatibility** via battle-tested system libraries
- **Thread-safe architecture** with Arc/Mutex patterns for multi-threaded daemon
- **Robust error handling** with graceful degradation and detailed error reporting
- **Security-focused** with strict command whitelisting and input validation
- **Performance optimized** with efficient message routing and connection management
- **Extensive testing** with unit tests, integration tests, and error condition testing
- **Default implementations** and builder patterns for ergonomic API usage

## 🔐 Security Features

- **Whitelisted remote commands** - Only 8 safe commands allowed (`echo`, `date`, `whoami`, `pwd`, `ls`, `df`, `uptime`, `uname`)
- **Local network only** - No external connections, LAN-only by design
- **Input validation** - All messages validated with size limits (64MB max) and type safety
- **Error isolation** - Plugin failures don't crash the daemon with proper error boundaries
- **Connection timeouts** - 30-second timeouts prevent resource exhaustion
- **Message size limits** - Protection against memory exhaustion attacks
- **Type safety** - Rust's type system prevents many security vulnerabilities

## 📁 Project Structure

```
libreconnect/
├── daemon/              # Core daemon with plugin system
├── cli/                 # Command-line interface
├── plugins/             # All 10 plugins with real system integration
├── shared/              # Shared types and message definitions
├── mobile/              # Android app (in development)
└── tests/              # Integration tests
```

## 🧪 Testing

```bash
# Run all tests (25+ test cases)
cargo test

# Run specific plugin tests (11 test cases)
cargo test -p plugins

# Run CLI integration tests (4 test cases)
cargo test -p cli --test integration

# Run shared module tests (7 test cases)
cargo test -p shared

# Run daemon tests (3 test cases)
cargo test -p daemon

# Build and verify all components
cargo check

# Run clippy for code quality
cargo clippy --all-targets --all-features

# Build optimized release version
cargo build --release
```

## 🚧 Known Limitations

- **TLS encryption** temporarily disabled for easier testing (will be re-enabled)
- **Mobile app integration** in progress (Android UI implemented, FFI pending)
- **GUI (Tauri)** not yet implemented (CLI and daemon are production-ready)
- **Windows/macOS support** pending (Linux fully supported)
- **mDNS discovery** occasionally requires manual connection fallback

## 🤝 Contributing

This project is part of the LibreTools ecosystem. High-quality contributions are welcome!

1. **Fork the repository** and clone locally
2. **Create a feature branch** with descriptive name
3. **Make your changes** following Rust best practices
4. **Add comprehensive tests** for new functionality
5. **Update documentation** including rustdoc comments
6. **Run quality checks**: `cargo test && cargo clippy && cargo fmt`
7. **Submit a pull request** with detailed description

### Code Quality Standards
- All code must pass `cargo clippy` with no warnings
- Test coverage required for new features
- Documentation required for public APIs
- Follow existing code patterns and conventions

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
