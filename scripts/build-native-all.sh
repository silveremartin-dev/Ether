#!/usr/bin/env bash
set -euo pipefail

echo "=========================================================="
echo "🦀 Building Ether Native Core for Windows, Linux & macOS..."
echo "=========================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR/native/ether-core-native"

echo "[1/3] 🐧 Building Linux native (.so)..."
cargo build --release

echo "[2/3] 🐧 Building Linux ARM64 cross (.so)..."
cargo build --release --target aarch64-unknown-linux-gnu || echo "Skipping aarch64 if toolchain missing"

echo "[3/3] 🍏 Building macOS targets (.dylib)..."
cargo build --release --target x86_64-apple-darwin || true
cargo build --release --target aarch64-apple-darwin || true

echo "✅ Build script completed."
