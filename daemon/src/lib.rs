use shared::{DeviceId, DeviceInfo, DeviceType, Message};
use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt};

pub struct Daemon {
    // TODO: Add fields for device management, pairing, etc.
}

impl Daemon {
    pub fn new() -> Self {
        Daemon {}
    }

    pub async fn start(&self) -> Result<(), Box<dyn std::error::Error>> {
        println!("LibreConnect Daemon starting...");

        // Example: Listen for incoming connections
        let listener = TcpListener::bind("127.0.0.1:8080").await?;
        println!("Listening on {}", listener.local_addr()?);

        loop {
            let (mut socket, addr) = listener.accept().await?;
            println!("New connection from {}", addr);

            tokio::spawn(async move {
                let mut buf = vec![0; 1024];
                // In a real scenario, you'd handle messages, pairing, etc.
                loop {
                    let n = match socket.read(&mut buf).await {
                        Ok(n) if n == 0 => return,
                        Ok(n) => n,
                        Err(e) => {
                            eprintln!("failed to read from socket; err = {:?}", e);
                            return;
                        }
                    };

                    // Echo back the data
                    if let Err(e) = socket.write_all(&buf[0..n]).await {
                        eprintln!("failed to write to socket; err = {:?}", e);
                        return;
                    }
                }
            });
        }
    }
}