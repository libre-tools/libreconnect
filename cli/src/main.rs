//! LibreConnect CLI
//!
//! Command-line interface for interacting with the LibreConnect daemon.
//! Provides commands for all plugin functionality including file transfer,
//! input sharing, clipboard sync, and more.

use clap::{Parser, Subcommand};
use shared::{
    BatteryStatus, KeyAction, KeyCode, MediaControlAction, Message, MouseAction, MouseButton,
    SlideControlAction, TouchpadEvent,
};
use std::path::PathBuf;
use tokio::fs::File;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;

/// LibreConnect command-line interface
#[derive(Parser, Debug)]
#[command(
    name = "libreconnect-cli",
    author = "LibreTools Team",
    version = "0.1.0",
    about = "Command-line interface for LibreConnect device synchronization",
    long_about = "LibreConnect CLI provides commands to interact with the LibreConnect daemon \
                  for device pairing, file transfer, input sharing, and more."
)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand, Debug)]
enum Commands {
    /// Start the LibreConnect daemon
    StartDaemon,
    /// Send a ping to the daemon
    PingDaemon,
    /// Set clipboard content on a device
    SetClipboard {
        /// The ID of the target device
        device_id: String,
        /// The content to set on the clipboard
        content: String,
    },
    /// Request clipboard content from a device
    GetClipboard {
        /// The ID of the target device
        device_id: String,
    },
    /// Send a file to a device
    SendFile {
        /// The ID of the target device
        device_id: String,
        /// The path to the file to send
        file_path: PathBuf,
    },
    /// Send a key event to a device
    SendKeyEvent {
        /// The ID of the target device
        device_id: String,
        /// The action (Press or Release)
        action: String,
        /// The key code
        key_code: String,
    },
    /// Send a mouse event to a device
    SendMouseEvent {
        /// The ID of the target device
        device_id: String,
        /// The action (Move, Press, Release, Scroll)
        action: String,
        /// X coordinate (for Move, Press, Release)
        #[arg(long, default_value_t = 0)]
        x: i32,
        /// Y coordinate (for Move, Press, Release)
        #[arg(long, default_value_t = 0)]
        y: i32,
        /// Mouse button (for Press, Release)
        #[arg(long)]
        button: Option<String>,
        /// Scroll delta (for Scroll)
        #[arg(long)]
        scroll_delta: Option<f32>,
    },
    /// Send a notification to a device
    SendNotification {
        /// The ID of the target device
        device_id: String,
        /// The title of the notification
        title: String,
        /// The body of the notification
        body: String,
        /// Optional: The app name sending the notification
        #[arg(long)]
        app_name: Option<String>,
    },
    /// Send a media control command to a device
    SendMediaControl {
        /// The ID of the target device
        device_id: String,
        /// The media control action (Play, Pause, Next, Previous, etc.)
        action: String,
    },
    /// Get battery status from a device
    GetBatteryStatus {
        /// The ID of the target device
        device_id: String,
    },
    /// Send a touchpad event to a device
    SendTouchpadEvent {
        /// The ID of the target device
        device_id: String,
        /// Delta X movement
        #[arg(long, default_value_t = 0.0)]
        dx: f32,
        /// Delta Y movement
        #[arg(long, default_value_t = 0.0)]
        dy: f32,
        /// Scroll delta X
        #[arg(long, default_value_t = 0.0)]
        scroll_delta_x: f32,
        /// Scroll delta Y
        #[arg(long, default_value_t = 0.0)]
        scroll_delta_y: f32,
        /// Left click
        #[arg(long)]
        left_click: bool,
        /// Right click
        #[arg(long)]
        right_click: bool,
    },
    /// Send a slide control command to a device
    SendSlideControl {
        /// The ID of the target device
        device_id: String,
        /// The slide control action (NextSlide, PreviousSlide, StartPresentation, EndPresentation)
        action: String,
    },
    /// Execute a remote command on a device
    SendRemoteCommand {
        /// The ID of the target device
        device_id: String,
        /// The command to execute
        command: String,
        /// Command arguments
        #[arg(long)]
        args: Vec<String>,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match &cli.command {
        Commands::StartDaemon => {
            println!("Starting LibreConnect daemon...");
            // In a real implementation, this would spawn the daemon process
            // For now, we'll just provide instructions
            println!("To start the daemon manually, run:");
            println!("  cargo run --bin libreconnectd");
            println!("Or build and run the daemon binary directly.");
        }
        Commands::PingDaemon => {
            println!("Sending Ping to daemon...");
            match send_message(&Message::Ping, true).await? {
                Some(Message::Pong) => println!("✅ Daemon is running and responding"),
                Some(other) => println!("❌ Unexpected response: {other:?}"),
                None => println!("❌ No response from daemon"),
            }
        }
        Commands::SetClipboard {
            device_id: _,
            content,
        } => {
            println!("Setting clipboard content...");
            send_message(&Message::ClipboardSync(content.clone()), false).await?;
            println!("✅ Clipboard content sent successfully");
        }
        Commands::GetClipboard { device_id: _ } => {
            println!("Requesting clipboard content...");
            match send_message(&Message::RequestClipboard, true).await? {
                Some(Message::ClipboardSync(content)) => {
                    println!("📋 Clipboard content: {content}")
                }
                Some(other) => println!("❌ Unexpected response: {other:?}"),
                None => println!("❌ No response from daemon"),
            }
        }
        Commands::SendFile {
            device_id,
            file_path,
        } => {
            println!(
                "📁 Sending file {} to device {device_id}...",
                file_path.display()
            );

            let file_name = file_path
                .file_name()
                .and_then(|n| n.to_str())
                .ok_or("Invalid file name")?
                .to_string();

            let mut file = File::open(&file_path)
                .await
                .map_err(|e| format!("Failed to open file {}: {e}", file_path.display()))?;
            let file_size = file.metadata().await?.len();

            if file_size == 0 {
                return Err("Cannot send empty file".into());
            }

            let mut stream = connect_to_daemon().await?;

            // Send file transfer request
            let request_message = Message::FileTransferRequest {
                file_name: file_name.clone(),
                file_size,
            };
            let serialized = serde_json::to_vec(&request_message)?;
            stream.write_all(&serialized).await?;

            println!("📤 Transferring {file_name} ({file_size} bytes)...");

            // Send file in chunks
            let mut buffer = vec![0; shared::MAX_CHUNK_SIZE.min(8192)]; // Use smaller chunks for CLI
            let mut offset = 0u64;
            let mut bytes_sent = 0u64;

            while bytes_sent < file_size {
                let n = file.read(&mut buffer).await?;
                if n == 0 {
                    break;
                }

                let chunk_message = Message::FileTransferChunk {
                    file_name: file_name.clone(),
                    chunk: buffer[..n].to_vec(),
                    offset,
                };
                let serialized = serde_json::to_vec(&chunk_message)?;
                stream.write_all(&serialized).await?;

                offset += n as u64;
                bytes_sent += n as u64;

                // Show progress
                let progress = (bytes_sent * 100) / file_size;
                print!("\r📊 Progress: {progress}% ({bytes_sent}/{file_size} bytes)");
                use std::io::{self, Write};
                io::stdout().flush()?;
            }

            // Send end message
            let end_message = Message::FileTransferEnd { file_name };
            let serialized = serde_json::to_vec(&end_message)?;
            stream.write_all(&serialized).await?;

            println!("\n✅ File transfer completed successfully!");
        }
        Commands::SendKeyEvent {
            device_id,
            action,
            key_code,
        } => {
            println!(
                "Sending key event to device {}: action={}, key_code={}",
                device_id, action, key_code
            );
            let key_action = match action.as_str() {
                "press" => KeyAction::Press,
                "release" => KeyAction::Release,
                _ => return Err("Invalid key action. Use 'press' or 'release'.".into()),
            };

            let key_code = parse_key_code(key_code)?;

            let key_event = shared::KeyEvent::new(key_action, key_code);
            send_message(&Message::KeyEvent(key_event), false).await?;
            println!("⌨️  Key event sent successfully");
        }
        Commands::SendMouseEvent {
            device_id,
            action,
            x,
            y,
            button,
            scroll_delta,
        } => {
            println!(
                "Sending mouse event to device {}: action={}, x={}, y={}, button={:?}, scroll_delta={:?}",
                device_id, action, x, y, button, scroll_delta
            );
            let mouse_action = match action.as_str() {
                "move" => MouseAction::Move,
                "press" => MouseAction::Press,
                "release" => MouseAction::Release,
                "scroll" => MouseAction::Scroll,
                _ => {
                    return Err(
                        "Invalid mouse action. Use 'move', 'press', 'release', or 'scroll'.".into(),
                    );
                }
            };

            let mouse_button = if let Some(btn) = button {
                Some(parse_mouse_button(btn)?)
            } else {
                None
            };

            let mouse_event = match mouse_action {
                MouseAction::Move => shared::MouseEvent::movement(*x, *y),
                MouseAction::Press => {
                    let btn = mouse_button.ok_or("Mouse button required for press action")?;
                    shared::MouseEvent::button_press(*x, *y, btn)
                }
                MouseAction::Release => {
                    let btn = mouse_button.ok_or("Mouse button required for release action")?;
                    shared::MouseEvent::button_release(*x, *y, btn)
                }
                MouseAction::Scroll => {
                    let delta = scroll_delta.ok_or("Scroll delta required for scroll action")?;
                    shared::MouseEvent::scroll(*x, *y, delta)
                }
            };
            send_message(&Message::MouseEvent(mouse_event), false).await?;
            println!("🖱️  Mouse event sent successfully");
        }
        Commands::SendNotification {
            device_id,
            title,
            body,
            app_name,
        } => {
            println!(
                "Sending notification to device {}: title=\"{}\", body=\"{}\", app_name={:?}",
                device_id, title, body, app_name
            );
            let notification = Message::Notification {
                title: title.clone(),
                body: body.clone(),
                app_name: app_name.clone(),
            };
            send_message(&notification, false).await?;
            println!("🔔 Notification sent successfully");
        }
        Commands::SendMediaControl { device_id, action } => {
            println!(
                "Sending media control to device {}: action={}",
                device_id, action
            );
            let media_action = match action.as_str() {
                "play" => MediaControlAction::Play,
                "pause" => MediaControlAction::Pause,
                "playpause" => MediaControlAction::PlayPause,
                "next" => MediaControlAction::Next,
                "previous" => MediaControlAction::Previous,
                "volumeup" => MediaControlAction::VolumeUp,
                "volumedown" => MediaControlAction::VolumeDown,
                "togglemute" => MediaControlAction::ToggleMute,
                _ => return Err(format!("Invalid media control action: {}", action).into()),
            };
            let media_message = Message::MediaControl {
                action: media_action,
            };
            send_message(&media_message, false).await?;
            println!("🎵 Media control sent successfully");
        }
        Commands::GetBatteryStatus { device_id } => {
            println!("Requesting battery status from device {}.", device_id);
            println!("🔋 Requesting battery status from {device_id}...");
            // Note: This is a placeholder - actual battery status requests would need
            // a different message type for requesting vs reporting battery status
            match send_message(
                &Message::BatteryStatus(BatteryStatus::new(0.0, false)),
                true,
            )
            .await?
            {
                Some(Message::BatteryStatus(status)) => {
                    println!("🔋 Battery Status: {status}");
                }
                Some(other) => println!("❌ Unexpected response: {other:?}"),
                None => println!("❌ No response from daemon"),
            }
        }
        Commands::SendTouchpadEvent {
            device_id,
            dx,
            dy,
            scroll_delta_x,
            scroll_delta_y,
            left_click,
            right_click,
        } => {
            println!(
                "Sending touchpad event to device {}: dx={}, dy={}, scroll_x={}, scroll_y={}, left_click={}, right_click={}",
                device_id, dx, dy, scroll_delta_x, scroll_delta_y, left_click, right_click
            );
            let touchpad_event = if *left_click || *right_click {
                TouchpadEvent::click(0.5, 0.5, *right_click)
            } else if *scroll_delta_x != 0.0 || *scroll_delta_y != 0.0 {
                TouchpadEvent::scroll(0.5, 0.5, *scroll_delta_x, *scroll_delta_y)
            } else {
                TouchpadEvent::movement(0.5, 0.5, *dx, *dy)
            };
            send_message(&Message::TouchpadEvent(touchpad_event), false).await?;
            println!("📱 Touchpad event sent successfully");
        }
        Commands::SendSlideControl { device_id, action } => {
            println!(
                "Sending slide control to device {}: action={}",
                device_id, action
            );
            let slide_action = parse_slide_action(action)?;
            send_message(&Message::SlideControl(slide_action), false).await?;
            println!("📊 Slide control sent successfully");
        }
        Commands::SendRemoteCommand {
            device_id,
            command,
            args,
        } => {
            println!(
                "Sending remote command to device {}: command={}, args={:?}",
                device_id, command, args
            );
            let remote_cmd = Message::RemoteCommand {
                command: command.clone(),
                args: args.clone(),
            };
            send_message(&remote_cmd, false).await?;
            println!("💻 Remote command sent successfully");
        }
    }

    Ok(())
}

/// Parse a key code string into a KeyCode enum
fn parse_key_code(key_code: &str) -> Result<KeyCode, Box<dyn std::error::Error>> {
    let code = match key_code.to_lowercase().as_str() {
        "a" => KeyCode::A,
        "b" => KeyCode::B,
        "c" => KeyCode::C,
        "d" => KeyCode::D,
        "e" => KeyCode::E,
        "f" => KeyCode::F,
        "g" => KeyCode::G,
        "h" => KeyCode::H,
        "i" => KeyCode::I,
        "j" => KeyCode::J,
        "k" => KeyCode::K,
        "l" => KeyCode::L,
        "m" => KeyCode::M,
        "n" => KeyCode::N,
        "o" => KeyCode::O,
        "p" => KeyCode::P,
        "q" => KeyCode::Q,
        "r" => KeyCode::R,
        "s" => KeyCode::S,
        "t" => KeyCode::T,
        "u" => KeyCode::U,
        "v" => KeyCode::V,
        "w" => KeyCode::W,
        "x" => KeyCode::X,
        "y" => KeyCode::Y,
        "z" => KeyCode::Z,
        "0" => KeyCode::Key0,
        "1" => KeyCode::Key1,
        "2" => KeyCode::Key2,
        "3" => KeyCode::Key3,
        "4" => KeyCode::Key4,
        "5" => KeyCode::Key5,
        "6" => KeyCode::Key6,
        "7" => KeyCode::Key7,
        "8" => KeyCode::Key8,
        "9" => KeyCode::Key9,
        "f1" => KeyCode::F1,
        "f2" => KeyCode::F2,
        "f3" => KeyCode::F3,
        "f4" => KeyCode::F4,
        "f5" => KeyCode::F5,
        "f6" => KeyCode::F6,
        "f7" => KeyCode::F7,
        "f8" => KeyCode::F8,
        "f9" => KeyCode::F9,
        "f10" => KeyCode::F10,
        "f11" => KeyCode::F11,
        "f12" => KeyCode::F12,
        "escape" | "esc" => KeyCode::Escape,
        "tab" => KeyCode::Tab,
        "capslock" | "caps" => KeyCode::CapsLock,
        "leftshift" | "lshift" => KeyCode::LeftShift,
        "leftcontrol" | "lctrl" => KeyCode::LeftControl,
        "leftalt" | "lalt" => KeyCode::LeftAlt,
        "space" => KeyCode::Space,
        "rightalt" | "ralt" => KeyCode::RightAlt,
        "rightcontrol" | "rctrl" => KeyCode::RightControl,
        "rightshift" | "rshift" => KeyCode::RightShift,
        "enter" | "return" => KeyCode::Enter,
        "backspace" | "bksp" => KeyCode::Backspace,
        "delete" | "del" => KeyCode::Delete,
        "arrowleft" | "left" => KeyCode::ArrowLeft,
        "arrowright" | "right" => KeyCode::ArrowRight,
        "arrowup" | "up" => KeyCode::ArrowUp,
        "arrowdown" | "down" => KeyCode::ArrowDown,
        _ => return Err(format!("Invalid key code: {key_code}").into()),
    };
    Ok(code)
}

/// Parse a mouse button string into a MouseButton enum
fn parse_mouse_button(button: &str) -> Result<MouseButton, Box<dyn std::error::Error>> {
    let btn = match button.to_lowercase().as_str() {
        "left" | "l" => MouseButton::Left,
        "right" | "r" => MouseButton::Right,
        "middle" | "m" | "wheel" => MouseButton::Middle,
        _ => return Err(format!("Invalid mouse button: {button}").into()),
    };
    Ok(btn)
}

/// Parse a slide action string into a SlideControlAction enum
fn parse_slide_action(action: &str) -> Result<SlideControlAction, Box<dyn std::error::Error>> {
    let slide_action = match action.to_lowercase().as_str() {
        "nextslide" | "next" => SlideControlAction::NextSlide,
        "previousslide" | "previous" | "prev" => SlideControlAction::PreviousSlide,
        "startpresentation" | "start" => SlideControlAction::StartPresentation,
        "endpresentation" | "end" | "stop" => SlideControlAction::EndPresentation,
        _ => return Err(format!("Invalid slide control action: {action}").into()),
    };
    Ok(slide_action)
}

/// Connect to the LibreConnect daemon
async fn connect_to_daemon() -> Result<TcpStream, Box<dyn std::error::Error>> {
    TcpStream::connect("127.0.0.1:8080").await.map_err(|e| {
        format!("Failed to connect to daemon. Is libreconnectd running? Error: {e}").into()
    })
}

/// Send a message to the daemon and optionally wait for a response
async fn send_message(
    message: &Message,
    expect_response: bool,
) -> Result<Option<Message>, Box<dyn std::error::Error>> {
    let mut stream = connect_to_daemon().await?;
    let serialized = serde_json::to_vec(message)?;
    stream.write_all(&serialized).await?;

    if expect_response {
        let mut buffer = vec![0; 1024];
        let n = stream.read(&mut buffer).await?;
        let response: Message = serde_json::from_slice(&buffer[0..n])?;
        Ok(Some(response))
    } else {
        Ok(None)
    }
}
