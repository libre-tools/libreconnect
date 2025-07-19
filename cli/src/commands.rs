//! This module contains the implementation for each of the CLI commands.

use cli::{send_message, parse_key_code, parse_mouse_button, parse_slide_action};
use shared::{Message, KeyAction, MouseAction, MediaControlAction, BatteryStatus, TouchpadEvent};
use std::path::PathBuf;
use tokio::fs::File;
use tokio::io::{AsyncReadExt, AsyncWriteExt};

pub fn start_daemon() {
    println!("Starting LibreConnect daemon...");
    // In a real implementation, this would spawn the daemon process
    // For now, we'll just provide instructions
    println!("To start the daemon manually, run:");
    println!("  cargo run --bin libreconnectd");
    println!("Or build and run the daemon binary directly.");
}

pub async fn ping_daemon() -> Result<(), Box<dyn std::error::Error>> {
    println!("Sending Ping to daemon...");
    match send_message(&Message::Ping, true).await? {
        Some(Message::Pong) => println!("✅ Daemon is running and responding"),
        Some(other) => println!("❌ Unexpected response: {other:?}"),
        None => println!("❌ No response from daemon"),
    }
    Ok(())
}

pub async fn set_clipboard(_device_id: &str, content: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!("Setting clipboard content...");
    send_message(&Message::ClipboardSync(content.to_string()), false).await?;
    println!("✅ Clipboard content sent successfully");
    Ok(())
}

pub async fn get_clipboard(_device_id: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!("Requesting clipboard content...");
    match send_message(&Message::RequestClipboard, true).await? {
        Some(Message::ClipboardSync(content)) => {
            println!("📋 Clipboard content: {content}")
        }
        Some(other) => println!("❌ Unexpected response: {other:?}"),
        None => println!("❌ No response from daemon"),
    }
    Ok(())
}

pub async fn send_file(device_id: &str, file_path: &PathBuf) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "📁 Sending file {} to device {}...",
        file_path.display(),
        device_id
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

    let mut stream = cli::connect_to_daemon().await?;

    // Send file transfer request
    let request_message = Message::FileTransferRequest {
        file_name: file_name.clone(),
        file_size,
    };
    let serialized = serde_json::to_vec(&request_message)?;
    stream.write_all(&serialized).await?;

    println!("📤 Transferring {} ({} bytes)...", file_name, file_size);

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
        print!("📊 Progress: {}% ({}/{}) bytes", progress, bytes_sent, file_size);
        use std::io::{self, Write};
        io::stdout().flush()?;
    }

    // Send end message
    let end_message = Message::FileTransferEnd { file_name };
    let serialized = serde_json::to_vec(&end_message)?;
    stream.write_all(&serialized).await?;

    println!("
✅ File transfer completed successfully!");
    Ok(())
}

pub async fn send_key_event(device_id: &str, action: &str, key_code: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending key event to device {}: action={}, key_code={}",
        device_id, action, key_code
    );
    let key_action = match action {
        "press" => KeyAction::Press,
        "release" => KeyAction::Release,
        _ => return Err("Invalid key action. Use 'press' or 'release'.".into()),
    };

    let key_code = parse_key_code(key_code)?;

    let key_event = shared::KeyEvent::new(key_action, key_code);
    send_message(&Message::KeyEvent(key_event), false).await?;
    println!("⌨️  Key event sent successfully");
    Ok(())
}

pub async fn send_mouse_event(
    device_id: &str,
    action: &str,
    x: i32,
    y: i32,
    button: Option<&str>,
    scroll_delta: Option<f32>,
) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending mouse event to device {}: action={}, x={}, y={}, button={:?}, scroll_delta={:?}",
        device_id, action, x, y, button, scroll_delta
    );
    let mouse_action = match action {
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
        MouseAction::Move => shared::MouseEvent::movement(x, y),
        MouseAction::Press => {
            let btn = mouse_button.ok_or("Mouse button required for press action")?;
            shared::MouseEvent::button_press(x, y, btn)
        }
        MouseAction::Release => {
            let btn = mouse_button.ok_or("Mouse button required for release action")?;
            shared::MouseEvent::button_release(x, y, btn)
        }
        MouseAction::Scroll => {
            let delta = scroll_delta.ok_or("Scroll delta required for scroll action")?;
            shared::MouseEvent::scroll(x, y, delta)
        }
    };
    send_message(&Message::MouseEvent(mouse_event), false).await?;
    println!("🖱️  Mouse event sent successfully");
    Ok(())
}

pub async fn send_notification(device_id: &str, title: &str, body: &str, app_name: Option<&str>) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending notification to device {}: title=\"{}\", body=\"{}\", app_name={:?}",
        device_id, title, body, app_name
    );
    let notification = Message::Notification {
        title: title.to_string(),
        body: body.to_string(),
        app_name: app_name.map(|s| s.to_string()),
    };
    send_message(&notification, false).await?;
    println!("🔔 Notification sent successfully");
    Ok(())
}

pub async fn send_media_control(
    device_id: &str,
    action: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending media control to device {}: action={}",
        device_id, action
    );
    let media_action = match action {
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
    Ok(())
}

pub async fn get_battery_status(device_id: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!("Requesting battery status from device {}.", device_id);
    println!("🔋 Requesting battery status from {}...", device_id);
    // Note: This is a placeholder - actual battery status requests would need
    // a different message type for requesting vs reporting battery status
    match send_message(
        &Message::BatteryStatus(BatteryStatus::new(0.0, false)),
        true,
    )
    .await?
    {
        Some(Message::BatteryStatus(status)) => {
            println!("🔋 Battery Status: {}", status);
        }
        Some(other) => println!("❌ Unexpected response: {:?}", other),
        None => println!("❌ No response from daemon"),
    }
    Ok(())
}

pub async fn send_touchpad_event(
    device_id: &str,
    dx: f32,
    dy: f32,
    scroll_delta_x: f32,
    scroll_delta_y: f32,
    left_click: bool,
    right_click: bool,
) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending touchpad event to device {}: dx={}, dy={}, scroll_x={}, scroll_y={}, left_click={}, right_click={}",
        device_id, dx, dy, scroll_delta_x, scroll_delta_y, left_click, right_click
    );
    let touchpad_event = if left_click || right_click {
        TouchpadEvent::click(0.5, 0.5, right_click)
    } else if scroll_delta_x != 0.0 || scroll_delta_y != 0.0 {
        TouchpadEvent::scroll(0.5, 0.5, scroll_delta_x, scroll_delta_y)
    } else {
        TouchpadEvent::movement(0.5, 0.5, dx, dy)
    };
    send_message(&Message::TouchpadEvent(touchpad_event), false).await?;
    println!("📱 Touchpad event sent successfully");
    Ok(())
}

pub async fn send_slide_control(
    device_id: &str,
    action: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending slide control to device {}: action={}",
        device_id, action
    );
    let slide_action = parse_slide_action(action)?;
    send_message(&Message::SlideControl(slide_action), false).await?;
    println!("📊 Slide control sent successfully");
    Ok(())
}

pub async fn send_remote_command(
    device_id: &str,
    command: &str,
    args: &[String],
) -> Result<(), Box<dyn std::error::Error>> {
    println!(
        "Sending remote command to device {}: command={}, args={:?}",
        device_id, command, args
    );
    let remote_cmd = Message::RemoteCommand {
        command: command.to_string(),
        args: args.to_vec(),
    };
    send_message(&remote_cmd, false).await?;
    println!("💻 Remote command sent successfully");
    Ok(())
}