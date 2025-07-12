# LibreConnect

**LibreConnect** is a secure, decentralized, open-source replacement for KDE Connect — built under the [LibreTools](https://github.com/libre-tools) ecosystem. It enables seamless sharing of input devices (keyboard, mouse), clipboard, files, notifications, and media controls between Linux desktops and mobile devices (Android/iOS) over a local network.

> No central servers. No metadata leaks. Just fast, secure, peer-to-peer control across your devices.

---

## 🔥 Vision

> To create a **modular, privacy-focused, local-first device linking platform** that replaces KDE Connect with a cleaner, faster, and more extensible architecture.

---

## 🧱 Tech Stack

| Layer              | Stack                                                                |
| ------------------ | -------------------------------------------------------------------- |
| **Language**       | Rust (core engine & daemon), Kotlin (Android UI), Tauri (desktop UI) |
| **Transport**      | TLS over TCP / QUIC (`rustls`, `quinn`)                              |
| **Discovery**      | mDNS / Zeroconf (`libmdns`, `bonjour`)                               |
| **Message Format** | JSON / MessagePack (`serde`, `rmp-serde`)                            |
| **Mobile**         | Jetpack Compose (Kotlin) + Rust FFI                                  |
| **Desktop GUI**    | Tauri (Rust backend + HTML UI)                                       |
| **CLI**            | `libreconnect-cli` using `clap`, `tokio`                             |

---

## 📦 Components

### 1. `libreconnectd` (Rust)

| Background daemon for peer discovery, pairing, plugin execution, encrypted communication | ⏳ In Progress | Basic structure and TCP listener added |

### 2. `libreconnect-cli` (Rust)

- Headless tool to pair/unpair devices, send files, manage plugins, view status

### 3. `libreconnect-mobile` (Android)

- Native Android app (Jetpack Compose) for notifications, file transfer, clipboard, and input control

### 4. `libreconnect-gui` (Tauri)

- Desktop tray GUI to manage paired devices, toggle features, view logs

---

## 🔌 Plugin Architecture

All features are modular plugins communicating via a secure message bus.

| Plugin              | Description                                           |
| ------------------- | ----------------------------------------------------- |
| `ping`              | Device reachability check                             |
| `clipboard-sync`    | Clipboard bi-directional sync                         |
| `file-transfer`     | Send files between devices                            |
| `input-share`       | Share keyboard/mouse from PC to Android or another PC |
| `notification-sync` | Mirror Android notifications                          |
| `battery-status`    | Show mobile battery level                             |
| `media-control`     | Control remote media playback                         |
| `remote-commands`   | Execute pre-defined shell commands remotely           |
| `touchpad-mode`     | Use phone as touchpad or virtual keyboard             |
| `slide-control`     | Use phone to control slideshows or presentations      |

---

## 🔐 Security

- **TLS encryption** using self-signed, pinned certificates (`rustls`)
- **Manual pairing approval** to establish trust
- **No cloud**, **no phone-home**, **LAN-only default**
- Fully local-first and **peer-to-peer**

---

## 🌐 Network Model

```text
[PC A] (libreconnectd) <== TLS/QUIC ==> [Mobile] (Android App)
           |                                 |
       CLI / GUI                      Jetpack Compose UI / background service
```

- Discovery via mDNS
- Secure pairing + handshake
- Messaging via encrypted TCP/QUIC
- Plugins use message dispatcher to communicate

---

## 📁 Directory Layout (Planned)

```
libreconnect/
├── daemon/              # libreconnectd - Rust core daemon
├── cli/                 # libreconnect-cli - Rust TUI/CLI
├── gui/                 # Tauri desktop tray app
├── mobile/
│   └── android/         # Native Android App (Jetpack Compose)
├── shared/              # Shared types, protocols, plugin registry
├── plugins/             # Modular plugin implementations
├── docs/
└── README.md
```

---

## 📦 Build Targets

| Target Platform | Support         |
| --------------- | --------------- |
| Linux Desktop   | ✅ Full          |
| Android         | ✅ Full          |
| Windows         | 🔄 Planned      |
| iOS             | 🔄 Partial      |
| macOS           | 🔄 Planned      |
| BSD             | 🔄 Experimental |

---

## ⚠️ Development Status Tracker

| Task/Module                   | Status        | Notes                             |
| ----------------------------- | ------------- | --------------------------------- |
| Project scaffold              | ✅ Done        | Core repo structure               |
| Shared types, protocols, plugin registry | ✅ Done        | Initial data structures defined   |
| Device discovery (mDNS)       | ⏳ In Progress | Using `libmdns`                   |
| Daemon core                   | ✅ Done        | Basic structure and TCP listener added |
| Secure pairing                | ⏳ In Progress | TLS cert exchange                 |
| Clipboard sync                | ⬜ Not Started |                                   |
| File transfer                 | ⬜ Not Started |                                   |
| Input share                   | ⬜ Not Started | PC input to Android over LAN      |
| Mobile app (Android)          | ⬜ Not Started | Android base UI + permissions     |
| CLI tool (`libreconnect-cli`) | ⏳ In Progress | Basic structure and command parsing added |
| GUI (Tauri)                   | ⬜ Not Started | System tray, plugin toggles, logs |
| Plugin dispatcher             | ⏳ In Progress | Basic plugin trait and PingPlugin added |
| Notification mirror           | ⬜ Not Started | Android notification listener     |
| Media control                 | ⬜ Not Started | Spotify/VLC integration           |
| Background service (Android)  | ⬜ Not Started | Long-lived connection             |
| Logging + debug mode          | ⬜ Not Started | Developer logs                    |

---

## 🌟 License

MIT or GPLv3 (TBD)

---

## 🌎 Tags

`libretools` `rust` `kotlin` `jetpack-compose` `p2p` `keyboard-sharing` `kdeconnect` `privacy` `decentralized` `device-control`

