#!/usr/bin/env bash
# ==============================================================================
# ETHER HISTORICAL BIFURCATION BENCHMARK RUNNER (BASH)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

MODE="${1:-master}"

echo "================================================================="
echo "  ETHER CLIOCLOUD: HISTORICAL BIFURCATION & RUPTURE BENCHMARK    "
echo "  Execution Mode: $MODE                                          "
echo "================================================================="

case "$MODE" in
    master)
        echo "[1/1] Running Master Historical Bifurcation Suite (7 Scenarios + Theorems)..."
        mvn test -Dtest=MasterHistoricalBifurcationSuite
        ;;
    residual)
        echo "[1/1] Running Empirical Residual & Metastability Suite..."
        mvn test -Dtest=EmpiricalResidualBifurcationTest
        ;;
    leaders)
        echo "[1/1] Running Historical Leader A/B Falsification Suite..."
        mvn test -Dtest=HistoricalLeaderBifurcationTest
        ;;
    all)
        echo "[1/3] Running Master Historical Bifurcation Suite..."
        mvn test -Dtest=MasterHistoricalBifurcationSuite
        echo "[2/3] Running Empirical Residual Inversion Suite..."
        mvn test -Dtest=EmpiricalResidualBifurcationTest
        echo "[3/3] Running Historical Leader A/B Falsification Suite..."
        mvn test -Dtest=HistoricalLeaderBifurcationTest
        ;;
    macro)
        echo "[PLANETARY BATCH] Running 41,162 H3 cell headless simulations..."
        mvn exec:java -Dexec.mainClass="org.ether.society.core.headless.HeadlessBatchRunner"
        ;;
    *)
        echo "Unknown mode: $MODE. Choose from: master, residual, leaders, all, macro"
        exit 1
        ;;
esac

echo ""
echo "[COMPLETED] Bifurcation benchmark execution finished."
