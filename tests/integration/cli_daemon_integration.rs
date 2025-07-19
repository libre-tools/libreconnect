//! Integration tests for CLI and Daemon interaction.

use tokio::process::Command;
use tokio::time::{timeout, Duration};
use cli::{send_message};
use shared::{Message};

const DAEMON_STARTUP_TIMEOUT: Duration = Duration::from_secs(10);
const CLI_COMMAND_TIMEOUT: Duration = Duration::from_secs(5);

async fn wait_for_daemon_startup(port: u16) -> Result<(), Box<dyn std::error::Error>> {
    let start_time = tokio::time::Instant::now();
    loop {
        if tokio::time::Instant::now().duration_since(start_time) > DAEMON_STARTUP_TIMEOUT {
            return Err("Daemon did not start in time".into());
        }
        // Try to ping the daemon to ensure it's ready
        match send_message(&Message::Ping, true).await {
            Ok(Some(Message::Pong)) => {
                tokio::time::sleep(Duration::from_millis(200)).await; // Give daemon a moment to fully initialize
                return Ok(());
            },
            _ => tokio::time::sleep(Duration::from_millis(500)).await,
        }
    }
}

#[tokio::test]
async fn test_cli_ping_daemon() -> Result<(), Box<dyn std::error::Error>> {
    // Ensure no daemon is running from previous tests
    Command::new("killall").arg("libreconnectd").output().await.ok();
    tokio::time::sleep(Duration::from_secs(3)).await; // Increased sleep for port release

    let port = 1716; // Use a fixed port for now, will randomize later

    // 1. Start the daemon in the background
    println!("Starting daemon for integration test...");
    let mut daemon_process = Command::new("cargo")
        .arg("run")
        .arg("--package")
        .arg("daemon")
        .arg("--bin")
        .arg("libreconnectd")
        .arg("--").arg("--port").arg(port.to_string())
        .kill_on_drop(true) // Ensure daemon is killed when test finishes
        .spawn()?;

    // Wait for the daemon to be ready
    wait_for_daemon_startup(port).await?;

    // 2. Execute the CLI ping command
    println!("Executing CLI ping command...");
    let cli_output = timeout(
        CLI_COMMAND_TIMEOUT,
        Command::new("cargo")
            .arg("run")
            .arg("--package")
            .arg("cli")
            .arg("--bin")
            .arg("libreconnect-cli")
            .arg("ping-daemon")
            .arg("--").arg("--port").arg(port.to_string())
            .output(),
    )
    .await??;

    // 3. Verify the CLI output
    let stdout = String::from_utf8_lossy(&cli_output.stdout);
    let stderr = String::from_utf8_lossy(&cli_output.stderr);

    println!("CLI stdout:\n{}\n", stdout);
    println!("CLI stderr:\n{}\n", stderr);

    assert!(cli_output.status.success());
    assert!(stdout.contains("✅ Daemon is running and responding"));

    // 4. Kill the daemon process
    daemon_process.kill().await?;

    Ok(())
}

#[tokio::test]
async fn test_cli_set_and_get_clipboard() -> Result<(), Box<dyn std::error::Error>> {
    // Ensure no daemon is running from previous tests
    Command::new("killall").arg("libreconnectd").output().await.ok();
    tokio::time::sleep(Duration::from_secs(3)).await; // Increased sleep for port release

    let port = 1716; // Use a fixed port for now, will randomize later

    // 1. Start the daemon in the background
    println!("Starting daemon for integration test...");
    let mut daemon_process = Command::new("cargo")
        .arg("run")
        .arg("--package")
        .arg("daemon")
        .arg("--bin")
        .arg("libreconnectd")
        .arg("--port").arg(port.to_string())
        .kill_on_drop(true)
        .spawn()?;

    wait_for_daemon_startup(port).await?;

    // 2. Set clipboard content via CLI
    println!("Executing CLI set-clipboard command...");
    let test_content = "Hello from integration test!";
    let set_output = timeout(
        CLI_COMMAND_TIMEOUT,
        Command::new("cargo")
            .arg("run")
            .arg("--package")
            .arg("cli")
            .arg("--bin")
            .arg("libreconnect-cli")
            .arg("--")
            .arg("set-clipboard")
            .arg("test-device") // Device ID as positional argument
            .arg(test_content)
            .arg("--port").arg(port.to_string())
            .output(),
    )
    .await??;

    let set_stdout = String::from_utf8_lossy(&set_output.stdout);
    let set_stderr = String::from_utf8_lossy(&set_output.stderr);

    println!("SetClipboard stdout:\n{}\n", set_stdout);
    println!("SetClipboard stderr:\n{}\n", set_stderr);

    assert!(set_output.status.success());
    assert!(set_stdout.contains("✅ Clipboard content sent successfully"));

    // Give some time for the daemon to process the message
    tokio::time::sleep(Duration::from_millis(500)).await;

    // 3. Get clipboard content via CLI
    println!("Executing CLI get-clipboard command...");
    let get_output = timeout(
        CLI_COMMAND_TIMEOUT,
        Command::new("cargo")
            .arg("run")
            .arg("--package")
            .arg("cli")
            .arg("--bin")
            .arg("libreconnect-cli")
            .arg("--")
            .arg("get-clipboard")
            .arg("test-device") // Device ID as positional argument
            .arg("--port").arg(port.to_string())
            .output(),
    )
    .await??;

    let get_stdout = String::from_utf8_lossy(&get_output.stdout);
    let get_stderr = String::from_utf8_lossy(&get_output.stderr);

    println!("GetClipboard stdout:\n{}\n", get_stdout);
    println!("GetClipboard stderr:\n{}\n", get_stderr);

    assert!(get_output.status.success());
    assert!(get_stdout.contains(&format!("📋 Clipboard content: {}\n", test_content)));

    // 4. Kill the daemon process
    daemon_process.kill().await?;

    Ok(())
}

#[tokio::test]
async fn test_cli_send_file() -> Result<(), Box<dyn std::error::Error>> {
    // Ensure no daemon is running from previous tests
    Command::new("killall").arg("libreconnectd").output().await.ok();
    tokio::time::sleep(Duration::from_secs(3)).await; // Increased sleep for port release

    let port = 1716; // Use a fixed port for now, will randomize later

    // 1. Start the daemon in the background
    println!("Starting daemon for integration test...");
    let mut daemon_process = Command::new("cargo")
        .arg("run")
        .arg("--package")
        .arg("daemon")
        .arg("--bin")
        .arg("libreconnectd")
        .arg("--port").arg(port.to_string())
        .kill_on_drop(true)
        .spawn()?;

    wait_for_daemon_startup(port).await?;

    // Create a dummy file to send
    let temp_dir = tempfile::tempdir()?;
    let file_path = temp_dir.path().join("test_file.txt");
    tokio::fs::write(&file_path, "This is a test file content.").await?;

    // 2. Send the file via CLI
    println!("Executing CLI send-file command...");
    let send_output = timeout(
        CLI_COMMAND_TIMEOUT,
        Command::new("cargo")
            .arg("run")
            .arg("--package")
            .arg("cli")
            .arg("--bin")
            .arg("libreconnect-cli")
            .arg("--")
            .arg("send-file")
            .arg("test-device") // Device ID as positional argument
            .arg(&file_path)
            .arg("--port").arg(port.to_string())
            .output(),
    )
    .await??;

    let send_stdout = String::from_utf8_lossy(&send_output.stdout);
    let send_stderr = String::from_utf8_lossy(&send_output.stderr);

    println!("SendFile stdout:\n{}\n", send_stdout);
    println!("SendFile stderr:\n{}\n", send_stderr);

    assert!(send_output.status.success());
    assert!(send_stdout.contains("✅ File transfer completed successfully!"));

    // Give some time for the daemon to process the message and write the file
    tokio::time::sleep(Duration::from_secs(1)).await;

    // In a real scenario, you would check if the daemon actually received and saved the file.
    // This would require a way to inspect the daemon's state or filesystem.
    // For now, we rely on the CLI's success message.

    // 3. Kill the daemon process
    daemon_process.kill().await?;

    Ok(())
}