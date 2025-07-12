use clap::{Parser, Subcommand};
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use shared::{DeviceId, Message};
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
    }

    Ok(())
}
