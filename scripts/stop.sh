#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Stop Database Container
# ==============================================================================
cd "$(dirname "$0")/.."

echo "========================================"
echo "Ether Simulation - Shutting Down DB..."
echo "========================================"
docker-compose down
echo "Database container shut down."
