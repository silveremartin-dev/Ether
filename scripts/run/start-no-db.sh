#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Quick Start (No Database)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "========================================"
echo "Ether Simulation - Quick Start"
echo "(Running without database)"
echo "========================================"
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

echo "Launching Ether with Native SIMD Vectorization & G1GC..."
java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "$EXEC_JAR" "$@" || \
java -Xms2g -Xmx8g -jar "$EXEC_JAR" "$@"
