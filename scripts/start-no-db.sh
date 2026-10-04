#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation - Quick Start (No Database / Offline Mode)
# Launches JavaFX application without starting Docker PostgreSQL
# ==============================================================================
cd "$(dirname "$0")/.."

echo "========================================"
echo "Ether Simulation - Quick Start"
echo "(Running in Database Offline Mode)"
echo "========================================"
echo ""

JAR_PATH="target/society-simulation-1.0.0-beta.1-executable.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Building optimized executable JAR..."
    mvn clean package -DskipTests
fi

echo "Launching Ether with Native SIMD Vectorization & G1GC..."
java --add-modules jdk.incubator.vector -XX:+UseG1GC -Xms2g -Xmx12g -jar "$JAR_PATH"
