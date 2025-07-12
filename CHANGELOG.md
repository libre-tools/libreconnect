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
- Basic `Plugin` trait and `PingPlugin` implementation added to `plugins` crate.
- Basic Jetpack Compose UI implemented in Android project (FFI simulated).
- Basic clipboard synchronization UI and logic implemented in Android app.
- Basic auto-acceptance pairing implemented in `daemon` crate.
- Basic plugin dispatcher and `PingPlugin` integrated into `daemon` crate.

### Changed
- mDNS integration in `daemon` crate temporarily put on hold due to library issues.
- Rust-Android FFI integration put on hold due to build complexities; FFI is currently simulated in the Android app.

### Removed
- `android_ffi` crate.
