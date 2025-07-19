//! LibreConnect Daemon
//!
//! Core daemon implementation for LibreConnect that handles device discovery,
//! connection management, and plugin coordination.

use mdns_sd::{ServiceDaemon, ServiceEvent, ServiceInfo};
use plugins::{
    BatteryStatusPlugin, ClipboardSyncPlugin, FileTransferPlugin, InputSharePlugin,
    MediaControlPlugin, NotificationSyncPlugin, PingPlugin, Plugin, RemoteCommandsPlugin,
    SlideControlPlugin, TouchpadModePlugin,
};
use serde_json;
use shared::{
    DEFAULT_PORT, DeviceId, DeviceInfo, DeviceType, MDNS_SERVICE_TYPE, Message, PROTOCOL_VERSION,
    PluginType,
};
use std::collections::HashMap;
use std::fs;
use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::timeout;
use rand::Rng;
use hostname;

/// Maximum message size to prevent memory exhaustion
const MAX_MESSAGE_SIZE: usize = shared::MAX_MESSAGE_SIZE;

/// Connection timeout duration
const CONNECTION_TIMEOUT: Duration = Duration::from_secs(120); // Match Android's 2-minute timeout

/// LibreConnect daemon main structure
pub struct Daemon {
    /// Map of paired devices
    paired_devices: Arc<Mutex<HashMap<DeviceId, DeviceInfo>>>,
    /// Current pairing key (6 digits)
    pairing_key: Arc<Mutex<String>>,
    /// Available plugins
    plugins: Arc<Vec<Box<dyn Plugin>>>,
    /// Local device information
    local_device: DeviceInfo,
    /// Port the daemon listens on
    port: u16,
    /// File path for persisting paired devices
    paired_devices_file: PathBuf,
}

impl Default for Daemon {
    fn default() -> Self {
        Self::new(DEFAULT_PORT)
    }
}

impl Daemon {
    /// Create a new daemon instance
    pub fn new(port: u16) -> Self {
        let local_device = DeviceInfo::new(
            format!(
                "libreconnect-{}",
                hostname::get().unwrap_or_default().to_string_lossy()
            ),
            format!(
                "LibreConnect Desktop ({})",
                hostname::get().unwrap_or_default().to_string_lossy()
            ),
            DeviceType::Desktop,
            PluginType::all(),
        );

        // Setup paired devices file path
        let paired_devices_file = Self::get_paired_devices_file_path();

        // Create daemon instance first
        let daemon = Daemon {
            paired_devices: Arc::new(Mutex::new(HashMap::new())),
            pairing_key: Arc::new(Mutex::new(String::new())),
            plugins: Arc::new(Self::create_plugins()),
            local_device,
            port,
            paired_devices_file,
        };

        // Load existing paired devices
        daemon.load_paired_devices();

        // Generate initial pairing key
        let pairing_key = Self::generate_pairing_key();
        {
            let mut key = daemon.pairing_key.lock().unwrap();
            *key = pairing_key.clone();
        }
        println!("🔑 Generated pairing key: {}", pairing_key);

        daemon
    }

    /// Generate a 6-digit pairing key
    fn generate_pairing_key() -> String {
        let mut rng = rand::thread_rng();
        format!("{:06}", rng.gen_range(100000..=999999))
    }

    /// Get the file path for storing paired devices
    fn get_paired_devices_file_path() -> PathBuf {
        let config_dir = dirs::config_dir()
            .unwrap_or_else(|| dirs::home_dir().unwrap_or_default().join(".config"))
            .join("libreconnect");
        
        // Create config directory if it doesn't exist
        if let Err(e) = fs::create_dir_all(&config_dir) {
            eprintln!("⚠️ Failed to create config directory: {}", e);
        }
        
        config_dir.join("paired_devices.json")
    }

    /// Load paired devices from disk
    fn load_paired_devices(&self) {
        match fs::read_to_string(&self.paired_devices_file) {
            Ok(content) => {
                match serde_json::from_str::<Vec<DeviceInfo>>(&content) {
                    Ok(devices) => {
                        let mut paired_devices = self.paired_devices.lock().unwrap();
                        for device in devices {
                            paired_devices.insert(device.id.clone(), device);
                        }
                        let count = paired_devices.len();
                        drop(paired_devices);
                        if count > 0 {
                            println!("📂 Loaded {} paired device(s) from disk", count);
                        }
                    }
                    Err(e) => {
                        eprintln!("⚠️ Failed to parse paired devices file: {}", e);
                    }
                }
            }
            Err(_) => {
                // File doesn't exist yet, which is fine for first run
                println!("📂 No existing paired devices file found (first run)");
            }
        }
    }

    /// Save paired devices to disk
    fn save_paired_devices(&self) {
        Self::save_paired_devices_static(&self.paired_devices, &self.paired_devices_file);
    }
    /// Save paired devices to disk (static version)
    fn save_paired_devices_static(
        paired_devices: &Arc<Mutex<HashMap<DeviceId, DeviceInfo>>>,
        file_path: &PathBuf,
    ) {
        let devices: Vec<DeviceInfo> = {
            match paired_devices.lock() {
                Ok(devices_guard) => devices_guard.values().cloned().collect(),
                Err(_) => {
                    eprintln!("⚠️ Failed to lock paired devices for saving");
                    return;
                }
            }
        };

        match serde_json::to_string_pretty(&devices) {
            Ok(content) => {
                if let Err(e) = fs::write(file_path, content) {
                    eprintln!("⚠️ Failed to save paired devices: {}", e);
                } else {
                    println!("💾 Saved {} paired device(s) to disk", devices.len());
                }
            }
            Err(e) => {
                eprintln!("⚠️ Failed to serialize paired devices: {}", e);
            }
        }
    }

    /// Create all available plugins
    fn create_plugins() -> Vec<Box<dyn Plugin>> {
        vec![
            Box::new(PingPlugin),
            Box::new(ClipboardSyncPlugin::new()),
            Box::new(FileTransferPlugin::new()),
            Box::new(InputSharePlugin::new()),
            Box::new(NotificationSyncPlugin),
            Box::new(MediaControlPlugin),
            Box::new(BatteryStatusPlugin::new()),
            Box::new(RemoteCommandsPlugin::new()),
            Box::new(TouchpadModePlugin::new()),
            Box::new(SlideControlPlugin::new()),
        ]
    }

    /// Start the daemon
    pub async fn start(&self) -> Result<(), DaemonError> {
        println!("🚀 LibreConnect Daemon starting...");
        println!("📱 Local device: {}", self.local_device.name);
        println!("🔌 Available plugins: {}", self.plugins.len());

        // Start mDNS discovery service
        let mdns_handle = self.start_mdns_discovery().await?;

        // Start TCP server
        let server_handle = self.start_tcp_server().await?;

        println!("✅ LibreConnect Daemon started successfully");
        println!("🌐 Listening on port {}", self.port);
        println!("🔍 mDNS discovery active");

        // Wait for both services to complete (they run indefinitely)
        tokio::select! {
            result = mdns_handle => {
                if let Err(e) = result {
                    eprintln!("❌ mDNS service error: {e}");
                }
            }
            result = server_handle => {
                if let Err(e) = result {
                    eprintln!("❌ TCP server error: {e}");
                }
            }
        }

        Ok(())
    }

    /// Start mDNS discovery service
    async fn start_mdns_discovery(
        &self,
    ) -> Result<tokio::task::JoinHandle<Result<(), DaemonError>>, DaemonError> {
        let mdns =
            ServiceDaemon::new().map_err(|e| DaemonError::MdnsInitialization(e.to_string()))?;

        let instance_name = &self.local_device.name;
        let host_name = format!("{}.local.", hostname::get()?.to_string_lossy().replace(' ', "-").to_lowercase());
        let properties = [
            ("version", PROTOCOL_VERSION),
            ("device_type", "desktop"),
            ("plugins", &self.local_device.capabilities.len().to_string()),
        ];

        let service_info = ServiceInfo::new(
            MDNS_SERVICE_TYPE,
            instance_name,
            &host_name,
            "192.168.1.6", // Use actual IP instead of empty string
            self.port,
            &properties[..],
        )
        .map_err(|e| DaemonError::MdnsInitialization(e.to_string()))?
        .to_owned();

        println!("📡 Registering mDNS service: {} on {}", instance_name, host_name);
        mdns.register(service_info)
            .map_err(|e| DaemonError::MdnsRegistration(e.to_string()))?;
        println!("✅ mDNS service registered successfully");

        let service_receiver = mdns
            .browse(MDNS_SERVICE_TYPE)
            .map_err(|e| DaemonError::MdnsBrowsing(e.to_string()))?;

        let paired_devices = self.paired_devices.clone();

        Ok(tokio::spawn(async move {
            println!("🔍 mDNS discovery started");

            while let Ok(event) = service_receiver.recv_async().await {
                match event {
                    ServiceEvent::ServiceFound(_, fullname) => {
                        println!("🔍 Service discovered: {fullname}");
                    }
                    ServiceEvent::ServiceResolved(info) => {
                        println!("✅ Service resolved: {}", info.get_fullname());

                        // Extract device information from mDNS properties
                        let device_id = DeviceId::new(info.get_fullname());
                        let device_name = info.get_fullname().to_string();
                        let device_type = info
                            .get_property_val_str("device_type")
                            .map(|dt| match dt {
                                "mobile" => DeviceType::Mobile,
                                _ => DeviceType::Desktop,
                            })
                            .unwrap_or(DeviceType::Desktop);

                        let device_info = DeviceInfo::new(
                            device_id.clone(),
                            device_name,
                            device_type,
                            vec![], // Capabilities will be exchanged during pairing
                        );

                        if let Ok(mut devices) = paired_devices.lock() {
                            devices.insert(device_id, device_info);
                            println!("📱 Discovered devices: {}", devices.len());
                        }
                    }
                    ServiceEvent::ServiceRemoved(_, fullname) => {
                        println!("❌ Service removed: {fullname}");
                        let device_id = DeviceId::new(fullname);
                        if let Ok(mut devices) = paired_devices.lock() {
                            devices.remove(&device_id);
                        }
                    }
                    _ => {
                        // Handle other events if needed
                    }
                }
            }

            Ok(())
        }))
    }

    /// Start TCP server for client connections
    async fn start_tcp_server(
        &self,
    ) -> Result<tokio::task::JoinHandle<Result<(), DaemonError>>, DaemonError> {
        let bind_addr = format!("0.0.0.0:{}", self.port);
        let listener = TcpListener::bind(&bind_addr)
            .await
            .map_err(|e| DaemonError::ServerStart(format!("Failed to bind to {bind_addr}: {e}")))?;

        println!("🌐 TCP server listening on {bind_addr}");

        let paired_devices = self.paired_devices.clone();
        let pairing_key = self.pairing_key.clone();
        let plugins = self.plugins.clone();
        let paired_devices_file = self.paired_devices_file.clone();

        Ok(tokio::spawn(async move {
            loop {
                match listener.accept().await {
                    Ok((socket, addr)) => {
                        println!("🔗 New connection from {addr}");

                        let paired_devices = paired_devices.clone();
                        let pairing_key = pairing_key.clone();
                        let plugins = plugins.clone();
                        let paired_devices_file = paired_devices_file.clone();

                        tokio::spawn(async move {
                            if let Err(e) = Self::handle_client_connection(
                                socket,
                                addr,
                                paired_devices,
                                pairing_key,
                                plugins,
                                paired_devices_file,
                            )
                            .await
                            {
                                eprintln!("❌ Client connection error from {addr}: {e}");
                            }
                        });
                    }
                    Err(e) => {
                        eprintln!("❌ Failed to accept connection: {e}");
                        tokio::time::sleep(Duration::from_millis(100)).await;
                    }
                }
            }
        }))
    }

    /// Handle individual client connection
    async fn handle_client_connection(
        mut stream: TcpStream,
        addr: SocketAddr,
        paired_devices: Arc<Mutex<HashMap<DeviceId, DeviceInfo>>>,
        pairing_key: Arc<Mutex<String>>,
        plugins: Arc<Vec<Box<dyn Plugin>>>,
        paired_devices_file: PathBuf,
    ) -> Result<(), DaemonError> {
        let mut buffer = vec![0u8; 8192]; // Use reasonable buffer size

        loop {
            // Read message with timeout
            let bytes_read = match timeout(CONNECTION_TIMEOUT, stream.read(&mut buffer)).await {
                Ok(Ok(0)) => {
                    println!("🔌 Client {addr} disconnected");
                    return Ok(());
                }
                Ok(Ok(n)) => n,
                Ok(Err(e)) => {
                    return Err(DaemonError::NetworkError(format!(
                        "Read error from {addr}: {e}"
                    )));
                }
                Err(_) => {
                    return Err(DaemonError::Timeout(format!("Read timeout from {addr}")));
                }
            };

            // Limit message size for security
            if bytes_read > MAX_MESSAGE_SIZE {
                return Err(DaemonError::MessageTooLarge(bytes_read));
            }

            // Deserialize message
            let message: Message = serde_json::from_slice(&buffer[0..bytes_read]).map_err(|e| {
                DaemonError::MessageDeserialization(format!("Invalid message from {addr}: {e}"))
            })?;

            println!(
                "📨 Received message from {addr}: {}",
                Self::message_type_name(&message)
            );

            // Handle message
            match Self::handle_message(message, addr, &paired_devices, &pairing_key, &plugins, &paired_devices_file).await? {
                Some(response) => {
                    let mut response_data = serde_json::to_vec(&response)
                        .map_err(|e| DaemonError::MessageSerialization(e.to_string()))?;
                    
                    // Add newline for line-delimited JSON (Android expects this)
                    response_data.push(b'\n');

                    match timeout(CONNECTION_TIMEOUT, stream.write_all(&response_data)).await {
                        Ok(Ok(())) => {
                            println!("📤 Sent response to {addr}: {}", Self::message_type_name(&response));
                        }
                        Ok(Err(e)) => {
                            return Err(DaemonError::NetworkError(format!(
                                "Write error to {addr}: {e}"
                            )));
                        }
                        Err(_) => {
                            return Err(DaemonError::Timeout(format!("Write timeout to {addr}")));
                        }
                    }
                }
                None => {
                    // No response needed
                }
            }
        }
    }

    /// Handle individual message
    async fn handle_message(
        message: Message,
        addr: SocketAddr,
        paired_devices: &Arc<Mutex<HashMap<DeviceId, DeviceInfo>>>,
        pairing_key: &Arc<Mutex<String>>,
        plugins: &Arc<Vec<Box<dyn Plugin>>>,
        paired_devices_file: &PathBuf,
    ) -> Result<Option<Message>, DaemonError> {
        match message {
            Message::RequestPairing(device_info) => {
                println!("🤝 Basic pairing request from {}: {} (auto-accepting for compatibility)", addr, device_info.name);

                // Auto-accept basic pairing for backward compatibility
                {
                    let mut devices = paired_devices.lock().map_err(|_| {
                        DaemonError::LockError("Failed to lock paired devices".to_string())
                    })?;
                    devices.insert(device_info.id.clone(), device_info.clone());
                    println!(
                        "✅ Device paired: {} (Total: {})",
                        device_info.name,
                        devices.len()
                    );
                }

                // Save paired devices to disk
                Self::save_paired_devices_static(paired_devices, paired_devices_file);

                Ok(Some(Message::PairingAccepted { device_id: device_info.id.as_str().to_string() }))
            }
            Message::RequestPairingWithKey { id, name, device_type, capabilities, pairing_key: provided_key } => {
                println!("🔑 Pairing request with key from {}: {}", addr, name);

                // Validate pairing key
                let valid_key = {
                    let key = pairing_key.lock().map_err(|_| {
                        DaemonError::LockError("Failed to lock pairing key".to_string())
                    })?;
                    *key == provided_key
                };

                if valid_key {
                    // Create device info from the received data
                    let device_info = DeviceInfo {
                        id: DeviceId::new(id),
                        name: name.clone(),
                        device_type: device_type.parse().unwrap_or(DeviceType::Mobile),
                        capabilities: capabilities.iter()
                            .filter_map(|cap| cap.parse().ok())
                            .collect(),
                    };

                    // Accept pairing and store device
                    {
                        let mut devices = paired_devices.lock().map_err(|_| {
                            DaemonError::LockError("Failed to lock paired devices".to_string())
                        })?;
                        devices.insert(device_info.id.clone(), device_info.clone());
                        println!(
                            "✅ Device paired with key: {} (Total: {})",
                            device_info.name,
                            devices.len()
                        );
                    }

                    // Save paired devices to disk
                    Self::save_paired_devices_static(paired_devices, paired_devices_file);

                    // Generate new pairing key for security
                    let new_key = Self::generate_pairing_key();
                    {
                        let mut key = pairing_key.lock().map_err(|_| {
                            DaemonError::LockError("Failed to lock pairing key".to_string())
                        })?;
                        *key = new_key.clone();
                    }
                    println!("🔑 Generated new pairing key: {}", new_key);

                    Ok(Some(Message::PairingAccepted { device_id: device_info.id.as_str().to_string() }))
                } else {
                    println!("❌ Invalid pairing key from {}: provided '{}' but expected a valid key", addr, provided_key);
                    // Create minimal device info for rejection response
                    Ok(Some(Message::PairingRejected { device_id: id, reason: "Invalid pairing key".to_string() }))
                }
            }
            _ => {
                // Route message to plugins
                let sender_id = DeviceId::new(addr.to_string());

                for plugin in plugins.iter() {
                    if let Ok(Some(response)) = plugin.handle_message(&message, &sender_id) {
                        println!("🔌 Plugin '{}' handled message", plugin.name());
                        return Ok(Some(response));
                    }
                }

                println!("📝 Message processed by plugins (no response)");
                Ok(None)
            }
        }
    }

    /// Get a human-readable name for a message type
    fn message_type_name(message: &Message) -> &'static str {
        match message {
            Message::Ping => "Ping",
            Message::Pong => "Pong",
            Message::DeviceInfo(_) => "DeviceInfo",
            Message::RequestPairing(_) => "RequestPairing",
            Message::RequestPairingWithKey { .. } => "RequestPairingWithKey",
            Message::PairingAccepted { .. } => "PairingAccepted",
            Message::PairingRejected { .. } => "PairingRejected",
            Message::ClipboardSync(_) => "ClipboardSync",
            Message::RequestClipboard => "RequestClipboard",
            Message::FileTransferRequest { .. } => "FileTransferRequest",
            Message::FileTransferChunk { .. } => "FileTransferChunk",
            Message::FileTransferEnd { .. } => "FileTransferEnd",
            Message::FileTransferError { .. } => "FileTransferError",
            Message::KeyEvent(_) => "KeyEvent",
            Message::MouseEvent(_) => "MouseEvent",
            Message::TouchpadEvent(_) => "TouchpadEvent",
            Message::Notification { .. } => "Notification",
            Message::MediaControl { .. } => "MediaControl",
            Message::BatteryStatus(_) => "BatteryStatus",
            Message::RemoteCommand { .. } => "RemoteCommand",
            Message::SlideControl(_) => "SlideControl",
        }
    }

    /// Get information about paired devices
    pub fn get_paired_devices(&self) -> Result<Vec<DeviceInfo>, DaemonError> {
        let devices = self
            .paired_devices
            .lock()
            .map_err(|_| DaemonError::LockError("Failed to lock paired devices".to_string()))?;
        Ok(devices.values().cloned().collect())
    }

    /// Get local device information
    pub fn get_local_device(&self) -> &DeviceInfo {
        &self.local_device
    }

    /// Get list of available plugins
    pub fn get_available_plugins(&self) -> Vec<&str> {
        self.plugins.iter().map(|p| p.name()).collect()
    }
}

/// Daemon-specific error types
#[derive(Debug, thiserror::Error)]
pub enum DaemonError {
    #[error("mDNS initialization failed: {0}")]
    MdnsInitialization(String),

    #[error("mDNS registration failed: {0}")]
    MdnsRegistration(String),

    #[error("mDNS browsing failed: {0}")]
    MdnsBrowsing(String),

    #[error("Server start failed: {0}")]
    ServerStart(String),

    #[error("Network error: {0}")]
    NetworkError(String),

    #[error("Message too large: {0} bytes (max: {MAX_MESSAGE_SIZE})")]
    MessageTooLarge(usize),

    #[error("Message deserialization failed: {0}")]
    MessageDeserialization(String),

    #[error("Message serialization failed: {0}")]
    MessageSerialization(String),

    #[error("Operation timeout: {0}")]
    Timeout(String),

    #[error("Lock error: {0}")]
    LockError(String),
}

impl From<std::io::Error> for DaemonError {
    fn from(error: std::io::Error) -> Self {
        DaemonError::NetworkError(error.to_string())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_daemon_creation() {
        let daemon = Daemon::new(1716);
        assert_eq!(daemon.get_available_plugins().len(), 10);
        assert_eq!(daemon.get_local_device().device_type, DeviceType::Desktop);
        assert!(daemon.get_paired_devices().unwrap().is_empty());
    }

    #[test]
    fn test_message_type_names() {
        assert_eq!(Daemon::message_type_name(&Message::Ping), "Ping");
        assert_eq!(Daemon::message_type_name(&Message::Pong), "Pong");
        assert_eq!(
            Daemon::message_type_name(&Message::ClipboardSync("test".to_string())),
            "ClipboardSync"
        );
    }

    #[tokio::test]
    async fn test_daemon_start_stop() {
        // This test would require more complex setup to avoid port conflicts
        // For now, just test that we can create and configure a daemon
        let daemon = Daemon::new(1716);
        assert!(daemon.get_available_plugins().len() > 0);
    }
}
