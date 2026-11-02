#!/usr/bin/env bash
# ==============================================================================
# Ether - Academic Paper & Scientific Model Falsification Test Harness
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$SCRIPT_DIR"

echo "==============================================================================="
echo "     ETHER CLIODYNAMICS - ACADEMIC PAPER & SCIENTIFIC FALSIFICATION"
echo "==============================================================================="
echo ""

TARGET="${1:---debates}"
export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

case "$TARGET" in
    --debates)
        echo "[RUN] Executing 8 Canonical Scientific Debates (ScientificModelFalsificationAndValidationSuite)..."
        mvn test -Pacademic-paper -Dtest=ScientificModelFalsificationAndValidationSuite
        ;;
    --steampunk)
        echo "[RUN] Executing Steampunk Rome 165 CE Counterfactual Falsifier (CounterfactualInverseFalsificationTest)..."
        mvn test -Pacademic-paper -Dtest=CounterfactualInverseFalsificationTest
        ;;
    --great-man)
        echo "[RUN] Executing Great Man Theory Contingency Benchmarks..."
        mvn test "-Dtest=HistoricalLeaderBifurcationTest,MasterHistoricalBifurcationSuite,VarianceDecompositionTest,TopologicalConvergenceTest"
        ;;
    --all)
        echo "[RUN 1/3] Executing 8 Canonical Scientific Debates..."
        mvn test -Pacademic-paper -Dtest=ScientificModelFalsificationAndValidationSuite
        echo "[RUN 2/3] Executing Great Man Theory Contingency Benchmarks..."
        mvn test "-Dtest=HistoricalLeaderBifurcationTest,MasterHistoricalBifurcationSuite,VarianceDecompositionTest,TopologicalConvergenceTest"
        echo "[RUN 3/3] Executing Steampunk Rome 165 CE Counterfactual Falsifier..."
        mvn test -Pacademic-paper -Dtest=CounterfactualInverseFalsificationTest
        ;;
    *)
        echo "[CUSTOM] Executing custom test target: $TARGET"
        mvn test -Pacademic-paper -Dtest="$TARGET"
        ;;
esac

echo ""
echo "==============================================================================="
echo "  [SUCCESS] SCIENTIFIC FALSIFICATION TESTS PASSED CLEANLY (0 REGRESSIONS)"
echo "==============================================================================="
