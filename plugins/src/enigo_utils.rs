//! Utility functions for Enigo (input simulation) initialization.

use enigo::Enigo;

/// Creates and returns a new Enigo instance.
pub fn create_enigo_instance() -> Result<Enigo, Box<dyn std::error::Error>> {
    Enigo::new(&enigo::Settings::default())
        .map_err(|e| format!("Failed to initialize input simulation: {e}").into())
}
