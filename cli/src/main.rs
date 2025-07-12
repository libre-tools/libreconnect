use clap::{Parser, Subcommand};
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use shared::{DeviceId, Message, KeyAction, KeyCode, KeyEvent, MouseAction, MouseButton, MouseEvent};
use serde_json;
use tokio::fs::File;
use tokio::io::AsyncReadExt as TokioAsyncReadExt;
use std::path::PathBuf;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
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
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match &cli.command {
        Commands::StartDaemon => {
            println!("Attempting to start daemon...");
            println!("Daemon assumed to be running or starting.");
        },
        Commands::PingDaemon => {
            println!("Sending Ping to daemon...");
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            let ping_message = serde_json::to_vec(&Message::Ping)?;
            stream.write_all(&ping_message).await?;

            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response_message: Message = serde_json::from_slice(&buffer[0..n])?;

            match response_message {
                Message::Pong => println!("Received Pong from daemon!"),
                _ => println!("Received unexpected response: {:?}", response_message),
            }
        },
        Commands::SetClipboard { device_id, content } => {
            println!("Setting clipboard on device.");
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            let message = serde_json::to_vec(&Message::ClipboardSync(content.clone()))?;
            stream.write_all(&message).await?;

            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response_message: Message = serde_json::from_slice(&buffer[0..n])?;
            println!("Daemon response: {:?}", response_message);
        },
        Commands::GetClipboard { device_id } => {
            println!("Requesting clipboard from device.");
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            let message = serde_json::to_vec(&Message::RequestClipboard)?;
            stream.write_all(&message).await?;

            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response_message: Message = serde_json::from_slice(&buffer[0..n])?;

            match response_message {
                Message::ClipboardSync(content) => println!("Received clipboard: {}", content),
                _ => println!("Received unexpected response: {:?}", response_message),
            }
        },
        Commands::SendFile { device_id, file_path } => {
            println!("Sending file {:?} to device {}.", file_path, device_id);
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;

            let file_name = file_path.file_name().unwrap().to_str().unwrap().to_string();
            let mut file = File::open(&file_path).await?;
            let file_size = file.metadata().await?.len();

            let request_message = serde_json::to_vec(&Message::FileTransferRequest {
                file_name: file_name.clone(),
                file_size,
            })?;
            stream.write_all(&request_message).await?;

            let mut buffer = vec![0; 1024];
            let mut offset = 0;
            loop {
                let n = file.read(&mut buffer).await?;
                if n == 0 { break; }

                let chunk_message = serde_json::to_vec(&Message::FileTransferChunk {
                    file_name: file_name.clone(),
                    chunk: buffer[..n].to_vec(),
                    offset,
                })?;
                stream.write_all(&chunk_message).await?;
                offset += n as u64;
            }

            let end_message = serde_json::to_vec(&Message::FileTransferEnd { file_name })?;
            stream.write_all(&end_message).await?;

            println!("File transfer complete.");
        },
        Commands::SendKeyEvent { device_id, action, key_code } => {
            println!("Sending key event to device {}: action={}, key_code={}", device_id, action, key_code);
            let key_action = match action.as_str() {
                "press" => KeyAction::Press,
                "release" => KeyAction::Release,
                _ => return Err("Invalid key action. Use 'press' or 'release'.".into()),
            };

            let key_code = match key_code.as_str() {
                "a" => KeyCode::A, "b" => KeyCode::B, "c" => KeyCode::C, "d" => KeyCode::D,
                "e" => KeyCode::E, "f" => KeyCode::F, "g" => KeyCode::G, "h" => KeyCode::H,
                "i" => KeyCode::I, "j" => KeyCode::J, "k" => KeyCode::K, "l" => KeyCode::L,
                "m" => KeyCode::M, "n" => KeyCode::N, "o" => KeyCode::O, "p" => KeyCode::P,
                "q" => KeyCode::Q, "r" => KeyCode::R, "s" => KeyCode::S, "t" => KeyCode::T,
                "u" => KeyCode::U, "v" => KeyCode::V, "w" => KeyCode::W, "x" => KeyCode::X,
                "y" => KeyCode::Y, "z" => KeyCode::Z,
                "0" => KeyCode::Key0, "1" => KeyCode::Key1, "2" => KeyCode::Key2, "3" => KeyCode::Key3,
                "4" => KeyCode::Key4, "5" => KeyCode::Key5, "6" => KeyCode::Key6, "7" => KeyCode::Key7,
                "8" => KeyCode::Key8, "9" => KeyCode::Key9,
                "f1" => KeyCode::F1, "f2" => KeyCode::F2, "f3" => KeyCode::F3, "f4" => KeyCode::F4,
                "f5" => KeyCode::F5, "f6" => KeyCode::F6, "f7" => KeyCode::F7, "f8" => KeyCode::F8,
                "f9" => KeyCode::F9, "f10" => KeyCode::F10, "f11" => KeyCode::F11, "f12" => KeyCode::F12,
                "escape" => KeyCode::Escape, "tab" => KeyCode::Tab, "capslock" => KeyCode::CapsLock,
                "leftshift" => KeyCode::LeftShift, "leftcontrol" => KeyCode::LeftControl,
                "leftalt" => KeyCode::LeftAlt, "space" => KeyCode::Space,
                "rightalt" => KeyCode::RightAlt, "rightcontrol" => KeyCode::RightControl,
                "rightshift" => KeyCode::RightShift, "enter" => KeyCode::Enter,
                "backspace" => KeyCode::Backspace, "delete" => KeyCode::Delete,
                "arrowleft" => KeyCode::ArrowLeft, "arrowright" => KeyCode::ArrowRight,
                "arrowup" => KeyCode::ArrowUp, "arrowdown" => KeyCode::ArrowDown,
                _ => return Err(format!("Invalid key code: {}", key_code).into()),
            };

            let key_event = KeyEvent { action: key_action, code: key_code };
            let message = serde_json::to_vec(&Message::KeyEvent(key_event))?;
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            stream.write_all(&message).await?;
            println!("Key event sent.");
        },
        Commands::SendMouseEvent { device_id, action, x, y, button, scroll_delta } => {
            println!("Sending mouse event to device {}: action={}, x={}, y={}, button={:?}, scroll_delta={:?}", device_id, action, x, y, button, scroll_delta);
            let mouse_action = match action.as_str() {
                "move" => MouseAction::Move,
                "press" => MouseAction::Press,
                "release" => MouseAction::Release,
                "scroll" => MouseAction::Scroll,
                _ => return Err("Invalid mouse action. Use 'move', 'press', 'release', or 'scroll'.".into()),
            };

            let mouse_button = if let Some(btn) = button {
                match btn.as_str() {
                    "left" => Some(MouseButton::Left),
                    "right" => Some(MouseButton::Right),
                    "middle" => Some(MouseButton::Middle),
                    _ => return Err(format!("Invalid mouse button: {}", btn).into()),
                }
            } else {
                None
            };

            let mouse_event = MouseEvent {
                action: mouse_action,
                x: *x,
                y: *y,
                button: mouse_button,
                scroll_delta: *scroll_delta,
            };
            let message = serde_json::to_vec(&Message::MouseEvent(mouse_event))?;
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            stream.write_all(&message).await?;
            println!("Mouse event sent.");
        },
    }

    Ok(())
}
