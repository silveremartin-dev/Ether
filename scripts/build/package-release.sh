#!/usr/bin/env bash
# ==============================================================================
# Automated multi-platform release packager for Ether Simulation Engine
# Linux / macOS Bash Parity Partner for scripts/build/package-release.ps1
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/deploy-release-all.sh" "$@"
