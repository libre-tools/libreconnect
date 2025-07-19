//! LibreConnect CLI
//!
//! Command-line interface for interacting with the LibreConnect daemon.
//! Provides commands for all plugin functionality including file transfer,
//! input sharing, clipboard sync, and more.

mod commands;


use clap::{Parser, Subcommand};
use std::path::PathBuf;

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
    PingDaemon {
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Set clipboard content on a device
    SetClipboard {
        /// The ID of the target device
        device_id: String,
        /// The content to set on the clipboard
        content: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Request clipboard content from a device
    GetClipboard {
        /// The ID of the target device
        device_id: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Send a file to a device
    SendFile {
        /// The ID of the target device
        device_id: String,
        /// The path to the file to send
        file_path: PathBuf,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Send a key event to a device
    SendKeyEvent {
        /// The ID of the target device
        device_id: String,
        /// The action (Press or Release)
        action: String,
        /// The key code
        key_code: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
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
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
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
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Send a media control command to a device
    SendMediaControl {
        /// The ID of the target device
        device_id: String,
        /// The media control action (Play, Pause, Next, Previous, etc.)
        action: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Get battery status from a device
    GetBatteryStatus {
        /// The ID of the target device
        device_id: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
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
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
    /// Send a slide control command to a device
    SendSlideControl {
        /// The ID of the target device
        device_id: String,
        /// The slide control action (NextSlide, PreviousSlide, StartPresentation, EndPresentation)
        action: String,
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
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
        #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
        port: u16,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match &cli.command {
        Commands::StartDaemon => commands::start_daemon(),
        Commands::PingDaemon { port: _ } => commands::ping_daemon().await?,
        Commands::SetClipboard { device_id, content, port: _ } => {
            commands::set_clipboard(device_id, content).await?
        }
        Commands::GetClipboard { device_id, port: _ } => commands::get_clipboard(device_id).await?,
        Commands::SendFile { device_id, file_path, port: _ } => {
            commands::send_file(device_id, file_path).await?
        }
        Commands::SendKeyEvent { device_id, action, key_code, port: _ } => {
            commands::send_key_event(device_id, action, key_code).await?
        }
        Commands::SendMouseEvent { device_id, action, x, y, button, scroll_delta, port: _ } => {
            commands::send_mouse_event(device_id, action, *x, *y, button.as_deref(), *scroll_delta).await?
        }
        Commands::SendNotification { device_id, title, body, app_name, port: _ } => {
            commands::send_notification(device_id, title, body, app_name.as_deref()).await?
        }
        Commands::SendMediaControl { device_id, action, port: _ } => {
            commands::send_media_control(device_id, action).await?
        }
        Commands::GetBatteryStatus { device_id, port: _ } => {
            commands::get_battery_status(device_id).await?
        }
        Commands::SendTouchpadEvent { device_id, dx, dy, scroll_delta_x, scroll_delta_y, left_click, right_click, port: _ } => {
            commands::send_touchpad_event(device_id, *dx, *dy, *scroll_delta_x, *scroll_delta_y, *left_click, *right_click).await?
        }
        Commands::SendSlideControl { device_id, action, port: _ } => {
            commands::send_slide_control(device_id, action).await?
        }
        Commands::SendRemoteCommand { device_id, command, args, port: _ } => {
            commands::send_remote_command(device_id, command, args).await?
        }
    }

    Ok(())
}