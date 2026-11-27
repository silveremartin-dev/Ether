#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 - 9 Continuous Macro-Epochs (-100,000 BP -> 2026 CE)
#             & Future Biophysical Calibration (2025 -> 2200 CE) Runner (Linux/macOS)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

FUTURE_END_YEAR="${1:-2200}"
export MAVEN_OPTS="-Xms4g -Xmx16g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

echo "================================================================================"
echo " 🌍 ETHER: 9 CONTINUOUS EPOCHS (-100,000 BP -> 2026 CE) & FUTURE CALIBRATION (2025 -> ${FUTURE_END_YEAR} CE)"
echo "================================================================================"

mvn test-compile -Djacoco.skip=true -B

echo ">>> [1/2] Executing All 9 Continuous Macro-Historical Epochs (EPOCH_1 .. EPOCH_9)..."
mvn exec:java -Dexec.mainClass=org.ether.society.analytics.HistoricalScenarioCalibrationHarness -Dexec.args="--epochs-only" -B

echo ">>> [2/2] Executing Future Earth Biophysical Calibration (2025 -> ${FUTURE_END_YEAR} CE)..."
mvn exec:java -Dexec.mainClass=org.ether.society.analytics.EtherFutureCalibrationRunner -Dfuture.calibration.endYear="${FUTURE_END_YEAR}" -B

echo "================================================================================"
echo " ✅ 9 CONTINUOUS EPOCHS & FUTURE CALIBRATION COMPLETED SUCCESSFULLY"
echo "================================================================================"
