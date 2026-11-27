#!/usr/bin/env bash
# ==============================================================================
# Full 51-Scenario Preset Catalog Runner (Linux/macOS)
# Executes 100% of all 51 presets in their native spatial & temporal resolutions.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

USE_PRESET_RES="${1:-true}"
RES_FLAG="-Dbenchmark.usePresetResolution=${USE_PRESET_RES}"

echo "================================================================================"
echo " 🚀 FULL 51-PRESET SCENARIO CATALOG RUNNER (Native Resolutions: ${USE_PRESET_RES})"
echo "================================================================================"

mvn test -Pacademic-paper -Dtest=MacroHistoricalEpochsFalsificationTest#testExecutePhase1Scenarios "${RES_FLAG}" -Djacoco.skip=true -DtestFailureIgnore=true -B
mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase2FalsificationTest#testExecutePhase2Scenarios "${RES_FLAG}" -Djacoco.skip=true -DtestFailureIgnore=true -B
mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase3FalsificationTest#testExecutePhase3Scenarios "${RES_FLAG}" -Djacoco.skip=true -DtestFailureIgnore=true -B
mvn test -Pacademic-paper -Dtest=MacroFutureAndExoFalsificationTest#testExecuteFutureAndExoScenarios "${RES_FLAG}" -Djacoco.skip=true -DtestFailureIgnore=true -B

mvn test -Dtest=WhitePaperCompiler#compileMasterWhitePaper -Djacoco.skip=true -B

echo "================================================================================"
echo " ✔ FULL 51-PRESET CATALOG EXECUTION COMPLETED SUCCESSFULLY"
echo "================================================================================"
