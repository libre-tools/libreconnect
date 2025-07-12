use shared::{Message, DeviceId, KeyEvent, MouseEvent};

pub trait Plugin: Send + Sync {
    fn name(&self) -> &'static str;
    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Option<Message>;
}

pub struct PingPlugin;

impl Plugin for PingPlugin {
    fn name(&self) -> &'static str {
        "ping"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Option<Message> {
        match message {
            Message::Ping => {
                println!("Ping received from {}. Sending Pong.", sender_id);
                Some(Message::Pong)
            },
            Message::Pong => {
                println!("Pong received from {}.", sender_id);
                None
            },
            _ => None,
        }
    }
}

pub struct ClipboardSyncPlugin;

impl Plugin for ClipboardSyncPlugin {
    fn name(&self) -> &'static str {
        "clipboard-sync"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Option<Message> {
        match message {
            Message::ClipboardSync(content) => {
                println!("Clipboard content received from {}: {}", sender_id, content);
                // In a real scenario, you'd update the local clipboard here
                None
            },
            Message::RequestClipboard => {
                println!("Clipboard request received from {}.", sender_id);
                // In a real scenario, you'd read the local clipboard and send it back
                Some(Message::ClipboardSync("Simulated clipboard content".to_string()))
            },
            _ => None,
        }
    }
}

pub struct FileTransferPlugin;

impl Plugin for FileTransferPlugin {
    fn name(&self) -> &'static str {
        "file-transfer"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Option<Message> {
        match message {
            Message::FileTransferRequest { file_name, file_size } => {
                println!("File transfer request from {}: {} ({} bytes)", sender_id, file_name, file_size);
                // In a real scenario, you'd prepare to receive the file
                None
            },
            Message::FileTransferChunk { file_name, chunk, offset } => {
                println!("Received chunk for {} (offset: {}, size: {}) from {}", file_name, offset, chunk.len(), sender_id);
                // In a real scenario, you'd write the chunk to the file
                None
            },
            Message::FileTransferEnd { file_name } => {
                println!("File transfer for {} ended from {}.", file_name, sender_id);
                // In a real scenario, you'd finalize the file
                None
            },
            Message::FileTransferError { file_name, error } => {
                eprintln!("File transfer error for {} from {}: {}", file_name, sender_id, error);
                None
            },
            _ => None,
        }
    }
}

pub struct InputSharePlugin;

impl Plugin for InputSharePlugin {
    fn name(&self) -> &'static str {
        "input-share"
    }

    fn handle_message(&self, message: &Message, sender_id: &DeviceId) -> Option<Message> {
        match message {
            Message::KeyEvent(key_event) => {
                println!("Key event from {}: {:?}", sender_id, key_event);
                // In a real scenario, you'd simulate the key event
                None
            },
            Message::MouseEvent(mouse_event) => {
                println!("Mouse event from {}: {:?}", sender_id, mouse_event);
                // In a real scenario, you'd simulate the mouse event
                None
            },
            _ => None,
        }
    }
}