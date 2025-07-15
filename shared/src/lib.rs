//! LibreConnect Shared Types and Messages
//!
//! This module contains all shared data structures, message types, and protocol definitions
//! used across the LibreConnect ecosystem for communication between devices and plugins.

use serde::{Deserialize, Serialize};
use std::fmt;

/// Unique identifier for a device in the LibreConnect network
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct DeviceId(String);

impl DeviceId {
    /// Create a new DeviceId from a string
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }

    /// Get the inner string representation
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl From<&str> for DeviceId {
    fn from(s: &str) -> Self {
        Self::new(s)
    }
}

impl From<String> for DeviceId {
    fn from(s: String) -> Self {
        Self::new(s)
    }
}

impl fmt::Display for DeviceId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Type of device participating in the LibreConnect network
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum DeviceType {
    /// Mobile device (phone, tablet)
    Mobile,
    /// Desktop computer
    Desktop,
}

impl fmt::Display for DeviceType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            DeviceType::Mobile => write!(f, "Mobile"),
            DeviceType::Desktop => write!(f, "Desktop"),
        }
    }
}

/// Information about a device in the network
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct DeviceInfo {
    /// Unique device identifier
    pub id: DeviceId,
    /// Human-readable device name
    pub name: String,
    /// Type of device
    pub device_type: DeviceType,
    /// List of supported plugin capabilities
    pub capabilities: Vec<PluginType>,
}

impl DeviceInfo {
    /// Create new device info
    pub fn new(
        id: impl Into<DeviceId>,
        name: impl Into<String>,
        device_type: DeviceType,
        capabilities: Vec<PluginType>,
    ) -> Self {
        Self {
            id: id.into(),
            name: name.into(),
            device_type,
            capabilities,
        }
    }

    /// Check if device supports a specific plugin
    pub fn supports_plugin(&self, plugin_type: &PluginType) -> bool {
        self.capabilities.contains(plugin_type)
    }
}

/// Core message types for LibreConnect protocol
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum Message {
    // === Core Protocol Messages ===
    /// Ping message for connectivity testing
    Ping,
    /// Pong response to ping
    Pong,
    /// Device information broadcast
    DeviceInfo(DeviceInfo),
    /// Request to pair with another device
    RequestPairing(DeviceInfo),
    /// Accept pairing request
    PairingAccepted(DeviceId),
    /// Reject pairing request
    PairingRejected(DeviceId),

    // === Plugin Messages ===
    /// Synchronize clipboard content
    ClipboardSync(String),
    /// Request current clipboard content
    RequestClipboard,

    /// File transfer request
    FileTransferRequest { file_name: String, file_size: u64 },
    /// File transfer data chunk
    FileTransferChunk {
        file_name: String,
        chunk: Vec<u8>,
        offset: u64,
    },
    /// File transfer completion
    FileTransferEnd { file_name: String },
    /// File transfer error
    FileTransferError { file_name: String, error: String },

    /// Keyboard input event
    KeyEvent(KeyEvent),
    /// Mouse input event
    MouseEvent(MouseEvent),
    /// Touchpad input event
    TouchpadEvent(TouchpadEvent),

    /// System notification
    Notification {
        title: String,
        body: String,
        app_name: Option<String>,
    },

    /// Media control command
    MediaControl { action: MediaControlAction },

    /// Battery status information
    BatteryStatus(BatteryStatus),

    /// Remote command execution
    RemoteCommand { command: String, args: Vec<String> },

    /// Slide presentation control
    SlideControl(SlideControlAction),
}

// === Input Event Types ===

/// Keyboard key action type
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum KeyAction {
    /// Key press down
    Press,
    /// Key release up
    Release,
}

/// Keyboard key codes
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum KeyCode {
    // Letters
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,

    // Numbers
    Key0,
    Key1,
    Key2,
    Key3,
    Key4,
    Key5,
    Key6,
    Key7,
    Key8,
    Key9,

    // Function keys
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12,

    // Special keys
    Escape,
    Tab,
    CapsLock,
    LeftShift,
    LeftControl,
    LeftAlt,
    Space,
    RightAlt,
    RightControl,
    RightShift,
    Enter,
    Backspace,
    Delete,

    // Arrow keys
    ArrowLeft,
    ArrowRight,
    ArrowUp,
    ArrowDown,

    /// Unknown or platform-specific key code
    Unknown(u32),
}

impl fmt::Display for KeyCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            KeyCode::A => write!(f, "A"),
            KeyCode::B => write!(f, "B"),
            KeyCode::C => write!(f, "C"),
            KeyCode::D => write!(f, "D"),
            KeyCode::E => write!(f, "E"),
            KeyCode::F => write!(f, "F"),
            KeyCode::G => write!(f, "G"),
            KeyCode::H => write!(f, "H"),
            KeyCode::I => write!(f, "I"),
            KeyCode::J => write!(f, "J"),
            KeyCode::K => write!(f, "K"),
            KeyCode::L => write!(f, "L"),
            KeyCode::M => write!(f, "M"),
            KeyCode::N => write!(f, "N"),
            KeyCode::O => write!(f, "O"),
            KeyCode::P => write!(f, "P"),
            KeyCode::Q => write!(f, "Q"),
            KeyCode::R => write!(f, "R"),
            KeyCode::S => write!(f, "S"),
            KeyCode::T => write!(f, "T"),
            KeyCode::U => write!(f, "U"),
            KeyCode::V => write!(f, "V"),
            KeyCode::W => write!(f, "W"),
            KeyCode::X => write!(f, "X"),
            KeyCode::Y => write!(f, "Y"),
            KeyCode::Z => write!(f, "Z"),
            KeyCode::Key0 => write!(f, "0"),
            KeyCode::Key1 => write!(f, "1"),
            KeyCode::Key2 => write!(f, "2"),
            KeyCode::Key3 => write!(f, "3"),
            KeyCode::Key4 => write!(f, "4"),
            KeyCode::Key5 => write!(f, "5"),
            KeyCode::Key6 => write!(f, "6"),
            KeyCode::Key7 => write!(f, "7"),
            KeyCode::Key8 => write!(f, "8"),
            KeyCode::Key9 => write!(f, "9"),
            KeyCode::F1 => write!(f, "F1"),
            KeyCode::F2 => write!(f, "F2"),
            KeyCode::F3 => write!(f, "F3"),
            KeyCode::F4 => write!(f, "F4"),
            KeyCode::F5 => write!(f, "F5"),
            KeyCode::F6 => write!(f, "F6"),
            KeyCode::F7 => write!(f, "F7"),
            KeyCode::F8 => write!(f, "F8"),
            KeyCode::F9 => write!(f, "F9"),
            KeyCode::F10 => write!(f, "F10"),
            KeyCode::F11 => write!(f, "F11"),
            KeyCode::F12 => write!(f, "F12"),
            KeyCode::Escape => write!(f, "Escape"),
            KeyCode::Tab => write!(f, "Tab"),
            KeyCode::CapsLock => write!(f, "CapsLock"),
            KeyCode::LeftShift => write!(f, "LeftShift"),
            KeyCode::LeftControl => write!(f, "LeftControl"),
            KeyCode::LeftAlt => write!(f, "LeftAlt"),
            KeyCode::Space => write!(f, "Space"),
            KeyCode::RightAlt => write!(f, "RightAlt"),
            KeyCode::RightControl => write!(f, "RightControl"),
            KeyCode::RightShift => write!(f, "RightShift"),
            KeyCode::Enter => write!(f, "Enter"),
            KeyCode::Backspace => write!(f, "Backspace"),
            KeyCode::Delete => write!(f, "Delete"),
            KeyCode::ArrowLeft => write!(f, "ArrowLeft"),
            KeyCode::ArrowRight => write!(f, "ArrowRight"),
            KeyCode::ArrowUp => write!(f, "ArrowUp"),
            KeyCode::ArrowDown => write!(f, "ArrowDown"),
            KeyCode::Unknown(code) => write!(f, "Unknown({code})"),
        }
    }
}

/// Keyboard input event
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct KeyEvent {
    /// Type of key action
    pub action: KeyAction,
    /// Key that was pressed/released
    pub code: KeyCode,
}

impl KeyEvent {
    /// Create a new key event
    pub fn new(action: KeyAction, code: KeyCode) -> Self {
        Self { action, code }
    }
}

/// Mouse action type
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum MouseAction {
    /// Mouse movement
    Move,
    /// Mouse button press
    Press,
    /// Mouse button release
    Release,
    /// Mouse scroll wheel
    Scroll,
}

/// Mouse button identifier
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum MouseButton {
    /// Left mouse button
    Left,
    /// Right mouse button
    Right,
    /// Middle mouse button (scroll wheel)
    Middle,
    /// Other mouse button with numeric identifier
    Other(u32),
}

/// Mouse input event
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct MouseEvent {
    /// Type of mouse action
    pub action: MouseAction,
    /// X coordinate
    pub x: i32,
    /// Y coordinate
    pub y: i32,
    /// Mouse button (for press/release actions)
    pub button: Option<MouseButton>,
    /// Scroll wheel delta (for scroll actions)
    pub scroll_delta: Option<f32>,
}

impl MouseEvent {
    /// Create a new mouse movement event
    pub fn movement(x: i32, y: i32) -> Self {
        Self {
            action: MouseAction::Move,
            x,
            y,
            button: None,
            scroll_delta: None,
        }
    }

    /// Create a new mouse button press event
    pub fn button_press(x: i32, y: i32, button: MouseButton) -> Self {
        Self {
            action: MouseAction::Press,
            x,
            y,
            button: Some(button),
            scroll_delta: None,
        }
    }

    /// Create a new mouse button release event
    pub fn button_release(x: i32, y: i32, button: MouseButton) -> Self {
        Self {
            action: MouseAction::Release,
            x,
            y,
            button: Some(button),
            scroll_delta: None,
        }
    }

    /// Create a new mouse scroll event
    pub fn scroll(x: i32, y: i32, delta: f32) -> Self {
        Self {
            action: MouseAction::Scroll,
            x,
            y,
            button: None,
            scroll_delta: Some(delta),
        }
    }
}

/// Touchpad input event (for phone-as-touchpad functionality)
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct TouchpadEvent {
    /// Absolute X position (normalized 0.0-1.0)
    pub x: f32,
    /// Absolute Y position (normalized 0.0-1.0)
    pub y: f32,
    /// Relative X movement delta
    pub dx: f32,
    /// Relative Y movement delta
    pub dy: f32,
    /// Horizontal scroll delta
    pub scroll_delta_x: f32,
    /// Vertical scroll delta
    pub scroll_delta_y: f32,
    /// Left click state
    pub is_left_click: bool,
    /// Right click state
    pub is_right_click: bool,
}

impl TouchpadEvent {
    /// Create a new touchpad movement event
    pub fn movement(x: f32, y: f32, dx: f32, dy: f32) -> Self {
        Self {
            x,
            y,
            dx,
            dy,
            scroll_delta_x: 0.0,
            scroll_delta_y: 0.0,
            is_left_click: false,
            is_right_click: false,
        }
    }

    /// Create a new touchpad scroll event
    pub fn scroll(x: f32, y: f32, scroll_x: f32, scroll_y: f32) -> Self {
        Self {
            x,
            y,
            dx: 0.0,
            dy: 0.0,
            scroll_delta_x: scroll_x,
            scroll_delta_y: scroll_y,
            is_left_click: false,
            is_right_click: false,
        }
    }

    /// Create a new touchpad click event
    pub fn click(x: f32, y: f32, is_right_click: bool) -> Self {
        Self {
            x,
            y,
            dx: 0.0,
            dy: 0.0,
            scroll_delta_x: 0.0,
            scroll_delta_y: 0.0,
            is_left_click: !is_right_click,
            is_right_click,
        }
    }
}

// === System Status Types ===

/// Battery status information
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct BatteryStatus {
    /// Battery charge level (0.0-100.0)
    pub charge: f32,
    /// Whether the battery is currently charging
    pub is_charging: bool,
}

impl BatteryStatus {
    /// Create a new battery status
    pub fn new(charge: f32, is_charging: bool) -> Self {
        Self {
            charge: charge.clamp(0.0, 100.0),
            is_charging,
        }
    }

    /// Check if battery is critically low (< 10%)
    pub fn is_critical(&self) -> bool {
        self.charge < 10.0
    }

    /// Check if battery is low (< 20%)
    pub fn is_low(&self) -> bool {
        self.charge < 20.0
    }
}

impl fmt::Display for BatteryStatus {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let status = if self.is_charging {
            "Charging"
        } else {
            "Discharging"
        };
        write!(f, "{:.1}% ({})", self.charge, status)
    }
}

// === Media Control Types ===

/// Media control actions
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum MediaControlAction {
    /// Start playback
    Play,
    /// Pause playback
    Pause,
    /// Toggle play/pause
    PlayPause,
    /// Next track/media
    Next,
    /// Previous track/media
    Previous,
    /// Increase volume
    VolumeUp,
    /// Decrease volume
    VolumeDown,
    /// Toggle mute
    ToggleMute,
}

impl fmt::Display for MediaControlAction {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            MediaControlAction::Play => write!(f, "Play"),
            MediaControlAction::Pause => write!(f, "Pause"),
            MediaControlAction::PlayPause => write!(f, "Play/Pause"),
            MediaControlAction::Next => write!(f, "Next"),
            MediaControlAction::Previous => write!(f, "Previous"),
            MediaControlAction::VolumeUp => write!(f, "Volume Up"),
            MediaControlAction::VolumeDown => write!(f, "Volume Down"),
            MediaControlAction::ToggleMute => write!(f, "Toggle Mute"),
        }
    }
}

// === Presentation Control Types ===

/// Slide control actions for presentations
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum SlideControlAction {
    /// Go to next slide
    NextSlide,
    /// Go to previous slide
    PreviousSlide,
    /// Start presentation mode
    StartPresentation,
    /// End presentation mode
    EndPresentation,
}

impl fmt::Display for SlideControlAction {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            SlideControlAction::NextSlide => write!(f, "Next Slide"),
            SlideControlAction::PreviousSlide => write!(f, "Previous Slide"),
            SlideControlAction::StartPresentation => write!(f, "Start Presentation"),
            SlideControlAction::EndPresentation => write!(f, "End Presentation"),
        }
    }
}

// === Plugin System Types ===

/// Available plugin types in LibreConnect
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PluginType {
    /// Clipboard synchronization
    ClipboardSync,
    /// File transfer capabilities
    FileTransfer,
    /// Input device sharing
    InputShare,
    /// Notification mirroring
    NotificationSync,
    /// Battery status monitoring
    BatteryStatus,
    /// Media playback control
    MediaControl,
    /// Remote command execution
    RemoteCommands,
    /// Touchpad emulation
    TouchpadMode,
    /// Slide presentation control
    SlideControl,
}

impl fmt::Display for PluginType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PluginType::ClipboardSync => write!(f, "Clipboard Sync"),
            PluginType::FileTransfer => write!(f, "File Transfer"),
            PluginType::InputShare => write!(f, "Input Share"),
            PluginType::NotificationSync => write!(f, "Notification Sync"),
            PluginType::BatteryStatus => write!(f, "Battery Status"),
            PluginType::MediaControl => write!(f, "Media Control"),
            PluginType::RemoteCommands => write!(f, "Remote Commands"),
            PluginType::TouchpadMode => write!(f, "Touchpad Mode"),
            PluginType::SlideControl => write!(f, "Slide Control"),
        }
    }
}

impl PluginType {
    /// Get all available plugin types
    pub fn all() -> Vec<PluginType> {
        vec![
            PluginType::ClipboardSync,
            PluginType::FileTransfer,
            PluginType::InputShare,
            PluginType::NotificationSync,
            PluginType::BatteryStatus,
            PluginType::MediaControl,
            PluginType::RemoteCommands,
            PluginType::TouchpadMode,
            PluginType::SlideControl,
        ]
    }

    /// Get plugin description
    pub fn description(&self) -> &'static str {
        match self {
            PluginType::ClipboardSync => "Synchronize clipboard content between devices",
            PluginType::FileTransfer => "Transfer files between devices",
            PluginType::InputShare => "Share keyboard and mouse input",
            PluginType::NotificationSync => "Mirror system notifications",
            PluginType::BatteryStatus => "Monitor and display battery status",
            PluginType::MediaControl => "Control media playback",
            PluginType::RemoteCommands => "Execute commands remotely",
            PluginType::TouchpadMode => "Use device as touchpad",
            PluginType::SlideControl => "Control presentation slides",
        }
    }
}

// === Protocol Constants ===

/// Protocol version for compatibility checking
pub const PROTOCOL_VERSION: &str = "0.1.0";

/// Default TCP port for LibreConnect daemon
pub const DEFAULT_PORT: u16 = 1716;

/// mDNS service type for LibreConnect discovery
pub const MDNS_SERVICE_TYPE: &str = "_libreconnect._tcp.local.";

/// Maximum message size in bytes
pub const MAX_MESSAGE_SIZE: usize = 64 * 1024 * 1024; // 64MB

/// Maximum file chunk size for transfers
pub const MAX_CHUNK_SIZE: usize = 1024 * 1024; // 1MB

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_device_id_creation() {
        let id1 = DeviceId::new("test-device");
        let id2 = DeviceId::from("test-device");
        let id3 = DeviceId::from("test-device".to_string());

        assert_eq!(id1, id2);
        assert_eq!(id2, id3);
        assert_eq!(id1.as_str(), "test-device");
        assert_eq!(id1.to_string(), "test-device");
    }

    #[test]
    fn test_device_info_capabilities() {
        let device = DeviceInfo::new(
            "test-device",
            "Test Device",
            DeviceType::Desktop,
            vec![PluginType::ClipboardSync, PluginType::FileTransfer],
        );

        assert!(device.supports_plugin(&PluginType::ClipboardSync));
        assert!(device.supports_plugin(&PluginType::FileTransfer));
        assert!(!device.supports_plugin(&PluginType::MediaControl));
    }

    #[test]
    fn test_battery_status() {
        let battery = BatteryStatus::new(85.5, true);
        assert_eq!(battery.charge, 85.5);
        assert!(battery.is_charging);
        assert!(!battery.is_low());
        assert!(!battery.is_critical());

        let low_battery = BatteryStatus::new(15.0, false);
        assert!(low_battery.is_low());
        assert!(!low_battery.is_critical());

        let critical_battery = BatteryStatus::new(5.0, false);
        assert!(critical_battery.is_low());
        assert!(critical_battery.is_critical());
    }

    #[test]
    fn test_plugin_types() {
        let all_plugins = PluginType::all();
        assert_eq!(all_plugins.len(), 9);
        assert!(all_plugins.contains(&PluginType::ClipboardSync));
        assert!(all_plugins.contains(&PluginType::SlideControl));

        for plugin in &all_plugins {
            assert!(!plugin.description().is_empty());
            assert!(!plugin.to_string().is_empty());
        }
    }

    #[test]
    fn test_mouse_event_constructors() {
        let move_event = MouseEvent::movement(100, 200);
        assert_eq!(move_event.action, MouseAction::Move);
        assert_eq!(move_event.x, 100);
        assert_eq!(move_event.y, 200);
        assert!(move_event.button.is_none());

        let click_event = MouseEvent::button_press(50, 75, MouseButton::Left);
        assert_eq!(click_event.action, MouseAction::Press);
        assert_eq!(click_event.button, Some(MouseButton::Left));

        let scroll_event = MouseEvent::scroll(0, 0, 1.5);
        assert_eq!(scroll_event.action, MouseAction::Scroll);
        assert_eq!(scroll_event.scroll_delta, Some(1.5));
    }

    #[test]
    fn test_touchpad_event_constructors() {
        let move_event = TouchpadEvent::movement(0.5, 0.5, 10.0, -5.0);
        assert_eq!(move_event.dx, 10.0);
        assert_eq!(move_event.dy, -5.0);
        assert!(!move_event.is_left_click);

        let scroll_event = TouchpadEvent::scroll(0.3, 0.7, 0.0, 2.0);
        assert_eq!(move_event.scroll_delta_y, 0.0);
        assert_eq!(scroll_event.scroll_delta_y, 2.0);

        let click_event = TouchpadEvent::click(0.5, 0.5, false);
        assert!(click_event.is_left_click);
        assert!(!click_event.is_right_click);

        let right_click_event = TouchpadEvent::click(0.5, 0.5, true);
        assert!(!right_click_event.is_left_click);
        assert!(right_click_event.is_right_click);
    }

    #[test]
    fn test_message_serialization() {
        let ping = Message::Ping;
        let serialized = serde_json::to_string(&ping).unwrap();
        let deserialized: Message = serde_json::from_str(&serialized).unwrap();
        assert_eq!(ping, deserialized);

        let device_info = DeviceInfo::new(
            "test-device",
            "Test Device",
            DeviceType::Mobile,
            vec![PluginType::ClipboardSync],
        );
        let info_msg = Message::DeviceInfo(device_info);
        let serialized = serde_json::to_string(&info_msg).unwrap();
        let deserialized: Message = serde_json::from_str(&serialized).unwrap();
        assert_eq!(info_msg, deserialized);
    }
}
