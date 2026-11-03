#!/usr/bin/env bash
# ==============================================================================
# Ether - Academic Paper & Scientific Model Falsification Test Harness
# ==============================================================================
# This script executes the peer-reviewed falsification suites formalizing
# the 8 cliodynamic debates, Popperian counterfactual tests (Steampunk Rome 165 CE),
# Great Man Theory contingency vs macro-determinism, multiscale isolation & convergence,
# empirical ground truth benchmarks, Bayesian inverse inference, and formal verification.
# All test classes are consolidated in the dedicated package org.ether.society.falsification.*
# ==============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==============================================================================="
echo "     ETHER CLIODYNAMICS - ACADEMIC PAPER & SCIENTIFIC FALSIFICATION"
echo "==============================================================================="
echo ""

TARGET="${1:-}"

export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

case "$TARGET" in
    ""|"--help")
        echo "[INFO] Usage options (Organized in 7 Bottom-Up Epistemological Pillars):"
        echo "  ./run_scientific_tests.sh --formal        [Pillar 1: Formal Invariants & Physical Conservation (Tier 1)]"
        echo "  ./run_scientific_tests.sh --multiscale    [Pillar 2: Multiscale Discretization, Fénétrange & Isolation]"
        echo "  ./run_scientific_tests.sh --inference     [Pillar 3: Bayesian ABC-SMC, Identifiability & Sobol Sensitivity]"
        echo "  ./run_scientific_tests.sh --debates       [Pillar 4: 8 Canonical Cliodynamic Debates (Tier 2)]"
        echo "  ./run_scientific_tests.sh --contingency   [Pillar 5: Historical Contingency & Great Man Theory Falsification]"
        echo "  ./run_scientific_tests.sh --counterfactual [Pillar 6: Popperian Negative Controls (Steampunk Rome 165 CE)]"
        echo "  ./run_scientific_tests.sh --benchmark     [Pillar 7: Deep-Time Empirical Historical Benchmarks]"
        echo "  ./run_scientific_tests.sh --all           [Runs All 7 Pillars sequentially in org.ether.society.falsification.*]"
        echo ""
        echo "[RUN] Defaulting to Pillar 1: Formal Invariants & Physical Conservation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
        ;;
    "--formal")
        echo "[RUN] Executing Pillar 1: Formal Invariants & Physical Conservation (Tier 1)..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
        ;;
    "--multiscale")
        echo "[RUN] Executing Pillar 2: Multiscale Spatiotemporal Discretization & Domain Truncation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
        ;;
    "--inference")
        echo "[RUN] Executing Pillar 3: Bayesian Inverse Inference & Global Identifiability..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
        ;;
    "--debates")
        echo "[RUN] Executing Pillar 4: The 8 Canonical Historiographical Debates..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
        ;;
    "--great-man"|"--contingency")
        echo "[RUN] Executing Pillar 5: Historical Contingency & Macro-Determinism..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
        ;;
    "--steampunk"|"--counterfactual")
        echo "[RUN] Executing Pillar 6: Popperian Falsification & Negative Controls (Steampunk Rome 165 CE)..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
        ;;
    "--benchmark"|"--benchmarks")
        echo "[RUN] Executing Pillar 7: Deep-Time Empirical Historical Benchmarks..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
        ;;
    "--all")
        echo "[RUN 1/7] Executing Pillar 1: Formal Invariants & Physical Conservation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
        echo "[RUN 2/7] Executing Pillar 2: Multiscale Spatiotemporal Discretization & Isolation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
        echo "[RUN 3/7] Executing Pillar 3: Bayesian Inverse Inference & Identifiability..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
        echo "[RUN 4/7] Executing Pillar 4: The 8 Canonical Historiographical Debates..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
        echo "[RUN 5/7] Executing Pillar 5: Historical Contingency & Macro-Determinism..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
        echo "[RUN 6/7] Executing Pillar 6: Popperian Falsification & Negative Controls..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
        echo "[RUN 7/7] Executing Pillar 7: Deep-Time Empirical Historical Benchmarks..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
        ;;
    *)
        echo "[CUSTOM] Executing custom test target: $TARGET"
        mvn test -Pacademic-paper "-Dtest=$TARGET"
        ;;
esac

echo ""
echo "==============================================================================="
echo "  [SUCCESS] SCIENTIFIC FALSIFICATION TESTS PASSED CLEANLY (0 REGRESSIONS)"
echo "==============================================================================="
