#!/usr/bin/env bash
# ==============================================================================
# Ether - Internal Development Test Suite Runner
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$SCRIPT_DIR"

echo "==============================================================================="
echo "           ETHER SIMULATION ENGINE -- INTERNAL TEST HARNESS"
echo "==============================================================================="
echo ""

TARGET="${1:---all}"
export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

case "$TARGET" in
    --tier1)
        echo "[RUN] Executing Tier 1 Biophysics & Conservation Kernels..."
        mvn test "-Dtest=org.ether.society.engines.tier1.**.*Test"
        ;;
    --tier2)
        echo "[RUN] Executing Tier 2 Cliodynamic Theories & Historical Engines..."
        mvn test "-Dtest=org.ether.society.engines.tier2.**.*Test"
        ;;
    --core)
        echo "[RUN] Executing Core DoD, Panama WorldBuffer & Symplectic Integrator Tests..."
        mvn test "-Dtest=org.ether.society.core.**.*Test"
        ;;
    --all)
        echo "[RUN] Executing all internal test suites (excluding heavy academic/benchmarks)..."
        mvn test
        ;;
    *)
        echo "[CUSTOM] Executing custom internal test target: $TARGET"
        mvn test "-Dtest=$TARGET"
        ;;
esac

echo ""
echo "==============================================================================="
echo "  [SUCCESS] ALL INTERNAL TESTS PASSED CLEANLY (0 REGRESSIONS)"
echo "==============================================================================="
