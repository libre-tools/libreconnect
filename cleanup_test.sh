#!/bin/bash
echo "🧹 Cleaning up LibreConnect test environment..."
pkill -f libreconnectd
rm -f daemon.log nohup.out
echo "✅ Cleanup complete"
