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
        echo "[INFO] Usage options:"
        echo "  ./run_scientific_tests.sh --debates       [Pillar 1: 8 Canonical Debates]"
        echo "  ./run_scientific_tests.sh --steampunk     [Pillar 2: Popperian Negative Control (Steampunk Rome)]"
        echo "  ./run_scientific_tests.sh --contingency   [Pillar 3: Great Man Theory & Bifurcation Benchmarks]"
        echo "  ./run_scientific_tests.sh --multiscale    [Pillar 4: Multiscale Spatio-Temporal Invariance & Isolation]"
        echo "  ./run_scientific_tests.sh --benchmark     [Pillar 5: Empirical Ground-Truth Scenario Audits]"
        echo "  ./run_scientific_tests.sh --inference     [Pillar 6: Bayesian ABC-SMC & Sobol Sensitivity]"
        echo "  ./run_scientific_tests.sh --formal        [Pillar 7: Offline Formal Verification & Invariants]"
        echo "  ./run_scientific_tests.sh --all           [Runs All Academic Paper Suites in org.ether.society.falsification.*]"
        echo ""
        echo "[RUN] Defaulting to Pillar 1: 8 Canonical Scientific Debates..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
        ;;
    "--debates")
        echo "[RUN] Executing Pillar 1: 8 Canonical Scientific Debates..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
        ;;
    "--steampunk"|"--counterfactual")
        echo "[RUN] Executing Pillar 2: Popperian Negative-Control Falsifier (Steampunk Rome 165 CE)..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
        ;;
    "--great-man"|"--contingency")
        echo "[RUN] Executing Pillar 3: Great Man Contingency vs Determinism..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
        ;;
    "--multiscale")
        echo "[RUN] Executing Pillar 4: Multiscale Spatio-Temporal Invariance & Isolation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
        ;;
    "--benchmark"|"--benchmarks")
        echo "[RUN] Executing Pillar 5: Empirical Ground-Truth Benchmarks..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
        ;;
    "--inference")
        echo "[RUN] Executing Pillar 6: Bayesian Inverse Inference & Sobol Sensitivity..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
        ;;
    "--formal")
        echo "[RUN] Executing Pillar 7: Offline Formal Verification & Physical Invariants..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
        ;;
    "--all")
        echo "[RUN 1/7] Executing Pillar 1: 8 Canonical Scientific Debates..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.debates.**.*"
        echo "[RUN 2/7] Executing Pillar 2: Popperian Negative-Control Counterfactual..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.counterfactual.**.*"
        echo "[RUN 3/7] Executing Pillar 3: Great Man Contingency vs Determinism..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.contingency.**.*"
        echo "[RUN 4/7] Executing Pillar 4: Multiscale Spatio-Temporal Invariance & Isolation..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.multiscale.**.*"
        echo "[RUN 5/7] Executing Pillar 5: Empirical Ground-Truth Benchmarks..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.benchmark.**.*"
        echo "[RUN 6/7] Executing Pillar 6: Bayesian Inverse Inference & Sobol Sensitivity..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.inference.**.*"
        echo "[RUN 7/7] Executing Pillar 7: Offline Formal Verification & Physical Invariants..."
        mvn test -Pacademic-paper "-Dtest=org.ether.society.falsification.formal.**.*"
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
