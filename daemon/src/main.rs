use clap::Parser;
use daemon::Daemon;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value_t = shared::DEFAULT_PORT)]
    port: u16,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Args::parse();
    println!("Starting LibreConnect Daemon on port {}...", args.port);

    let daemon = Daemon::new(args.port);
    daemon.start().await?;

    Ok(())
}
