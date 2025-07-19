//! LibreConnect CLI Library
//! 
//! This library provides the core logic for the LibreConnect CLI, including
//! command parsing, daemon communication, and utility functions.

use shared::{Message, DEFAULT_PORT, KeyCode, MouseButton, SlideControlAction};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use std::env;

/// Connect to the LibreConnect daemon
pub async fn connect_to_daemon() -> Result<TcpStream, Box<dyn std::error::Error>> {
    let port = env::var("LIBRECONNECT_PORT")
        .unwrap_or_else(|_| DEFAULT_PORT.to_string())
        .parse::<u16>()?;
    TcpStream::connect(format!("127.0.0.1:{}", port)).await.map_err(|e| {
        format!("Failed to connect to daemon. Is libreconnectd running? Error: {e}").into()
    })
}

/// Send a message to the daemon and optionally wait for a response
pub async fn send_message(
    message: &Message,
    expect_response: bool,
) -> Result<Option<Message>, Box<dyn std::error::Error>> {
    let mut stream = connect_to_daemon().await?;
    
    // Send the message
    let json = serde_json::to_string(message)?;
    stream.write_all(json.as_bytes()).await?;
    stream.write_all(b"\n").await?;
    
    if expect_response {
        // Read response
        let mut buffer = Vec::new();
        let mut temp_buf = [0u8; 1024];
        
        loop {
            let n = stream.read(&mut temp_buf).await?;
            if n == 0 {
                break;
            }
            buffer.extend_from_slice(&temp_buf[..n]);
            
            // Check if we have a complete message (ends with newline)
            if buffer.ends_with(b"\n") {
                break;
            }
        }
        
        let response_str = String::from_utf8_lossy(&buffer);
        let response = serde_json::from_str(response_str.trim())?;
        Ok(Some(response))
    } else {
        Ok(None)
    }
}

/// Parse a string into a KeyCode
pub fn parse_key_code(key_str: &str) -> Result<KeyCode, String> {
    match key_str.to_lowercase().as_str() {
        "a" => Ok(KeyCode::A),
        "b" => Ok(KeyCode::B),
        "c" => Ok(KeyCode::C),
        "d" => Ok(KeyCode::D),
        "e" => Ok(KeyCode::E),
        "f" => Ok(KeyCode::F),
        "g" => Ok(KeyCode::G),
        "h" => Ok(KeyCode::H),
        "i" => Ok(KeyCode::I),
        "j" => Ok(KeyCode::J),
        "k" => Ok(KeyCode::K),
        "l" => Ok(KeyCode::L),
        "m" => Ok(KeyCode::M),
        "n" => Ok(KeyCode::N),
        "o" => Ok(KeyCode::O),
        "p" => Ok(KeyCode::P),
        "q" => Ok(KeyCode::Q),
        "r" => Ok(KeyCode::R),
        "s" => Ok(KeyCode::S),
        "t" => Ok(KeyCode::T),
        "u" => Ok(KeyCode::U),
        "v" => Ok(KeyCode::V),
        "w" => Ok(KeyCode::W),
        "x" => Ok(KeyCode::X),
        "y" => Ok(KeyCode::Y),
        "z" => Ok(KeyCode::Z),
        "0" => Ok(KeyCode::Key0),
        "1" => Ok(KeyCode::Key1),
        "2" => Ok(KeyCode::Key2),
        "3" => Ok(KeyCode::Key3),
        "4" => Ok(KeyCode::Key4),
        "5" => Ok(KeyCode::Key5),
        "6" => Ok(KeyCode::Key6),
        "7" => Ok(KeyCode::Key7),
        "8" => Ok(KeyCode::Key8),
        "9" => Ok(KeyCode::Key9),
        "space" => Ok(KeyCode::Space),
        "enter" => Ok(KeyCode::Enter),
        "escape" => Ok(KeyCode::Escape),
        "tab" => Ok(KeyCode::Tab),
        "shift" | "leftshift" => Ok(KeyCode::LeftShift),
        "ctrl" | "leftctrl" | "leftcontrol" => Ok(KeyCode::LeftControl),
        "alt" | "leftalt" => Ok(KeyCode::LeftAlt),
        "rightshift" => Ok(KeyCode::RightShift),
        "rightctrl" | "rightcontrol" => Ok(KeyCode::RightControl),
        "rightalt" => Ok(KeyCode::RightAlt),
        "backspace" => Ok(KeyCode::Backspace),
        "delete" => Ok(KeyCode::Delete),
        "capslock" => Ok(KeyCode::CapsLock),
        "f1" => Ok(KeyCode::F1),
        "f2" => Ok(KeyCode::F2),
        "f3" => Ok(KeyCode::F3),
        "f4" => Ok(KeyCode::F4),
        "f5" => Ok(KeyCode::F5),
        "f6" => Ok(KeyCode::F6),
        "f7" => Ok(KeyCode::F7),
        "f8" => Ok(KeyCode::F8),
        "f9" => Ok(KeyCode::F9),
        "f10" => Ok(KeyCode::F10),
        "f11" => Ok(KeyCode::F11),
        "f12" => Ok(KeyCode::F12),
        "arrow_left" | "arrowleft" | "left" => Ok(KeyCode::ArrowLeft),
        "arrow_right" | "arrowright" | "right" => Ok(KeyCode::ArrowRight),
        "arrow_up" | "arrowup" | "up" => Ok(KeyCode::ArrowUp),
        "arrow_down" | "arrowdown" | "down" => Ok(KeyCode::ArrowDown),
        _ => Err(format!("Unknown key code: {}", key_str)),
    }
}

/// Parse a string into a MouseButton
pub fn parse_mouse_button(button_str: &str) -> Result<MouseButton, String> {
    match button_str.to_lowercase().as_str() {
        "left" => Ok(MouseButton::Left),
        "right" => Ok(MouseButton::Right),
        "middle" => Ok(MouseButton::Middle),
        _ => Err(format!("Unknown mouse button: {}", button_str)),
    }
}

/// Parse a string into a SlideControlAction
pub fn parse_slide_action(action_str: &str) -> Result<SlideControlAction, String> {
    match action_str.to_lowercase().as_str() {
        "start" | "startpresentation" => Ok(SlideControlAction::StartPresentation),
        "nextslide" | "next" => Ok(SlideControlAction::NextSlide),
        "prevslide" | "previousslide" | "prev" | "previous" => Ok(SlideControlAction::PreviousSlide),
        "stop" | "end" | "endpresentation" => Ok(SlideControlAction::EndPresentation),
        _ => Err(format!("Unknown slide action: {}", action_str)),
    }
}