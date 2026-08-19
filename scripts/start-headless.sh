#!/usr/bin/env bash
# ==============================================================================
# Ether 2.0 — Start Headless Engine Batch Runner
# Executes high-speed batch simulation without JavaFX UI overhead.
# ==============================================================================

SCENARIO="${1:-OUT_OF_AFRICA}"
TICKS="${2:-300}"
CELLS="${3:-3000}"

echo "=========================================================="
echo "     ETHER 2.0 — HEADLESS ENGINE BATCH RUNNER             "
echo "=========================================================="
echo "  Scenario Preset : ${SCENARIO}"
echo "  Target Ticks    : ${TICKS}"
echo "  H3 Grid Cells   : ${CELLS}"
echo "----------------------------------------------------------"

JAR_PATH="target/society-simulation-2.0.0-SNAPSHOT-executable.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building executable JAR..."
    mvn clean package -DskipTests
fi

echo "🚀 Executing Headless Batch Run..."
java --add-modules jdk.incubator.vector -jar "$JAR_PATH" --headless --scenario="${SCENARIO}" --ticks="${TICKS}" --cells="${CELLS}" --profile
