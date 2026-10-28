#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 - Master White Paper Generator for 45 Cliodynamic Scenarios (Linux/macOS)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

OUTPUT_DIR="${1:-docs}"
TARGET_FILE="${OUTPUT_DIR}/WHITE_PAPER_45_SCENARIOS_HISTORICAL_BENCHMARK.md"

echo "================================================================================"
echo " [ETHER] MASTER WHITE PAPER GENERATION: 45 SCENARIO AUDIT SUITE"
echo "================================================================================"
echo "  Target Output  : ${TARGET_FILE}"
echo "--------------------------------------------------------------------------------"

START_TIME=$(date +%s)

mvn test -Dtest=WhitePaperCompiler -Djacoco.skip=true

END_TIME=$(date +%s)
ELAPSED=$(( END_TIME - START_TIME ))

echo "================================================================================"
echo " [SUCCESS] WHITE PAPER GENERATED IN ${ELAPSED} SECONDS!"
echo "   File: ${TARGET_FILE}"
echo "================================================================================"
