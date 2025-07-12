use clap::{Parser, Subcommand};
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use shared::{DeviceId, DeviceInfo, DeviceType, Message};

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
    /// List connected devices
    ListDevices,
    /// Send a file to a device
    SendFile {
        /// The ID of the target device
        device_id: String,
        /// The path to the file to send
        file_path: String,
    },
    /// Send a message to a device
    SendMessage {
        /// The ID of the target device
        device_id: String,
        /// The message to send
        message: String,
    },
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match &cli.command {
        Commands::StartDaemon => {
            println!("Attempting to start daemon...");
            // In a real scenario, you'd spawn the daemon process here
            // For now, assume it's running or will be started externally.
            println!("Daemon assumed to be running or starting.");
        },
        Commands::ListDevices => {
            println!("Listing devices...");
            // Placeholder for connecting to daemon and requesting device list
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            stream.write_all(b"list_devices").await?;
            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response = String::from_utf8_lossy(&buffer[..n]);
            println!("Daemon response: {}", response);
        },
        Commands::SendFile { device_id, file_path } => {
            println!("Sending file {} to device {}", file_path, device_id);
            // Placeholder for connecting to daemon and sending file
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            stream.write_all(format!("send_file {},{}", device_id, file_path).as_bytes()).await?;
            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response = String::from_utf8_lossy(&buffer[..n]);
            println!("Daemon response: {}", response);
        },
        Commands::SendMessage { device_id, message } => {
            println!("Sending message \"{}\" to device {}", message, device_id);
            // Placeholder for connecting to daemon and sending message
            let mut stream = TcpStream::connect("127.0.0.1:8080").await?;
            stream.write_all(format!("send_message {},{}", device_id, message).as_bytes()).await?;
            let mut buffer = vec![0; 1024];
            let n = stream.read(&mut buffer).await?;
            let response = String::from_utf8_lossy(&buffer[..n]);
            println!("Daemon response: {}", response);
        },
    }

    Ok(())
}