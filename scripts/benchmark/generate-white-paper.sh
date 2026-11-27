#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 - Master White Paper & Academic Manuscript Compiler (Linux/macOS)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

OUTPUT_DIR="${1:-docs}"
TARGET_FILE="${OUTPUT_DIR}/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
ACADEMIC_FILE="${OUTPUT_DIR}/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"

echo "================================================================================"
echo " [ETHER] MASTER WHITE PAPER & ACADEMIC MANUSCRIPT COMPILER"
echo "================================================================================"
echo "  White Paper    : ${TARGET_FILE}"
echo "  Academic Paper : ${ACADEMIC_FILE}"
echo "--------------------------------------------------------------------------------"

START_TIME=$(date +%s)

mvn test -Dtest=WhitePaperCompiler#compileMasterWhitePaper -Djacoco.skip=true -B

END_TIME=$(date +%s)
ELAPSED=$(( END_TIME - START_TIME ))

test -f "${TARGET_FILE}"
test -f "${ACADEMIC_FILE}"

echo "================================================================================"
echo " [SUCCESS] SCIENTIFIC DOCUMENTS UPDATED IN ${ELAPSED} SECONDS!"
echo "   White Paper    : ${TARGET_FILE}"
echo "   Academic Paper : ${ACADEMIC_FILE}"
echo "================================================================================"
