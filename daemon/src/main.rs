use daemon::Daemon;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Starting LibreConnect Daemon...");

    let daemon = Daemon::new();
    daemon.start().await?;

    Ok(())
}
