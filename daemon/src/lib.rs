use shared::{DeviceId, DeviceInfo, DeviceType, Message};
use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use serde_json;
use plugins::{Plugin, PingPlugin, ClipboardSyncPlugin, FileTransferPlugin};

pub struct Daemon {
    paired_devices: Arc<Mutex<HashMap<DeviceId, DeviceInfo>>>,
    plugins: Arc<Vec<Box<dyn Plugin>>>,
}

impl Daemon {
    pub fn new() -> Self {
        Daemon {
            paired_devices: Arc::new(Mutex::new(HashMap::new())),
            plugins: Arc::new(vec![
                Box::new(PingPlugin),
                Box::new(ClipboardSyncPlugin),
                Box::new(FileTransferPlugin),
            ]),
        }
    }

    pub async fn start(&self) -> Result<(), Box<dyn std::error::Error>> {
        println!("LibreConnect Daemon starting...");

        // Example: Listen for incoming connections
        let listener = TcpListener::bind("127.0.0.1:8080").await?;
        println!("Listening on {}", listener.local_addr()?);

        let paired_devices = self.paired_devices.clone();
        let plugins = self.plugins.clone();

        loop {
            let (mut socket, addr) = listener.accept().await?;
            println!("New connection from {}", addr);

            let paired_devices_clone = paired_devices.clone();
            let plugins_clone = plugins.clone();

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

                    let message: Message = serde_json::from_slice(&buf[0..n])
                        .expect("Failed to deserialize message");

                    match message {
                        Message::RequestPairing(device_info) => {
                            let response_message;
                            {
                                // Acquire lock, perform operations, and release lock within this scope
                                let mut devices = paired_devices_clone.lock().unwrap();
                                devices.insert(device_info.id.clone(), device_info.clone());
                                println!("Paired devices: {:?}", devices);
                                response_message = Message::PairingAccepted(device_info.id.clone());
                            }
                            
                            let response = serde_json::to_vec(&response_message)
                                .expect("Failed to serialize pairing accepted message");
                            if let Err(e) = socket.write_all(&response).await {
                                eprintln!("failed to write to socket; err = {:?}", e);
                                return;
                            }
                        },
                        _ => {
                            // Dispatch message to plugins
                            let sender_id = DeviceId::from(addr.to_string()); // Placeholder for actual sender ID
                            for plugin in plugins_clone.iter() {
                                if let Some(response_message) = plugin.handle_message(&message, &sender_id) {
                                    let response = serde_json::to_vec(&response_message)
                                        .expect("Failed to serialize plugin response");
                                    if let Err(e) = socket.write_all(&response).await {
                                        eprintln!("failed to write to socket; err = {:?}", e);
                                        return;
                                    }
                                }
                            }
                            println!("Received unknown message: {:?}", message);
                        }
                    }
                }
            });
        }
    }
}
