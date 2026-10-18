#!/usr/bin/env bash
# ==============================================================================
# Ether Planetary Simulation Engine — 1-Click Master Benchmark Suite (Linux/macOS)
# Runs the complete 5-axis empirical benchmark suite (Spatial, Temporal, Backends,
# Physics Tiers, Historical Epochs) and prints the comprehensive report matrix.
# ==============================================================================

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

echo "=============================================================================="
echo "  ETHER PLANETARY SIMULATION ENGINE — 1-CLICK 5-AXIS BENCHMARK SUITE"
echo "=============================================================================="
echo "Working Directory: ${ROOT_DIR}"

# Locate executable JAR
JAR_PATH="target/society-simulation-1.0.0-beta.2-executable.jar"
if [[ ! -f "${JAR_PATH}" ]]; then
    echo "[*] Executable JAR not found. Building shaded JAR with Maven..."
    mvn clean package -DskipTests
fi

# Detect Native Rust dynamic library
NATIVE_FLAG=""
if [[ -f "/opt/ether/libether_core_native.so" ]]; then
    export LD_LIBRARY_PATH="/opt/ether:${LD_LIBRARY_PATH:-}"
    echo "[*] Detected native library at /opt/ether/libether_core_native.so"
elif [[ -f "native/ether-core-native/target/release/libether_core_native.so" ]]; then
    export LD_LIBRARY_PATH="${ROOT_DIR}/native/ether-core-native/target/release:${LD_LIBRARY_PATH:-}"
fi

# Detect available memory and set heap sizing accordingly
AVAILABLE_MEM_KB=$(grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}' || echo "16000000")
if (( AVAILABLE_MEM_KB > 24000000 )); then
    HEAP_FLAGS="-Xms16g -Xmx24g"
elif (( AVAILABLE_MEM_KB > 12000000 )); then
    HEAP_FLAGS="-Xms8g -Xmx12g"
else
    HEAP_FLAGS="-Xms4g -Xmx6g"
fi

echo "[*] Running with JVM Heap: ${HEAP_FLAGS}"
echo "[*] Launching 5-Axis Real Planetary Simulation Engine Benchmarks..."

java -XX:+UseG1GC ${HEAP_FLAGS} \
    --enable-native-access=ALL-UNNAMED \
    -cp "${JAR_PATH}" \
    org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner "$@"

echo ""
echo "[*] Benchmark execution completed successfully."
