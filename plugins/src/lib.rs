//! LibreConnect Plugin System
//!
//! This module provides the plugin architecture for LibreConnect, enabling modular
//! functionality for device communication and system integration.

use shared::{
    BatteryStatus, DeviceId, KeyAction, KeyCode, MediaControlAction, Message, MouseAction,
    MouseButton, SlideControlAction,
};
use std::collections::HashMap;
use std::fs::File;
use std::io::{Seek, SeekFrom, Write};
use std::path::Path;
use std::process::Command;
use std::sync::{Arc, Mutex};

mod enigo_utils;
use enigo_utils::create_enigo_instance;

// Import Enigo traits for input simulation
use enigo::{Keyboard, Mouse};

pub trait Plugin: Send + Sync {
    fn name(&self) -> &'static str;
    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>>;
}

pub struct PingPlugin;

impl Plugin for PingPlugin {
    fn name(&self) -> &'static str {
        "ping"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::Ping => {
                println!("Ping received from {sender_id}. Sending Pong.");
                Ok(Some(Message::Pong))
            }
            Message::Pong => {
                println!("Pong received from {sender_id}.");
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct ClipboardSyncPlugin {
    clipboard: Arc<Mutex<Option<arboard::Clipboard>>>,
}

impl Default for ClipboardSyncPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl ClipboardSyncPlugin {
    pub fn new() -> Self {
        let clipboard = match arboard::Clipboard::new() {
            Ok(cb) => Some(cb),
            Err(e) => {
                eprintln!("Failed to initialize clipboard: {e}");
                None
            }
        };

        ClipboardSyncPlugin {
            clipboard: Arc::new(Mutex::new(clipboard)),
        }
    }
}

impl Plugin for ClipboardSyncPlugin {
    fn name(&self) -> &'static str {
        "clipboard-sync"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::ClipboardSync(content) => {
                println!("Clipboard content received from {sender_id}: {content}");

                if let Ok(mut clipboard_guard) = self.clipboard.lock() {
                    if let Some(ref mut clipboard) = clipboard_guard.as_mut() {
                        clipboard.set_text(content)?;
                    }
                }
                Ok(None)
            }
            Message::RequestClipboard => {
                println!("Clipboard request received from {sender_id}.");

                if let Ok(mut clipboard_guard) = self.clipboard.lock() {
                    if let Some(ref mut clipboard) = clipboard_guard.as_mut() {
                        let content = clipboard.get_text()?;
                        return Ok(Some(Message::ClipboardSync(content)));
                    }
                }
                Ok(Some(Message::ClipboardSync(String::new())))
            }
            _ => Ok(None),
        }
    }
}

pub struct FileTransferPlugin {
    active_transfers: Arc<Mutex<HashMap<String, File>>>,
    download_dir: String,
}

impl Default for FileTransferPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl FileTransferPlugin {
    pub fn new() -> Self {
        let download_dir = dirs::download_dir()
            .unwrap_or_else(|| {
                std::env::current_dir().unwrap_or_else(|_| std::path::PathBuf::from("."))
            })
            .join("LibreConnect")
            .to_string_lossy()
            .to_string();

        // Create download directory if it doesn't exist
        if let Err(e) = std::fs::create_dir_all(&download_dir) {
            eprintln!("Failed to create download directory: {e}");
        }

        FileTransferPlugin {
            active_transfers: Arc::new(Mutex::new(HashMap::new())),
            download_dir,
        }
    }
}

impl Plugin for FileTransferPlugin {
    fn name(&self) -> &'static str {
        "file-transfer"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::FileTransferRequest {
                file_name,
                file_size,
            } => {
                println!("File transfer request from {sender_id}: {file_name} ({file_size} bytes)");

                let file_path = Path::new(&self.download_dir).join(file_name);

                let file = File::create(&file_path)?;
                if let Ok(mut transfers) = self.active_transfers.lock() {
                    transfers.insert(file_name.clone(), file);
                }
                println!("Ready to receive file: {}", file_path.display());
                Ok(None)
            }
            Message::FileTransferChunk {
                file_name,
                chunk,
                offset,
            } => {
                println!(
                    "Received chunk for {} (offset: {}, size: {}) from {}",
                    file_name,
                    offset,
                    chunk.len(),
                    sender_id
                );

                if let Ok(mut transfers) = self.active_transfers.lock() {
                    if let Some(file) = transfers.get_mut(file_name) {
                        file.seek(SeekFrom::Start(*offset))?;
                        file.write_all(chunk)?;
                        file.flush()?;
                    }
                }
                Ok(None)
            }
            Message::FileTransferEnd { file_name } => {
                println!("File transfer for {file_name} completed from {sender_id}.");

                if let Ok(mut transfers) = self.active_transfers.lock() {
                    transfers.remove(file_name);
                }

                let file_path = Path::new(&self.download_dir).join(file_name);
                println!("File saved to: {}", file_path.display());
                Ok(None)
            }
            Message::FileTransferError { file_name, error } => {
                eprintln!("File transfer error for {file_name} from {sender_id}: {error}");

                if let Ok(mut transfers) = self.active_transfers.lock() {
                    transfers.remove(file_name);
                }

                // Clean up partial file
                let file_path = Path::new(&self.download_dir).join(file_name);
                std::fs::remove_file(&file_path)?;
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct InputSharePlugin {
    enigo: Arc<Mutex<Option<enigo::Enigo>>>,
}

impl Default for InputSharePlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl InputSharePlugin {
    pub fn new() -> Self {
        let enigo = match create_enigo_instance() {
            Ok(e) => Some(e),
            Err(err) => {
                eprintln!("Failed to initialize input simulation: {err}");
                None
            }
        };

        InputSharePlugin {
            enigo: Arc::new(Mutex::new(enigo)),
        }
    }

    fn keycode_to_enigo_key(&self, keycode: &KeyCode) -> Option<enigo::Key> {
        match keycode {
            KeyCode::A => Some(enigo::Key::Unicode('a')),
            KeyCode::B => Some(enigo::Key::Unicode('b')),
            KeyCode::C => Some(enigo::Key::Unicode('c')),
            KeyCode::D => Some(enigo::Key::Unicode('d')),
            KeyCode::E => Some(enigo::Key::Unicode('e')),
            KeyCode::F => Some(enigo::Key::Unicode('f')),
            KeyCode::G => Some(enigo::Key::Unicode('g')),
            KeyCode::H => Some(enigo::Key::Unicode('h')),
            KeyCode::I => Some(enigo::Key::Unicode('i')),
            KeyCode::J => Some(enigo::Key::Unicode('j')),
            KeyCode::K => Some(enigo::Key::Unicode('k')),
            KeyCode::L => Some(enigo::Key::Unicode('l')),
            KeyCode::M => Some(enigo::Key::Unicode('m')),
            KeyCode::N => Some(enigo::Key::Unicode('n')),
            KeyCode::O => Some(enigo::Key::Unicode('o')),
            KeyCode::P => Some(enigo::Key::Unicode('p')),
            KeyCode::Q => Some(enigo::Key::Unicode('q')),
            KeyCode::R => Some(enigo::Key::Unicode('r')),
            KeyCode::S => Some(enigo::Key::Unicode('s')),
            KeyCode::T => Some(enigo::Key::Unicode('t')),
            KeyCode::U => Some(enigo::Key::Unicode('u')),
            KeyCode::V => Some(enigo::Key::Unicode('v')),
            KeyCode::W => Some(enigo::Key::Unicode('w')),
            KeyCode::X => Some(enigo::Key::Unicode('x')),
            KeyCode::Y => Some(enigo::Key::Unicode('y')),
            KeyCode::Z => Some(enigo::Key::Unicode('z')),
            KeyCode::Key0 => Some(enigo::Key::Unicode('0')),
            KeyCode::Key1 => Some(enigo::Key::Unicode('1')),
            KeyCode::Key2 => Some(enigo::Key::Unicode('2')),
            KeyCode::Key3 => Some(enigo::Key::Unicode('3')),
            KeyCode::Key4 => Some(enigo::Key::Unicode('4')),
            KeyCode::Key5 => Some(enigo::Key::Unicode('5')),
            KeyCode::Key6 => Some(enigo::Key::Unicode('6')),
            KeyCode::Key7 => Some(enigo::Key::Unicode('7')),
            KeyCode::Key8 => Some(enigo::Key::Unicode('8')),
            KeyCode::Key9 => Some(enigo::Key::Unicode('9')),
            KeyCode::F1 => Some(enigo::Key::F1),
            KeyCode::F2 => Some(enigo::Key::F2),
            KeyCode::F3 => Some(enigo::Key::F3),
            KeyCode::F4 => Some(enigo::Key::F4),
            KeyCode::F5 => Some(enigo::Key::F5),
            KeyCode::F6 => Some(enigo::Key::F6),
            KeyCode::F7 => Some(enigo::Key::F7),
            KeyCode::F8 => Some(enigo::Key::F8),
            KeyCode::F9 => Some(enigo::Key::F9),
            KeyCode::F10 => Some(enigo::Key::F10),
            KeyCode::F11 => Some(enigo::Key::F11),
            KeyCode::F12 => Some(enigo::Key::F12),
            KeyCode::Escape => Some(enigo::Key::Escape),
            KeyCode::Tab => Some(enigo::Key::Tab),
            KeyCode::CapsLock => Some(enigo::Key::CapsLock),
            KeyCode::LeftShift => Some(enigo::Key::LShift),
            KeyCode::LeftControl => Some(enigo::Key::LControl),
            KeyCode::LeftAlt => Some(enigo::Key::Alt),
            KeyCode::Space => Some(enigo::Key::Space),
            KeyCode::RightAlt => Some(enigo::Key::Alt),
            KeyCode::RightControl => Some(enigo::Key::RControl),
            KeyCode::RightShift => Some(enigo::Key::RShift),
            KeyCode::Enter => Some(enigo::Key::Return),
            KeyCode::Backspace => Some(enigo::Key::Backspace),
            KeyCode::Delete => Some(enigo::Key::Delete),
            KeyCode::ArrowLeft => Some(enigo::Key::LeftArrow),
            KeyCode::ArrowRight => Some(enigo::Key::RightArrow),
            KeyCode::ArrowUp => Some(enigo::Key::UpArrow),
            KeyCode::ArrowDown => Some(enigo::Key::DownArrow),
            KeyCode::Unknown(_) => None,
        }
    }

    fn mouse_button_to_enigo(&self, button: &MouseButton) -> enigo::Button {
        match button {
            MouseButton::Left => enigo::Button::Left,
            MouseButton::Right => enigo::Button::Right,
            MouseButton::Middle => enigo::Button::Middle,
            MouseButton::Other(_) => enigo::Button::Left, // Default to left
        }
    }
}

impl Plugin for InputSharePlugin {
    fn name(&self) -> &'static str {
        "input-share"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::KeyEvent(key_event) => {
                println!("Key event from {sender_id}: {key_event:?}");

                if let Ok(mut enigo_guard) = self.enigo.lock() {
                    if let Some(ref mut enigo) = enigo_guard.as_mut() {
                        if let Some(key) = self.keycode_to_enigo_key(&key_event.code) {
                            match key_event.action {
                                KeyAction::Press => {
                                    enigo.key(key, enigo::Direction::Press)?;
                                }
                                KeyAction::Release => {
                                    enigo.key(key, enigo::Direction::Release)?;
                                }
                            }
                        }
                    }
                }
                Ok(None)
            }
            Message::MouseEvent(mouse_event) => {
                println!("Mouse event from {sender_id}: {mouse_event:?}");

                if let Ok(mut enigo_guard) = self.enigo.lock() {
                    if let Some(ref mut enigo) = enigo_guard.as_mut() {
                        match mouse_event.action {
                            MouseAction::Move => {
                                enigo.move_mouse(
                                    mouse_event.x,
                                    mouse_event.y,
                                    enigo::Coordinate::Abs,
                                )?;
                            }
                            MouseAction::Press => {
                                if let Some(button) = &mouse_event.button {
                                    let enigo_button = self.mouse_button_to_enigo(button);
                                    enigo.button(enigo_button, enigo::Direction::Press)?;
                                }
                            }
                            MouseAction::Release => {
                                if let Some(button) = &mouse_event.button {
                                    let enigo_button = self.mouse_button_to_enigo(button);
                                    enigo.button(enigo_button, enigo::Direction::Release)?;
                                }
                            }
                            MouseAction::Scroll => {
                                if let Some(scroll_delta) = mouse_event.scroll_delta {
                                    enigo.scroll(scroll_delta as i32, enigo::Axis::Vertical)?;
                                }
                            }
                        }
                    }
                }
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct NotificationSyncPlugin;

impl Plugin for NotificationSyncPlugin {
    fn name(&self) -> &'static str {
        "notification-sync"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::Notification {
                title,
                body,
                app_name,
            } => {
                println!(
                    "Notification from {sender_id}: Title=\"{title}\", Body=\"{body}\", App={app_name:?}"
                );

                let mut notification = notify_rust::Notification::new();
                notification.summary(title);
                notification.body(body);

                if let Some(app) = app_name {
                    notification.appname(app);
                }

                notification.timeout(notify_rust::Timeout::Milliseconds(5000));

                notification.show()?;

                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct MediaControlPlugin;

impl Plugin for MediaControlPlugin {
    fn name(&self) -> &'static str {
        "media-control"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::MediaControl { action } => {
                println!("Media control action from {sender_id}: {action:?}");

                // Use Enigo to simulate media key presses
                if let Ok(mut enigo) = create_enigo_instance() {
                    match action {
                        MediaControlAction::Play => {
                            enigo.key(enigo::Key::MediaPlayPause, enigo::Direction::Click)?;
                        }
                        MediaControlAction::Pause => {
                            enigo.key(enigo::Key::MediaPlayPause, enigo::Direction::Click)?;
                        }
                        MediaControlAction::PlayPause => {
                            enigo.key(enigo::Key::MediaPlayPause, enigo::Direction::Click)?;
                        }
                        MediaControlAction::Next => {
                            enigo.key(enigo::Key::MediaNextTrack, enigo::Direction::Click)?;
                        }
                        MediaControlAction::Previous => {
                            enigo.key(enigo::Key::MediaPrevTrack, enigo::Direction::Click)?;
                        }
                        MediaControlAction::VolumeUp => {
                            enigo.key(enigo::Key::VolumeUp, enigo::Direction::Click)?;
                        }
                        MediaControlAction::VolumeDown => {
                            enigo.key(enigo::Key::VolumeDown, enigo::Direction::Click)?;
                        }
                        MediaControlAction::ToggleMute => {
                            enigo.key(enigo::Key::VolumeMute, enigo::Direction::Click)?;
                        }
                    }
                }
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct BatteryStatusPlugin;

impl Plugin for BatteryStatusPlugin {
    fn name(&self) -> &'static str {
        "battery-status"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::BatteryStatus(status) => {
                println!(
                    "Battery status from {sender_id}: Charge={}%, Charging={}",
                    status.charge, status.is_charging
                );

                // Store or display the remote battery status
                // This could be saved to a file or displayed in a GUI
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

impl Default for BatteryStatusPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl BatteryStatusPlugin {
    pub fn new() -> Self {
        Self
    }

    pub fn get_local_battery_status(&self) -> Option<BatteryStatus> {
        // Try to get battery status using a simpler approach that doesn't require thread safety
        match battery::Manager::new() {
            Ok(manager) => {
                if let Ok(batteries) = manager.batteries() {
                    for battery_result in batteries {
                        if let Ok(battery) = battery_result {
                            let charge = battery
                                .state_of_charge()
                                .get::<battery::units::ratio::percent>();
                            let is_charging = matches!(battery.state(), battery::State::Charging);

                            return Some(BatteryStatus {
                                charge: charge as f32,
                                is_charging,
                            });
                        }
                    }
                }
            }
            Err(e) => {
                eprintln!("Failed to get battery status: {e}");
            }
        }
        None
    }
}

pub struct RemoteCommandsPlugin {
    allowed_commands: Vec<String>,
}

impl Default for RemoteCommandsPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl RemoteCommandsPlugin {
    pub fn new() -> Self {
        RemoteCommandsPlugin {
            allowed_commands: vec![
                "echo".to_string(),
                "date".to_string(),
                "whoami".to_string(),
                "pwd".to_string(),
                "ls".to_string(),
                "df".to_string(),
                "uptime".to_string(),
                "uname".to_string(),
            ],
        }
    }
}

impl Plugin for RemoteCommandsPlugin {
    fn name(&self) -> &'static str {
        "remote-commands"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::RemoteCommand { command, args } => {
                println!("Remote command from {sender_id}: {command} with args {args:?}");

                // Security: Only allow specific whitelisted commands
                if !self.allowed_commands.contains(&command.to_string()) {
                    eprintln!("Command '{command}' is not allowed");
                    return Ok(None);
                }

                let output = Command::new(command).args(args).output()?;
                let stdout = String::from_utf8_lossy(&output.stdout);
                let stderr = String::from_utf8_lossy(&output.stderr);

                println!("Command output:");
                println!("STDOUT: {}", stdout);
                if !stderr.is_empty() {
                    println!("STDERR: {}", stderr);
                }

                // In a real implementation, you might want to send the output back
                // This would require extending the Message enum
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct TouchpadModePlugin {
    enigo: Arc<Mutex<Option<enigo::Enigo>>>,
}

impl Default for TouchpadModePlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl TouchpadModePlugin {
    pub fn new() -> Self {
        let enigo = match create_enigo_instance() {
            Ok(e) => Some(e),
            Err(err) => {
                eprintln!("Failed to initialize input simulation for touchpad: {err}");
                None
            }
        };

        TouchpadModePlugin {
            enigo: Arc::new(Mutex::new(enigo)),
        }
    }
}

impl Plugin for TouchpadModePlugin {
    fn name(&self) -> &'static str {
        "touchpad-mode"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::TouchpadEvent(event) => {
                println!("Touchpad event from {sender_id}: {event:?}");

                if let Ok(mut enigo_guard) = self.enigo.lock() {
                    if let Some(ref mut enigo) = enigo_guard.as_mut() {
                        // Move cursor based on delta movement
                        if event.dx != 0.0 || event.dy != 0.0 {
                            enigo.move_mouse(
                                event.dx as i32,
                                event.dy as i32,
                                enigo::Coordinate::Rel,
                            )?;
                        }

                        // Handle scroll
                        if event.scroll_delta_y != 0.0 {
                            enigo.scroll(event.scroll_delta_y as i32, enigo::Axis::Vertical)?;
                        }

                        if event.scroll_delta_x != 0.0 {
                            enigo.scroll(event.scroll_delta_x as i32, enigo::Axis::Horizontal)?;
                        }

                        // Handle clicks
                        if event.is_left_click {
                            enigo.button(enigo::Button::Left, enigo::Direction::Click)?;
                        }

                        if event.is_right_click {
                            enigo.button(enigo::Button::Right, enigo::Direction::Click)?;
                        }
                    }
                }
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

pub struct SlideControlPlugin {
    enigo: Arc<Mutex<Option<enigo::Enigo>>>,
}

impl Default for SlideControlPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl SlideControlPlugin {
    pub fn new() -> Self {
        let enigo = match create_enigo_instance() {
            Ok(e) => Some(e),
            Err(err) => {
                eprintln!("Failed to initialize input simulation for slide control: {err}");
                None
            }
        };

        SlideControlPlugin {
            enigo: Arc::new(Mutex::new(enigo)),
        }
    }
}

impl Plugin for SlideControlPlugin {
    fn name(&self) -> &'static str {
        "slide-control"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Result<Option<Message>, Box<dyn std::error::Error>> {
        match message {
            Message::SlideControl(action) => {
                println!("Slide control action from {sender_id}: {action:?}");

                if let Ok(mut enigo_guard) = self.enigo.lock() {
                    if let Some(ref mut enigo) = enigo_guard.as_mut() {
                        match action {
                            SlideControlAction::NextSlide => {
                                // Send Right Arrow key (common for next slide)
                                enigo.key(enigo::Key::RightArrow, enigo::Direction::Click)?;
                            }
                            SlideControlAction::PreviousSlide => {
                                // Send Left Arrow key (common for previous slide)
                                enigo.key(enigo::Key::LeftArrow, enigo::Direction::Click)?;
                            }
                            SlideControlAction::StartPresentation => {
                                // Send F5 key (common for start presentation)
                                enigo.key(enigo::Key::F5, enigo::Direction::Click)?;
                            }
                            SlideControlAction::EndPresentation => {
                                // Send Escape key (common for end presentation)
                                enigo.key(enigo::Key::Escape, enigo::Direction::Click)?;
                            }
                        }
                    }
                }
                Ok(None)
            }
            _ => Ok(None),
        }
    }
}

// Helper function to create all plugins
pub fn create_all_plugins() -> Vec<Box<dyn Plugin>> {
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

#[cfg(test)]
mod tests {
    use super::*;
    use shared::{
        DeviceId, KeyAction, KeyCode, KeyEvent, MediaControlAction, Message, MouseAction,
        MouseButton, MouseEvent, TouchpadEvent,
    };

    #[test]
    fn test_ping_plugin() {
        let plugin = PingPlugin;
        let device_id = DeviceId::from("test-device");

        // Test ping response
        let response = plugin.handle_message(&Message::Ping, &device_id);
        assert_eq!(response.unwrap(), Some(Message::Pong));

        // Test pong handling (should return None)
        let response = plugin.handle_message(&Message::Pong, &device_id);
        assert_eq!(response.unwrap(), None);

        // Test other messages (should return None)
        let response = plugin.handle_message(&Message::RequestClipboard, &device_id);
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_clipboard_sync_plugin() {
        let plugin = ClipboardSyncPlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test clipboard sync (should not crash)
        let response = plugin.handle_message(
            &Message::ClipboardSync("test content".to_string()),
            &device_id,
        );
        assert_eq!(response.unwrap(), None);

        // Test clipboard request (should return content or empty string)
        let response = plugin.handle_message(&Message::RequestClipboard, &device_id);
        assert!(matches!(response.unwrap(), Some(Message::ClipboardSync(_))));
    }

    #[test]
    fn test_file_transfer_plugin() {
        let plugin = FileTransferPlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test file transfer request
        let response = plugin.handle_message(
            &Message::FileTransferRequest {
                file_name: "test.txt".to_string(),
                file_size: 100,
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);

        // Test file transfer end
        let response = plugin.handle_message(
            &Message::FileTransferEnd {
                file_name: "test.txt".to_string(),
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_input_share_plugin() {
        let plugin = InputSharePlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test key event
        let key_event = KeyEvent {
            action: KeyAction::Press,
            code: KeyCode::A,
        };
        let response = plugin.handle_message(&Message::KeyEvent(key_event), &device_id);
        assert_eq!(response.unwrap(), None);

        // Test mouse event
        let mouse_event = MouseEvent {
            action: MouseAction::Move,
            x: 100,
            y: 100,
            button: Some(MouseButton::Left),
            scroll_delta: None,
        };
        let response = plugin.handle_message(&Message::MouseEvent(mouse_event), &device_id);
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_notification_sync_plugin() {
        let plugin = NotificationSyncPlugin;
        let device_id = DeviceId::from("test-device");

        // Test notification
        let response = plugin.handle_message(
            &Message::Notification {
                title: "Test Title".to_string(),
                body: "Test Body".to_string(),
                app_name: Some("Test App".to_string()),
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_media_control_plugin() {
        let plugin = MediaControlPlugin;
        let device_id = DeviceId::from("test-device");

        // Test media control
        let response = plugin.handle_message(
            &Message::MediaControl {
                action: MediaControlAction::Play,
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_battery_status_plugin() {
        let plugin = BatteryStatusPlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test battery status
        let battery_status = BatteryStatus {
            charge: 85.0,
            is_charging: true,
        };
        let response = plugin.handle_message(&Message::BatteryStatus(battery_status), &device_id);
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_remote_commands_plugin() {
        let plugin = RemoteCommandsPlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test allowed command
        let response = plugin.handle_message(
            &Message::RemoteCommand {
                command: "echo".to_string(),
                args: vec!["hello".to_string()],
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);

        // Test disallowed command (should not crash)
        let response = plugin.handle_message(
            &Message::RemoteCommand {
                command: "rm".to_string(),
                args: vec!["-rf".to_string(), "/".to_string()],
            },
            &device_id,
        );
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_touchpad_mode_plugin() {
        let plugin = TouchpadModePlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test touchpad event
        let touchpad_event = TouchpadEvent {
            x: 0.5,
            y: 0.5,
            dx: 10.0,
            dy: -5.0,
            scroll_delta_x: 0.0,
            scroll_delta_y: 2.0,
            is_left_click: false,
            is_right_click: false,
        };
        let response = plugin.handle_message(&Message::TouchpadEvent(touchpad_event), &device_id);
        assert_eq!(response.unwrap(), None);
    }

    #[test]
    fn test_slide_control_plugin() {
        let plugin = SlideControlPlugin::new();
        let device_id = DeviceId::from("test-device");

        // Test slide control actions
        let actions = vec![
            SlideControlAction::NextSlide,
            SlideControlAction::PreviousSlide,
            SlideControlAction::StartPresentation,
            SlideControlAction::EndPresentation,
        ];

        for action in actions {
            let response = plugin.handle_message(&Message::SlideControl(action), &device_id);
            assert_eq!(response.unwrap(), None);
        }
    }

    #[test]
    fn test_plugin_names() {
        let plugins = create_all_plugins();
        let expected_names = vec![
            "ping",
            "clipboard-sync",
            "file-transfer",
            "input-share",
            "notification-sync",
            "media-control",
            "battery-status",
            "remote-commands",
            "touchpad-mode",
            "slide-control",
        ];

        assert_eq!(plugins.len(), expected_names.len());
        for (plugin, expected_name) in plugins.iter().zip(expected_names.iter()) {
            assert_eq!(plugin.name(), *expected_name);
        }
    }
}
