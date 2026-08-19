#!/usr/bin/env bash
# ==============================================================================
# Ether 2.0 — Start Cluster Master Server
# Launches Ether in Cluster Master mode to orchestrate worker nodes.
# ==============================================================================

SCENARIO="${1:-OUT_OF_AFRICA}"
PORT="${2:-9090}"
SECRET="${3:-EtherClusterSecret2026}"
TICKS="${4:-500}"
CELLS="${5:-10000}"

echo "=========================================================="
echo "     ETHER 2.0 — STARTING CLUSTER MASTER SERVER           "
echo "=========================================================="
echo "  Scenario Preset : ${SCENARIO}"
echo "  Port            : ${PORT}"
echo "  Target Ticks    : ${TICKS}"
echo "  H3 Grid Cells   : ${CELLS}"
echo "----------------------------------------------------------"

JAR_PATH="target/society-simulation-2.0.0-SNAPSHOT-executable.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building executable JAR..."
    mvn clean package -DskipTests
fi

echo "🚀 Launching Master Node Server..."
java --add-modules jdk.incubator.vector -jar "$JAR_PATH" --headless --mode=cluster --role=master --port="${PORT}" --secret="${SECRET}" --scenario="${SCENARIO}" --ticks="${TICKS}" --cells="${CELLS}" --profile
