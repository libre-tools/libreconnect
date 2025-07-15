use std::process::Command;

#[test]
fn test_cli_help() {
    let output = Command::new("cargo")
        .args(["run", "--bin", "libreconnect-cli", "--", "--help"])
        .output()
        .expect("Failed to execute CLI");

    assert!(output.status.success());
    let stdout = String::from_utf8_lossy(&output.stdout);
    assert!(stdout.contains("CLI") || stdout.contains("Usage"));
}

#[test]
fn test_cli_start_daemon_command() {
    let output = Command::new("cargo")
        .args(["run", "--bin", "libreconnect-cli", "--", "start-daemon"])
        .output()
        .expect("Failed to execute CLI");

    assert!(output.status.success());
    let stdout = String::from_utf8_lossy(&output.stdout);
    assert!(stdout.contains("Starting LibreConnect daemon"));
}

#[test]
fn test_cli_invalid_command() {
    let output = Command::new("cargo")
        .args(["run", "--bin", "libreconnect-cli", "--", "invalid-command"])
        .output()
        .expect("Failed to execute CLI");

    assert!(!output.status.success());
}

#[test]
fn test_cli_send_key_event_requires_args() {
    let output = Command::new("cargo")
        .args(["run", "--bin", "libreconnect-cli", "--", "send-key-event"])
        .output()
        .expect("Failed to execute CLI");

    assert!(!output.status.success());
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(stderr.contains("required") || stderr.contains("missing"));
}
