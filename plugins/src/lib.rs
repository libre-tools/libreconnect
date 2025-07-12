use shared::{Message, DeviceId};

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