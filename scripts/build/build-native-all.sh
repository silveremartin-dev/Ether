#!/usr/bin/env bash
# ==============================================================================
# Ether Native Core Builder (Linux .so, macOS .dylib, Windows .dll via Cargo)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
NATIVE_DIR="$ROOT_DIR/ether-core-native"

echo "=========================================================="
echo " Building Ether Native Core for Linux, macOS and Windows... "
echo "=========================================================="

if [ ! -d "$NATIVE_DIR" ]; then
    echo "[INFO] Native crate directory ($NATIVE_DIR) not found. Skipping."
    exit 0
fi

cd "$NATIVE_DIR"

if ! command -v cargo &> /dev/null; then
    echo "[WARN] Rust / Cargo toolchain not found. Skipping native compilation."
    exit 0
fi

echo "[1/3] Building host target with release optimizations..."
cargo build --release

echo "[2/3] Building Linux targets (if toolchain present)..."
cargo build --release --target x86_64-unknown-linux-gnu 2>/dev/null || echo "[INFO] Linux cross-target skipped"

echo "[3/3] Building macOS targets (if toolchain present)..."
cargo build --release --target x86_64-apple-darwin 2>/dev/null || echo "[INFO] macOS cross-target skipped"

echo "Build script completed successfully."
