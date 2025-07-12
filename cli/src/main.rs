use clap::{Parser, Subcommand};
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use shared::{DeviceId, Message};
use serde_json;

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
    }

    Ok(())
}
