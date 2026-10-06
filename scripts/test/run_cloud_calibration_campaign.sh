#!/usr/bin/env bash
# ==============================================================================
# Ether — Cloud Calibration Campaign Runner
# ==============================================================================
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$ROOT_DIR"

mkdir -p logs/campaign
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="logs/campaign/calibration_campaign_${TIMESTAMP}.log"

echo "==============================================================" | tee -a "$LOG_FILE"
echo "  🚀 ETHER MASTER CLOUD CAMPAIGN STARTED: $(date)" | tee -a "$LOG_FILE"
echo "==============================================================" | tee -a "$LOG_FILE"

# Run calibration suite with large heap & incubation vector module
export MAVEN_OPTS="-Xms4g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
mvn test -Dtest=HistoricalScenarioCalibrationHarnessTest -DargLine="-Xms4g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED" 2>&1 | tee -a "$LOG_FILE"

echo "==============================================================" | tee -a "$LOG_FILE"
echo "  ✨ ETHER MASTER CLOUD CAMPAIGN FINISHED: $(date)" | tee -a "$LOG_FILE"
echo "==============================================================" | tee -a "$LOG_FILE"
