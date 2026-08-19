#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Docker + App Launcher
# Starts PostgreSQL in Docker Compose and launches JavaFX application.
# ==============================================================================
cd "$(dirname "$0")/.."

echo "========================================"
echo "Ether Simulation - Starting with DB..."
echo "========================================"
echo ""

if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker is not installed or not in PATH!"
    exit 1
fi

echo "[1/4] Docker check passed ✓"
echo ""

echo "[2/4] Starting PostgreSQL database..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "[ERROR] Failed to start database container!"
    exit 1
fi

echo "      Database container started ✓"
echo ""

echo "[3/4] Waiting for database readiness..."
until docker-compose exec -T postgres pg_isready -U ether > /dev/null 2>&1; do
    echo "      Waiting for PostgreSQL..."
    sleep 2
done

echo "      Database is ready ✓"
echo ""

echo "[4/4] Launching Ether Simulation..."
mvn javafx:run

echo ""
echo "========================================"
echo "Application closed. Stopping Database..."
docker-compose stop
echo "Database container stopped."
echo "========================================"
