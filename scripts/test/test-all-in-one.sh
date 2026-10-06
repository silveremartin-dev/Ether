#!/usr/bin/env bash
# ==============================================================================
# Ether Simulation Engine — All-In-One Test Harness (Bash)
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "==============================================================================="
echo "           ETHER SIMULATION ENGINE -- ALL-IN-ONE TEST HARNESS                  "
echo "==============================================================================="
echo ""

FAILURES=0

echo "[1/4] Running Java Compilation and Unit Tests..."
if mvn test -Dtest="*Test" -DfailIfNoSpecifiedTests=false; then
    echo "[PASS] Unit tests completed successfully."
else
    echo "[FAIL] Unit tests encountered errors!"
    FAILURES=$((FAILURES + 1))
fi

echo ""
echo "[2/4] Running Master Historical Bifurcation Suite..."
if mvn test -Dtest="MasterHistoricalBifurcationSuite" -DfailIfNoSpecifiedTests=false; then
    echo "[PASS] Master Historical Bifurcation Suite passed."
else
    echo "[FAIL] Master Historical Bifurcation Suite failed!"
    FAILURES=$((FAILURES + 1))
fi

echo ""
echo "[3/4] Running Empirical Residual and Metastability Suite..."
if mvn test -Dtest="EmpiricalResidualBifurcationTest" -DfailIfNoSpecifiedTests=false; then
    echo "[PASS] Empirical Residual Suite passed."
else
    echo "[FAIL] Empirical Residual Suite failed!"
    FAILURES=$((FAILURES + 1))
fi

echo ""
echo "[4/4] Running Historical Leader A/B Falsification Suite..."
if mvn test -Dtest="HistoricalLeaderBifurcationTest" -DfailIfNoSpecifiedTests=false; then
    echo "[PASS] Historical Leader Suite passed."
else
    echo "[FAIL] Historical Leader Suite failed!"
    FAILURES=$((FAILURES + 1))
fi

echo ""
echo "==============================================================================="
if [ $FAILURES -eq 0 ]; then
    echo "[SUCCESS] ALL TEST SUITES PASSED CLEANLY (0 failures)!"
    echo "==============================================================================="
    exit 0
else
    echo "[ERROR] $FAILURES TEST SUITE(S) FAILED. Please review the logs above."
    echo "==============================================================================="
    exit 1
fi
