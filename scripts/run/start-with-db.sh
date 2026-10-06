#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Startup Script (GUI + Docker PostgreSQL)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "========================================"
echo "Ether Simulation - Starting with DB..."
echo "========================================"
echo ""

if ! docker info >/dev/null 2>&1; then
    echo "[ERROR] Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

echo "[1/4] Docker is running ✓"
echo ""

echo "[2/4] Starting PostgreSQL database..."
if docker compose version >/dev/null 2>&1; then
    docker compose up -d postgres
else
    docker-compose up -d postgres
fi

echo "[3/4] Waiting for database to be ready..."
until (docker compose exec -T postgres pg_isready -U ether 2>/dev/null || docker-compose exec -T postgres pg_isready -U ether 2>/dev/null); do
    echo "      Still waiting for PostgreSQL..."
    sleep 2
done
echo "      Database is ready ✓"
echo ""

EXEC_JAR=""
if [ -f "bin/ether.jar" ]; then
    EXEC_JAR="bin/ether.jar"
elif ls target/*executable.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
elif ls target/society-simulation-*.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/society-simulation-*.jar | grep -v "sources" | grep -v "javadoc" | head -n 1)
fi

if [ -z "$EXEC_JAR" ]; then
    echo "Building optimized executable JAR..."
    mvn clean package -DskipTests
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
fi

echo "[4/4] Launching Ether Simulation with Native SIMD Vectorization..."
echo ""
java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "$EXEC_JAR" "$@" || \
java -Xms2g -Xmx8g -jar "$EXEC_JAR" "$@"

echo ""
echo "========================================"
echo "Application closed. Stopping Database..."
if docker compose version >/dev/null 2>&1; then
    docker compose stop postgres
else
    docker-compose stop postgres
fi
echo "Database stopped."
echo "========================================"
