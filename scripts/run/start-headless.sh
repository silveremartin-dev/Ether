#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Start Headless Engine Batch Runner (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

SCENARIO="${1:-OUT_OF_AFRICA}"
TICKS="${2:-300}"
CELLS="${3:-3000}"

echo "=========================================================="
echo "     ETHER -- HEADLESS ENGINE BATCH RUNNER (Linux)        "
echo "=========================================================="
echo "  Scenario Preset : $SCENARIO"
echo "  Target Ticks    : $TICKS"
echo "  H3 Grid Cells   : $CELLS"
echo "----------------------------------------------------------"

EXEC_JAR=""
if [ -f "bin/ether.jar" ]; then
    EXEC_JAR="bin/ether.jar"
elif ls target/*executable.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
elif ls target/society-simulation-*.jar 1> /dev/null 2>&1; then
    EXEC_JAR=$(ls target/society-simulation-*.jar | grep -v "sources" | grep -v "javadoc" | head -n 1)
fi

if [ -z "$EXEC_JAR" ]; then
    echo "Building executable JAR..."
    mvn clean package -DskipTests
    EXEC_JAR=$(ls target/*executable.jar | head -n 1)
fi

echo "Executing Headless Batch Run..."
java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar "$EXEC_JAR" --headless --scenario="$SCENARIO" --ticks="$TICKS" --cells="$CELLS" --profile
