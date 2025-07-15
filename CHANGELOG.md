# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure with Cargo workspace and Android project.
- Initial data structures defined in `shared` crate.
- Basic `Daemon` struct and TCP listener added to `daemon` crate.
- Basic CLI structure and command parsing added to `cli` crate.
- `PingDaemon` command added to `cli` for daemon interaction.
- `SetClipboard` and `GetClipboard` commands added to `cli` for clipboard synchronization.
- `SendFile` command added to `cli` for file transfer.
- Basic file transfer UI and logic implemented in Android app.
- `SendKeyEvent` and `SendMouseEvent` commands added to `cli` for input sharing.
- Basic input sharing UI and logic implemented in Android app.
- Basic notification mirroring UI and logic implemented in Android app.
- Basic media control UI and logic implemented in Android app.
- `SendRemoteCommand` added to `cli` for remote command execution.
- Basic remote command UI and logic implemented in Android app.
- Basic touchpad mode UI and logic implemented in Android app.
- `SendSlideControl` command added to `cli` for slide control.
- Basic slide control UI and logic implemented in Android app.
- Basic Jetpack Compose UI implemented in Android project (FFI simulated).
- Basic clipboard synchronization UI and logic implemented in Android app.
- Basic auto-acceptance pairing implemented in `daemon` crate.
- Basic plugin dispatcher and `PingPlugin` integrated into `daemon` crate.
- **Complete plugin system implementation** with full system integration:
  - `ClipboardSyncPlugin` with real clipboard access using `arboard`
  - `FileTransferPlugin` with actual file I/O operations and download management
  - `InputSharePlugin` with cross-platform input simulation using `enigo`
  - `NotificationSyncPlugin` with system notification display using `notify-rust`
  - `MediaControlPlugin` with media key simulation
  - `BatteryStatusPlugin` with battery monitoring capabilities
  - `RemoteCommandsPlugin` with secure whitelisted command execution
  - `TouchpadModePlugin` with touchpad-to-mouse simulation
  - `SlideControlPlugin` with presentation control via keyboard shortcuts
- Cross-platform system integration dependencies: `arboard`, `enigo`, `notify-rust`, `dirs`, `battery`, `sysinfo`
- Comprehensive test suite covering all plugin functionality (25+ test cases)
- Security-focused remote command execution with strict command whitelist
- Thread-safe plugin architecture compatible with multi-threaded daemon
- Error handling and graceful degradation for system features
- **Major code refactoring and quality improvements**:
  - Enhanced documentation with comprehensive API docs and module descriptions
  - Improved error handling with custom error types and proper error propagation
  - Code quality improvements addressing all clippy warnings and suggestions
  - Better type safety with helper constructors and validation methods
  - Enhanced CLI with improved user experience and better error messages
  - Daemon refactored with proper connection management and timeout handling
  - Protocol constants and type safety improvements in shared module
  - Default trait implementations and builder patterns for better ergonomics

### Changed
- mDNS integration in `daemon` crate temporarily put on hold due to library issues.
- Rust-Android FFI integration put on hold due to build complexities; FFI is currently simulated in the Android app.
- Updated rustls imports for version 0.22 compatibility with `rustls_pki_types`
- Plugins now provide actual system functionality instead of just logging messages

### Fixed
- Compilation errors with rustls 0.22 API changes
- Missing trait imports for enigo input simulation
- Thread safety issues with battery manager
- Plugin constructor compatibility with daemon initialization

### Removed
- `android_ffi` crate.
