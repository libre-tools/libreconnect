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
