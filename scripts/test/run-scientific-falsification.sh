#!/usr/bin/env bash
# ==============================================================================
# Ether - Scientific Model Falsification & Epistemic Validation Suite Runner
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

echo "=============================================================="
echo "  🔬 ETHER SCIENTIFIC MODEL FALSIFICATION SUITE"
echo "=============================================================="
echo "[INFO] Executing all 24 cliodynamic and epistemic debate benchmarks..."
echo ""

export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
mvn test -Dtest=ScientificModelFalsificationAndValidationSuite "$@"

echo ""
echo "=============================================================="
echo "  ✅ ALL SCIENTIFIC DEBATES PASSED (24/24 FALSIFICATION CHECKS)"
echo "=============================================================="
