#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Quick Start (No Database / Offline Mode)
# Launches JavaFX application without starting Docker PostgreSQL
# ==============================================================================
cd "$(dirname "$0")/.."

echo "========================================"
echo "Ether Simulation - Quick Start"
echo "(Running in Database Offline Mode)"
echo "========================================"
echo ""

mvn javafx:run
