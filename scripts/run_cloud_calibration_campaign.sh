#!/bin/bash
set -e
cd /home/silve/Ether

mkdir -p logs/campaign
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="logs/campaign/calibration_campaign_${TIMESTAMP}.log"

echo "==============================================================" | tee -a "$LOG_FILE"
echo "  🚀 ETHER MASTER CLOUD CAMPAIGN STARTED: $(date)" | tee -a "$LOG_FILE"
echo "==============================================================" | tee -a "$LOG_FILE"

# Run calibration suite with 24GB heap & incubation vector module
export MAVEN_OPTS="-Xms8g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
mvn test -Dtest=HistoricalScenarioCalibrationHarnessTest -DargLine="-Xms8g -Xmx24g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED" 2>&1 | tee -a "$LOG_FILE"

echo "==============================================================" | tee -a "$LOG_FILE"
echo "  ✨ ETHER MASTER CLOUD CAMPAIGN FINISHED: $(date)" | tee -a "$LOG_FILE"
echo "==============================================================" | tee -a "$LOG_FILE"
