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
    Notification {
        title: String,
        body: String,
        app_name: Option<String>,
    },
    MediaControl { action: MediaControlAction },
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