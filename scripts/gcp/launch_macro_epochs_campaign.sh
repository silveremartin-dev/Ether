#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Macro Historical Epochs (9 Test Scenarios) GCP Runner
# Runs MacroHistoricalEpochsFalsificationTest with incubator vector modules,
# high-throughput memory configuration, auto-log tracking, and optional auto-shutdown.
# ==============================================================================
set -euo pipefail

CAMPAIGN_DIR="/home/silve/Ether"
LOG_FILE="/home/silve/campaign_9_scenarios.log"
PID_FILE="/home/silve/campaign_9_scenarios.pid"
DONE_FLAG="/home/silve/campaign_9_scenarios_done.flag"

cd "${CAMPAIGN_DIR}"

echo "================================================================================"
echo " 🚀 ETHER 9-SCENARIO HISTORICAL BENCHMARK & FALSIFICATION CAMPAIGN"
echo "================================================================================"
echo "Timestamp: $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
echo "Host Machine: $(uname -a)"
echo "Available Cores: $(nproc)"
echo "Target: MacroHistoricalEpochsFalsificationTest (9 Master Historical Epochs)"
echo "Log destination: ${LOG_FILE}"
echo "================================================================================"

rm -f "${DONE_FLAG}"

export MAVEN_OPTS="--add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xms12g -Xmx40g"

mvn test -Pacademic-paper \
         -Dtest=MacroHistoricalEpochsFalsificationTest \
         -Djacoco.skip=true \
         -DexcludedGroups=none > "${LOG_FILE}" 2>&1
EXIT_CODE=$?

echo "================================================================================" >> "${LOG_FILE}"
echo "Finished at: $(date -u '+%Y-%m-%d %H:%M:%S UTC') with exit code: ${EXIT_CODE}" >> "${LOG_FILE}"
echo "================================================================================" >> "${LOG_FILE}"

echo "${EXIT_CODE}" > "${DONE_FLAG}"

# Bundle report artifacts
tar -czf /home/silve/latest_9_scenarios_reports.tar.gz docs/reports/ docs/images/charts/ 2>/dev/null || true

if [ "${EXIT_CODE}" -eq 0 ]; then
    echo "✔ Benchmark campaign finished successfully with exit code 0."
else
    echo "✘ Benchmark campaign failed with exit code ${EXIT_CODE}. Check ${LOG_FILE}."
fi

# If auto-shutdown argument is passed, safely shut down VM
if [ "${1:-}" = "--auto-shutdown" ]; then
    echo "Auto-shutdown requested. Powering off VM in 60 seconds..."
    sleep 60
    sudo shutdown -h now
fi
