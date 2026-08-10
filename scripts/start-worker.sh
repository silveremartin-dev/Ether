#!/usr/bin/env bash
# ==============================================================================
# Ether 2.0 — Start Cluster Compute Worker Node
# Connects to an active Master Server and executes assigned spatial cell chunks.
# ==============================================================================

MASTER_HOST="${1:-127.0.0.1}"
PORT="${2:-9090}"
SECRET="${3:-EtherClusterSecret2026}"

echo "=========================================================="
echo "     ETHER 2.0 — STARTING CLUSTER WORKER NODE             "
echo "=========================================================="
echo "  Master Host : ${MASTER_HOST}"
echo "  Port        : ${PORT}"
echo "----------------------------------------------------------"

JAR_PATH="target/society-simulation-2.0.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building executable JAR..."
    mvn clean package -DskipTests
fi

echo "🔗 Connecting Worker Node to Master at ${MASTER_HOST}:${PORT}..."
java -jar "$JAR_PATH" --mode=cluster --role=worker --master-host="${MASTER_HOST}" --port="${PORT}" --secret="${SECRET}"
