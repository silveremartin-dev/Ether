#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Docker Container Teardown
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "========================================"
echo "  Ether Simulation - Docker Teardown"
echo "========================================"
echo ""

echo "Stopping and removing Docker containers..."
if docker compose version >/dev/null 2>&1; then
    docker compose down
else
    docker-compose down
fi

echo ""
echo "[SUCCESS] PostgreSQL & Redis containers stopped and removed ✓"
echo "========================================"
