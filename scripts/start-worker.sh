#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Start Cluster Compute Worker Node
# Connects to an active Master Server and executes assigned spatial cell chunks.
# ==============================================================================

MASTER_HOST="${1:-127.0.0.1}"
PORT="${2:-9090}"
SECRET="${3:-EtherClusterSecret2026}"

echo "=========================================================="
echo "     ETHER — STARTING CLUSTER WORKER NODE (v1.0 b1)       "
echo "=========================================================="
echo "  Master Host : ${MASTER_HOST}"
echo "  Port        : ${PORT}"
echo "----------------------------------------------------------"

JAR_PATH="target/society-simulation-1.0.0-beta.1-executable.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building executable JAR..."
    mvn clean package -DskipTests
fi

echo "🔗 Connecting Worker Node to Master at ${MASTER_HOST}:${PORT}..."
java --add-modules jdk.incubator.vector -jar "$JAR_PATH" --headless --mode=cluster --role=worker --master-host="${MASTER_HOST}" --port="${PORT}" --secret="${SECRET}"
