#[derive(Debug, Clone, PartialEq, Eq, Hash, serde::Serialize, serde::Deserialize)]
pub struct DeviceId(String);

impl From<&str> for DeviceId {
    fn from(s: &str) -> Self {
        DeviceId(s.to_string())
    }
}

impl From<String> for DeviceId {
    fn from(s: String) -> Self {
        DeviceId(s)
    }
}

impl std::fmt::Display for DeviceId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.0)
    }
}

#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum DeviceType {
    Mobile,
    Desktop,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct DeviceInfo {
    pub id: DeviceId,
    pub name: String,
    pub device_type: DeviceType,
    pub capabilities: Vec<PluginType>,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Message {
    // Core messages
    Ping,
    Pong,
    DeviceInfo(DeviceInfo),
    RequestPairing(DeviceInfo),
    PairingAccepted(DeviceId),
    PairingRejected(DeviceId),
    // Plugin messages (examples)
    ClipboardSync(String),
    RequestClipboard,
    FileTransferRequest { file_name: String, file_size: u64 },
    FileTransferChunk { file_name: String, chunk: Vec<u8>, offset: u64 },
    FileTransferEnd { file_name: String },
    FileTransferError { file_name: String, error: String },
    KeyEvent(KeyEvent),
    MouseEvent(MouseEvent),
    Notification {
        title: String,
        body: String,
        app_name: Option<String>,
    },
    MediaControl { action: MediaControlAction },
    BatteryStatus(BatteryStatus),
    RemoteCommand { command: String, args: Vec<String> },
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum KeyAction {
    Press,
    Release,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum KeyCode {
    // Basic alphanumeric and common keys
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    Key0, Key1, Key2, Key3, Key4, Key5, Key6, Key7, Key8, Key9,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    Escape, Tab, CapsLock, LeftShift, LeftControl, LeftAlt, Space, RightAlt, RightControl, RightShift, Enter, Backspace, Delete,
    ArrowLeft, ArrowRight, ArrowUp, ArrowDown,
    // Add more as needed
    Unknown(u32),
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct KeyEvent {
    pub action: KeyAction,
    pub code: KeyCode,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum MouseAction {
    Move,
    Press,
    Release,
    Scroll,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum MouseButton {
    Left,
    Right,
    Middle,
    Other(u32),
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct MouseEvent {
    pub action: MouseAction,
    pub x: i32,
    pub y: i32,
    pub button: Option<MouseButton>,
    pub scroll_delta: Option<f32>,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct BatteryStatus {
    pub charge: f32,
    pub is_charging: bool,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum MediaControlAction {
    Play,
    Pause,
    PlayPause,
    Next,
    Previous,
    VolumeUp,
    VolumeDown,
    ToggleMute,
}

#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum PluginType {
    ClipboardSync,
    FileTransfer,
    InputShare,
    NotificationSync,
    BatteryStatus,
    MediaControl,
    RemoteCommands,
    TouchpadMode,
    SlideControl,
}
